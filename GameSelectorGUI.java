// ...existing code...
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.MediaTracker;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;

public class GameSelectorGUI{
    ArrayList<Game> Games=new ArrayList<>();
    ArrayList<JLabel> GameTexts=new ArrayList<>();
    int selectNumber=0;
    public Boolean Gaming=false;
    JLabel GameIcon;
    JLabel ExplainText;
    JLabel TutorialText;
    JLabel GameNameText;

    // 変更: 背景用ラベル -> 背景を描画するパネルに置き換え
    BackgroundPanel backgroundPanel;
    BackgroundPanel backgroundPanelCover;
    
    // アニメーション用
    int[] currentSizes;
    int[] targetSizes;
    Timer animTimer;
    final int SMALL_SIZE = 36;
    final int SELECT_SIZE = 54;
    final int INIT_SIZE = 24;
    final int STEP = 2; // 1フレームあたりの変化量（未使用だが残しています）
    final int TIMER_DELAY = 16; // ms（滑らかにするため少し短め）

    // OutCubic アニメーション用（拡大のみ）
    int animIndex = -1;
    long animStartTime = 0L;
    int animStartSize = 0;
    int animEndSize = 0;
    final int ANIM_DURATION = 360; // ms

    public GameSelectorGUI(){
         //ゲームのリストを作成する（CSVから読み込む。見つからない場合はデフォルトを追加）
        // プロジェクトルート（実行ディレクトリ）に置く GamesMaster.csv を優先して読み込む
        // フォーマット: name,path,image,background,explain
        if (!loadGamesFromCSV("GameMaster.csv")) {
            System.err.println("GamesMasterなし");
            return;
        }

        //maxmizeにしたい
        Dimension dim=Toolkit.getDefaultToolkit().getScreenSize();
        int width = dim.width;
        int height = dim.height;
        

        //ウィンドウの作成
        var f = new BaseFrame("GameSelectorGUI",this);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(width, height);
        f.setLayout(new GridLayout(1,2));

        // 背景用パネルを作成してフレームのコンテントに設定（背景は自動でリサイズして描画される）
        Image initialBg = Games.get(selectNumber).backGround != null ? Games.get(selectNumber).backGround.getImage() : null;
        backgroundPanel = new BackgroundPanel(initialBg);
        // 左にゲーム一覧（下寄せ）、中央にアイコン等を置くレイアウトに変更
        backgroundPanel.setLayout(new BorderLayout());
        ImageIcon cv = new ImageIcon("backGroundCover.png");
        Image cover = cv.getImage();
        backgroundPanelCover=new BackgroundPanel(cover);
        backgroundPanelCover.setLayout(new GridLayout(1,2));
        backgroundPanel.setOverlayImage(cover);
        f.setContentPane(backgroundPanel);
        //f.setContentPane(backgroundPanelCover);

        // 文字を配置する際、JFrame.addでは各方角に1つしか配置できないらしいのでPanelを使用する。
        JPanel textPanel=new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel,BoxLayout.Y_AXIS));
        // 透明にして背景を透かす
        textPanel.setOpaque(false);

        // 下寄せで縦方向に並べるため、先にスペースを作ってからラベルを追加する
        Stream<Game> GameStream=Games.stream();
        textPanel.add(Box.createVerticalGlue()); // これでラベル群が下に詰まる
        GameStream.forEach(x -> {
            JLabel GameText=new JLabel(x.name);
            GameTexts.add(GameText);
            // 起動時の大きさを揃えるため SMALL_SIZE を初期フォントにする
            GameText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,SMALL_SIZE));
            GameText.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT); // 左揃え
            textPanel.add(GameText);
            textPanel.add(Box.createVerticalStrut(6)); // ラベル間の余白
        });
         //Exit用
        // Game Exit=new Game("[Exit]","null","null","ウィンドウを閉じます",null);
        // Games.add(Exit);
        JLabel GameText=new JLabel();
        GameTexts.add(GameText);
        // ここも SMALL_SIZE に揃える
        GameText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,SMALL_SIZE));
        GameText.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        textPanel.add(GameText);
        textPanel.add(Box.createVerticalStrut(6));
        // 左側パネルに余白を与える（左下寄せで表示される）
        textPanel.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));

        // 初期化：サイズ配列
        int n = GameTexts.size();
        currentSizes = new int[n];
        targetSizes = new int[n];
        for(int i=0;i<n;i++){
            // 全てを SMALL_SIZE で揃えておく（起動時に非選択項目は変化しないように）
            currentSizes[i] = SMALL_SIZE;
            targetSizes[i] = SMALL_SIZE;
        }
        // 最初の選択を反映（最初の項目だけ拡大ターゲット）
        targetSizes[selectNumber] = SELECT_SIZE;

        // アニメーションタイマー（OutCubic 拡大専用ロジックを使用）
        animTimer = new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();

                if (animIndex >= 0) {
                    long elapsed = now - animStartTime;
                    double t = Math.min(1.0, Math.max(0.0, (double)elapsed / ANIM_DURATION));
                    // easeOutCubic: 1 - (1 - t)^3
                    double eased = 1.0 - Math.pow(1.0 - t, 3.0);
                    int value = animStartSize + (int)Math.round(eased * (animEndSize - animStartSize));
                    currentSizes[animIndex] = value;
                    GameTexts.get(animIndex).setFont(new Font("BIZ UDPゴシック", Font.PLAIN, currentSizes[animIndex]));

                    if (t >= 1.0) {
                        // アニメーション終了
                        animIndex = -1;
                        animStartTime = 0L;
                    }
                }

                // 画面上のフォントが変更された場合に反映
                for (int i = 0; i < GameTexts.size(); i++) {
                    // animIndex が処理中の要素は既に更新済み
                    if (i == animIndex) continue;
                    GameTexts.get(i).setFont(new Font("BIZ UDPゴシック", Font.PLAIN, currentSizes[i]));
                }

                // タイマーは、アニメーション中のみ回し続ける
                if (animIndex < 0) {
                    animTimer.stop();
                }
            }
        });

        // 右側用パネル（右寄せ・下寄せで説明文を配置）
        var iconPanel = new JPanel();
        iconPanel.setOpaque(false); // 背景透過
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.Y_AXIS));
        // 上側に伸びるスペースを入れて下寄せにする
        iconPanel.add(Box.createVerticalGlue());

        // アイコンは説明文の上に配置、右詰め
        GameIcon = new JLabel(Games.get(selectNumber).image);
        GameIcon.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        iconPanel.add(GameIcon);
        iconPanel.add(Box.createVerticalStrut(12)); // アイコンと説明の間隔

        // 説明文は下・右詰め
        ExplainText = new JLabel(Games.get(selectNumber).Explain);
        ExplainText.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 36));
        ExplainText.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        ExplainText.setHorizontalAlignment(JLabel.TRAILING);

        // ゲーム名は説明文の下に右詰めで配置
        GameNameText = new JLabel(Games.get(selectNumber).name);
        GameNameText.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 48));
        GameNameText.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        GameNameText.setHorizontalAlignment(JLabel.TRAILING);

        // 説明 → ゲーム名 の順で追加（ゲーム名が説明の下に来る）
        iconPanel.add(ExplainText);
        iconPanel.add(Box.createVerticalStrut(6));
        iconPanel.add(GameNameText);

        // パネル全体の余白（右下に余白を作る）
        iconPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // 背景パネル上に他コンポーネントを追加（透過設定を維持）
        backgroundPanel.add(textPanel, BorderLayout.WEST);
        // 右側に配置する
        backgroundPanel.add(iconPanel, BorderLayout.EAST);

        // リサイズ時は背景を再描画
        f.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backgroundPanel.revalidate();
                backgroundPanel.repaint();
            }
        });

        f.setVisible(true);
        f.addKeyListener(f);

        // 起動時に最初の選択が拡大するアニメーションを開始（OutCubic）
        startExpandAnimation(selectNumber);
    }

    private BackgroundPanel CreateBackgroundPanel(ImageIcon target){
        return null;
    }

    // CSV を読み込んで Games リストを構築する
    // 返り値: 成功したら true、失敗（ファイル無しやIO例外）は false
    private boolean loadGamesFromCSV(String csvPath) {
        Path p = Paths.get(csvPath);
        if (!Files.exists(p)){
            System.err.println("pathが不適切");
            return false;
        }
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                // 空行はスキップ
                if (line.trim().isEmpty()) continue;
                // ヘッダー判定: 1 行目が "name" で始まる場合はヘッダーとしてスキップ
                if (first) {
                    String t = line.trim().toLowerCase();
                    if (t.startsWith("name") || t.contains("path") && t.contains("image")) {
                        first = false;
                        continue;
                    }
                }
                first = false;
                ArrayList<String> cols = parseCSVLine(line);
                // 必要列: name,path,image,background,explain （不足分は null で埋める）
                String name = cols.size() > 0 ? cols.get(0) : "";
                String path = cols.size() > 1 ? cols.get(1) : "null";
                String image = cols.size() > 2 ? cols.get(2) : null;
                String background = cols.size() > 3 ? cols.get(3) : null;
                String explain = cols.size() > 4 ? cols.get(4) : null;
                Games.add(new Game(name, path, image, background, explain));
            }
            return true;
        } catch (IOException ex) {
            System.err.println("GamesMaster.csv 読み込みエラー: " + ex.getMessage());
            return false;
        }
    }

    // 単純な CSV パーサ（ダブルクォートで囲まれたフィールドと "" エスケープをサポート）
    private ArrayList<String> parseCSVLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // 次も " ならエスケープ
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

    // 選択が変わったときにターゲットサイズを更新し、拡大はアニメーション、縮小は即時反映
    private void updateSelectionTargets(){
        int previous = -1;
        for (int i = 0; i < targetSizes.length; i++) {
            if (targetSizes[i] == SELECT_SIZE) {
                previous = i;
                break;
            }
        }

        // 新しいターゲットをセット（論理的ターゲット配列）
        for(int i=0;i<targetSizes.length;i++){
            targetSizes[i] = (i==selectNumber) ? SELECT_SIZE : SMALL_SIZE;
        }

        // 説明やアイコンは即時更新（テキストの拡大はアニメーション）
        ExplainText.setText(Games.get(selectNumber).Explain);
        GameNameText.setText(Games.get(selectNumber).name);
        if(selectNumber==GameTexts.size()-1) {
            GameIcon.setIcon(null);
            // Exit または無効時は背景をクリア
            backgroundPanel.setBackgroundImage(null);
        }
        else {
            GameIcon.setIcon(Games.get(selectNumber).image);
            // 背景を選択中のゲームの backGround に更新
            backgroundPanel.setBackgroundImage(Games.get(selectNumber).backGround != null ? Games.get(selectNumber).backGround.getImage() : null);
        }

        // 前回選択されていた項目は即座に小さくする（アニメーション不要）
        if (previous >= 0 && previous != selectNumber) {
            currentSizes[previous] = SMALL_SIZE;
            GameTexts.get(previous).setFont(new Font("BIZ UDPゴシック", Font.PLAIN, currentSizes[previous]));
        }

        // 今回選ばれた項目は拡大アニメーション（OutCubic）
        if (selectNumber != previous) {
            startExpandAnimation(selectNumber);
        }
    }

    // 拡大アニメーションを開始する（OutCubic）
    private void startExpandAnimation(int index) {
        if (index < 0 || index >= GameTexts.size()) return;
        if (currentSizes[index] >= SELECT_SIZE) return; // 既に十分な大きさなら何もしない

        animIndex = index;
        animStartTime = System.currentTimeMillis();
        animStartSize = currentSizes[index];
        animEndSize = SELECT_SIZE;

        if(!animTimer.isRunning()) animTimer.start();
    }

    // 既存の呼び出しを保つための簡易メソッド（即座にターゲットにセットする）
    public void setTextSize(){
        // 既存の挙動を置き換えるため、ターゲットを更新してアニメーションを開始
        updateSelectionTargets();
    }

    public void PushUpAction(){
        if(selectNumber>0) {
            selectNumber--;
            updateSelectionTargets();
        }
    }

    public void PushDownAction(){
        if(selectNumber < GameTexts.size()-2) {
            selectNumber++;
            updateSelectionTargets();
        }
    }

    public void StartGame(){
        if(!Gaming){
            if(selectNumber==GameTexts.size()-1) {
                System.exit(1);
            }
            String GetPath=Games.get(selectNumber).path;
            try {
                ProcessBuilder builder = new ProcessBuilder(GetPath);
                Process process=builder.start();
                Gaming=true;
                int exitCode=process.waitFor();
                Gaming=false;

            } catch (Exception e) {
                System.out.println("Cant Open exe file");
                Gaming=false;
            }
        }
    }

    public static void main(String[] args) {
        new GameSelectorGUI();
    }
}

