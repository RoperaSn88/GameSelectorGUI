import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

// GameMaster.csv を GUI で編集するためのアプリケーション
// 列順: ゲーム名,exe,icon.png,bg.png,説明,チュートリアル
public class GameEditorGUI {
    static final String CSV_NAME = "GameMaster.csv";
    static final int COLUMN_COUNT = 6;
    static final String[] COLUMN_LABELS = {"ゲーム名", "exe", "アイコン画像", "背景画像", "説明", "チュートリアル"};
    // パスとして扱う列（ファイル選択で編集する）
    static final boolean[] IS_PATH = {false, true, true, true, false, false};
    // 多行文字列として扱う列（html に変換して保存する）
    static final boolean[] IS_HTML = {false, false, false, false, true, true};

    final Path csvPath;
    final Path baseDir;
    String header;
    final ArrayList<GameEntry> entries = new ArrayList<>();

    JFrame frame;
    JPanel listPanel;
    JLabel summaryLabel;

    // CSV 1行分のデータ
    static class GameEntry {
        String[] values = new String[COLUMN_COUNT];
        int originalColumnCount;

        GameEntry(List<String> cols) {
            originalColumnCount = cols.size();
            for (int i = 0; i < COLUMN_COUNT; i++) {
                values[i] = i < cols.size() ? cols.get(i) : "";
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GameEditorGUI(Paths.get(CSV_NAME));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        CSV_NAME + " を読み込めませんでした。\n" + ex.getMessage(),
                        "読み込みエラー", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    public GameEditorGUI(Path csvPath) throws IOException {
        this.csvPath = csvPath.toAbsolutePath().normalize();
        this.baseDir = this.csvPath.getParent();
        loadCSV();
        buildFrame();
        refreshList();
        frame.setVisible(true);
    }

    // ---------- CSV 入出力 ----------

    private void loadCSV() throws IOException {
        if (!Files.exists(csvPath)) {
            throw new FileNotFoundException(csvPath + " が見つかりません");
        }
        try (BufferedReader r = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // 1行目はヘッダー行（GameSelectorGUI と同じ扱い）
                if (header == null) {
                    header = line;
                    continue;
                }
                entries.add(new GameEntry(parseCSVLine(line)));
            }
        }
        if (header == null) {
            header = String.join(",", "ゲーム名", "exe", "icon.png", "bg.png", "説明", "チュートリアル");
        }
    }

    private void saveCSV() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (GameEntry e : entries) {
            for (int i = 0; i < COLUMN_COUNT; i++) {
                if (i > 0) sb.append(',');
                sb.append(escapeCSV(e.values[i]));
            }
            sb.append("\n");
        }
        // 一時ファイルに書いてから置き換え、書き込み途中で壊れないようにする
        Path tmp = csvPath.resolveSibling(CSV_NAME + ".tmp");
        Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, csvPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // GameSelectorGUI と同じ規則の CSV パーサ（ダブルクォートと "" エスケープをサポート）
    static ArrayList<String> parseCSVLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    static String escapeCSV(String s) {
        if (s == null) return "";
        // GameSelectorGUI は1行ずつ読むので改行は残さない
        s = s.replace("\r", "").replace("\n", "");
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ---------- 検証 ----------

    // 読み込めなかった項目の一覧を返す（空なら全項目ロード可能）
    List<String> validate(GameEntry e) {
        ArrayList<String> problems = new ArrayList<>();
        for (int i = 0; i < COLUMN_COUNT; i++) {
            String v = e.values[i];
            if (i >= e.originalColumnCount) {
                problems.add(COLUMN_LABELS[i] + "：列がありません");
            } else if (v == null || v.trim().isEmpty()) {
                problems.add(COLUMN_LABELS[i] + "：未入力");
            } else if (IS_PATH[i] && !Files.exists(resolveCsvPath(v))) {
                problems.add(COLUMN_LABELS[i] + "：ファイルが見つかりません (" + v + ")");
            }
        }
        return problems;
    }

    // ---------- パス変換 ----------

    // CSV 内の表記（例: Images\\a.png）を実ファイルのパスに変換
    Path resolveCsvPath(String csvValue) {
        try {
            return baseDir.resolve(csvValue.replace("\\\\", "\\").replace("/", File.separator)).normalize();
        } catch (InvalidPathException ex) {
            return baseDir.resolve("__invalid__");
        }
    }

    // 実ファイルを CSV に書く相対パス表記に変換（既存データに合わせて区切りは \\ ）
    String toCsvPath(File file) {
        Path abs = file.toPath().toAbsolutePath().normalize();
        String rel;
        try {
            rel = baseDir.relativize(abs).toString();
        } catch (IllegalArgumentException ex) {
            // 別ドライブなど相対化できない場合は絶対パス
            rel = abs.toString();
        }
        return rel.replace("/", "\\").replace("\\", "\\\\");
    }

    // ---------- html 変換 ----------

    static String toHtml(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String s = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        s = s.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br />");
        return "<html><body>" + s + "</body></html>";
    }

    static String fromHtml(String html) {
        if (html == null) return "";
        String s = html.trim();
        if (!s.regionMatches(true, 0, "<html>", 0, 6)) return s;
        s = s.replaceAll("(?i)</?html>", "").replaceAll("(?i)</?body>", "");
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
        return s;
    }

    // ---------- 画面 ----------

    private void buildFrame() {
        frame = new JFrame("GameEditorGUI - " + csvPath.getFileName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 640);
        frame.setLocationRelativeTo(null);

        summaryLabel = new JLabel();
        summaryLabel.setBorder(new EmptyBorder(8, 10, 8, 10));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(summaryLabel, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
    }

    private void refreshList() {
        listPanel.removeAll();
        int warnCount = 0;
        for (int i = 0; i < entries.size(); i++) {
            List<String> problems = validate(entries.get(i));
            if (!problems.isEmpty()) warnCount++;
            JPanel item = createItemPanel(i, problems);
            item.setAlignmentX(0f);
            listPanel.add(item);
            listPanel.add(Box.createVerticalStrut(6));
        }
        if (warnCount == 0) {
            summaryLabel.setText(entries.size() + " 件のゲームを読み込みました。");
            summaryLabel.setForeground(Color.DARK_GRAY);
        } else {
            summaryLabel.setText(entries.size() + " 件中 " + warnCount + " 件に警告があります。");
            summaryLabel.setForeground(new Color(180, 90, 0));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createItemPanel(int index, List<String> problems) {
        GameEntry e = entries.get(index);
        boolean ok = problems.isEmpty();

        JPanel panel = new JPanel(new BorderLayout(10, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ok ? Color.LIGHT_GRAY : new Color(230, 150, 40), ok ? 1 : 2),
                new EmptyBorder(8, 8, 8, 8)));

        // アイコン
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(64, 64));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        Path iconPath = resolveCsvPath(e.values[2]);
        if (!e.values[2].isEmpty() && Files.exists(iconPath)) {
            Image img = new ImageIcon(iconPath.toString()).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            icon.setIcon(new ImageIcon(img));
        } else {
            icon.setText("No Image");
        }
        panel.add(icon, BorderLayout.WEST);

        // 情報
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = new JLabel((index + 1) + ". " + (e.values[0].isEmpty() ? "(名前なし)" : e.values[0]));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));
        info.add(name);
        info.add(new JLabel("exe: " + e.values[1]));
        info.add(new JLabel("説明: " + fromHtml(e.values[4]).replace("\n", " ")));
        if (!ok) {
            JLabel warnTitle = new JLabel("⚠ 読み込めなかった項目:");
            warnTitle.setForeground(new Color(200, 60, 0));
            info.add(warnTitle);
            for (String p : problems) {
                JLabel l = new JLabel("    ・" + p);
                l.setForeground(new Color(200, 60, 0));
                info.add(l);
            }
        }
        panel.add(info, BorderLayout.CENTER);

        // ボタン
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        JButton edit = new JButton("編集");
        edit.addActionListener(ev -> openEditDialog(index));
        JButton delete = new JButton("削除");
        delete.addActionListener(ev -> deleteEntry(index));
        buttons.add(edit);
        buttons.add(delete);
        panel.add(buttons, BorderLayout.EAST);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    // ---------- 削除 ----------

    private void deleteEntry(int index) {
        GameEntry e = entries.get(index);
        int res = JOptionPane.showConfirmDialog(frame,
                "「" + e.values[0] + "」(" + (index + 1) + "行目) を " + CSV_NAME + " から削除します。\nよろしいですか？",
                "削除の確認", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        entries.remove(index);
        if (!trySave()) {
            entries.add(index, e);
        }
        refreshList();
    }

    // ---------- 編集 ----------

    private void openEditDialog(int index) {
        GameEntry e = entries.get(index);
        JDialog dialog = new JDialog(frame, "編集: " + e.values[0], true);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 各列の入力欄（JTextField か JTextArea）
        javax.swing.text.JTextComponent[] inputs = new javax.swing.text.JTextComponent[COLUMN_COUNT];

        for (int i = 0; i < COLUMN_COUNT; i++) {
            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = 0;
            lc.gridy = i;
            lc.anchor = GridBagConstraints.NORTHWEST;
            lc.insets = new Insets(4, 4, 4, 8);
            form.add(new JLabel(COLUMN_LABELS[i]), lc);

            GridBagConstraints fc = new GridBagConstraints();
            fc.gridx = 1;
            fc.gridy = i;
            fc.weightx = 1;
            fc.fill = GridBagConstraints.BOTH;
            fc.insets = new Insets(4, 0, 4, 4);

            if (IS_HTML[i]) {
                JTextArea area = new JTextArea(fromHtml(e.values[i]), 5, 40);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                inputs[i] = area;
                fc.weighty = 1;
                form.add(new JScrollPane(area), fc);
            } else {
                JTextField field = new JTextField(e.values[i], 40);
                inputs[i] = field;
                form.add(field, fc);
            }

            if (IS_PATH[i]) {
                final int col = i;
                JButton browse = new JButton("参照...");
                browse.addActionListener(ev -> {
                    File chosen = chooseFile(dialog, inputs[col].getText());
                    if (chosen != null) inputs[col].setText(toCsvPath(chosen));
                });
                GridBagConstraints bc = new GridBagConstraints();
                bc.gridx = 2;
                bc.gridy = i;
                bc.insets = new Insets(4, 0, 4, 4);
                form.add(browse, bc);
            }
        }

        JLabel note = new JLabel("※ 説明・チュートリアルの改行は保存時に html (<br />) に変換されます。");
        note.setForeground(Color.GRAY);

        JButton saveBtn = new JButton("保存");
        JButton cancelBtn = new JButton("キャンセル");
        saveBtn.addActionListener(ev -> {
            String[] before = e.values.clone();
            int beforeCount = e.originalColumnCount;
            for (int i = 0; i < COLUMN_COUNT; i++) {
                String text = inputs[i].getText();
                e.values[i] = IS_HTML[i] ? toHtml(text) : text.trim();
            }
            e.originalColumnCount = COLUMN_COUNT;
            if (trySave()) {
                dialog.dispose();
                refreshList();
            } else {
                e.values = before;
                e.originalColumnCount = beforeCount;
            }
        });
        cancelBtn.addActionListener(ev -> dialog.dispose());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(0, 10, 10, 10));
        bottom.add(note, BorderLayout.WEST);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(saveBtn);
        btns.add(cancelBtn);
        bottom.add(btns, BorderLayout.EAST);

        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private File chooseFile(JDialog parent, String current) {
        JFileChooser chooser = new JFileChooser(baseDir.toFile());
        if (current != null && !current.trim().isEmpty()) {
            Path p = resolveCsvPath(current.trim());
            if (Files.exists(p)) {
                chooser.setSelectedFile(p.toFile());
            } else if (p.getParent() != null && Files.isDirectory(p.getParent())) {
                chooser.setCurrentDirectory(p.getParent().toFile());
            }
        }
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (!f.toPath().toAbsolutePath().normalize().startsWith(baseDir)) {
            JOptionPane.showMessageDialog(parent,
                    "選択したファイルは " + CSV_NAME + " と同じフォルダの外にあります。\n" +
                    "GameSelectorGUI の実行場所によっては読み込めない可能性があります。",
                    "注意", JOptionPane.WARNING_MESSAGE);
        }
        return f;
    }

    private boolean trySave() {
        try {
            saveCSV();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    CSV_NAME + " の保存に失敗しました。\n" + ex.getMessage(),
                    "保存エラー", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