class BaseFrame extends JFrame implements  KeyListener{
    GameSelectorGUI selecterGUI;
    Container c;

    public BaseFrame(String name,GameSelectorGUI selecter){
        super(name);
        c = getContentPane();
        this.selecterGUI=selecter;
    }

    public void contentAdd(JLabel j){
        c.add(j);
    }
    public void contentPack(){
        pack();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch ( e.getKeyCode() ) {
        case KeyEvent.VK_UP:
            if(!selecterGUI.Gaming)selecterGUI.PushUpAction();
            break;
        case KeyEvent.VK_DOWN:
            if(!selecterGUI.Gaming)selecterGUI.PushDownAction();
            break;
        case KeyEvent.VK_W:
            if(!selecterGUI.Gaming)selecterGUI.PushUpAction();
            break;
        case KeyEvent.VK_S:
            if(!selecterGUI.Gaming)selecterGUI.PushDownAction();
            break;

        case KeyEvent.VK_SPACE:
            if(!selecterGUI.Gaming)selecterGUI.StartGame();
            break;

        case KeyEvent.VK_ESCAPE:
            System.exit(0);
            break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        
    }
}

// 背景描画用パネル（コンポーネントの背面に画像を描く）
// base（ゲームの背景）と cover（backGroundCover.png）を合成して表示できるように拡張
class BackgroundPanel extends JPanel {
    private Image backgroundImage; // base image
    private Image overlayImage;    // cover image (backGroundCover.png)

    public BackgroundPanel(Image img) {
        this.backgroundImage = img;
        setOpaque(false);
    }

    // 単体で背景を設定
    public void setBackgroundImage(Image img) {
        this.backgroundImage = img;
        repaint();
    }

    // 単体でオーバーレイを設定（backGroundCover.png）
    public void setOverlayImage(Image img) {
        this.overlayImage = img;
        repaint();
    }

    // base と overlay を同時に設定して合成表示する（両方 null 可）
    public void setBackgroundWithOverlay(Image base, Image overlay) {
        this.backgroundImage = base;
        this.overlayImage = overlay;
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // 高品質なスケーリング
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth();
        int h = getHeight();

        // base を引き伸ばして描画
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
        }

        // overlay を上に描画（透明度情報があれば透過合成される）
        if (overlayImage != null) {
            g2.drawImage(overlayImage, 0, 0, w, h, this);
        }

        g2.dispose();
    }
}

