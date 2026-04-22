// ...existing code...
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.MediaTracker;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.Rectangle;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSelectorGUI{
    ArrayList<Game> Games=new ArrayList<>();
    ArrayList<JLabel> GameTexts=new ArrayList<>();
    int selectNumber=0;
    public volatile boolean Gaming=false;
    JLabel GameNameText;
    BaseFrame mainFrame;
    GameDetailDialog detailDialog;

    // 変更: 背景用ラベル -> 背景を描画するパネルに置き換え
    BackgroundPanel backgroundPanel;
    ImageLayerPanel backGroundPanelLayer;
    
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
    final int FADE_DURATION = 480; // ms
    private static final String BACKGROUND_COVER_IMAGE_PATH = "backGroundCover.png";
    private static final String BACKGROUND_PANEL_IMAGE_PATH = "backGroundPanel.png";
    Timer fadeTimer;
    private final AtomicBoolean exiting = new AtomicBoolean(false);

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
        mainFrame = f;
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.setSize(width, height);
        f.setLayout(new GridLayout(1,2));

        // 背景用パネルを作成してフレームのコンテントに設定（背景は自動でリサイズして描画される）
        Image initialBg = Games.get(selectNumber).backGround != null ? Games.get(selectNumber).backGround.getImage() : null;
        Image initialVideo = Games.get(selectNumber).backgroundVideo != null ? Games.get(selectNumber).backgroundVideo.getImage() : null;
        backgroundPanel = new BackgroundPanel(initialBg);
        backgroundPanel.setBackgroundMedia(initialBg, initialVideo);
        backgroundPanel.setLayout(new BorderLayout());
        var layeredRoot = new JLayeredPane();
        layeredRoot.setLayout(null);
        f.setContentPane(layeredRoot);

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
            GameText.setForeground(Color.white);
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
        GameText.setForeground(Color.WHITE);
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

        // ゲーム名のみ表示
        GameNameText = new JLabel(Games.get(selectNumber).name);
        GameNameText.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 64));
        GameNameText.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        GameNameText.setHorizontalAlignment(JLabel.TRAILING);
        GameNameText.setForeground(Color.WHITE);
        var namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.setLayout(new BorderLayout());
        namePanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        namePanel.add(GameNameText, BorderLayout.SOUTH);

        var gameNameLayer = new JPanel(new BorderLayout());
        gameNameLayer.setOpaque(false);
        gameNameLayer.add(namePanel, BorderLayout.EAST);

        var textLayer = new JPanel(new BorderLayout());
        textLayer.setOpaque(false);
        textLayer.add(textPanel, BorderLayout.WEST);
        JLabel guideLabel = new JLabel("上下キー/WSキーで選択　スペースで決定");
        guideLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 24));
        guideLabel.setForeground(Color.WHITE);
        guideLabel.setHorizontalAlignment(SwingConstants.CENTER);
        guideLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        textLayer.add(guideLabel, BorderLayout.SOUTH);

        var backGroundCoverLayer = new ImageLayerPanel(loadImage(BACKGROUND_COVER_IMAGE_PATH));
        backGroundPanelLayer = new ImageLayerPanel(loadImage(BACKGROUND_PANEL_IMAGE_PATH));
        backGroundPanelLayer.setLayerAlpha(1f);

        layeredRoot.add(backgroundPanel, Integer.valueOf(0));
        layeredRoot.add(backGroundCoverLayer, Integer.valueOf(100));
        layeredRoot.add(gameNameLayer, Integer.valueOf(200));
        layeredRoot.add(textLayer, Integer.valueOf(300));
        layeredRoot.add(backGroundPanelLayer, Integer.valueOf(400));
        updateLayerBounds(layeredRoot, backgroundPanel, backGroundCoverLayer, gameNameLayer, textLayer, backGroundPanelLayer);

        // リサイズ時は背景を再描画
        f.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backgroundPanel.revalidate();
                backgroundPanel.repaint();
                updateLayerBounds(layeredRoot, backgroundPanel, backGroundCoverLayer, gameNameLayer, textLayer, backGroundPanelLayer);
            }
        });
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                animateDarkOverlay(1f, 0f, FADE_DURATION, null);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                RequestApplicationExit();
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
                // 必要列: name,path,image,background,explain,tutorial,backgroundVideo （不足分は null で埋める）
                String name = cols.size() > 0 ? cols.get(0) : "";
                String path = cols.size() > 1 ? cols.get(1) : "null";
                String image = cols.size() > 2 ? cols.get(2) : null;
                String background = cols.size() > 3 ? cols.get(3) : null;
                String explain = cols.size() > 4 ? cols.get(4) : null;
                String tut = cols.size() > 5 ? cols.get(5) : null;
                String backgroundVideo = cols.size() > 6 ? cols.get(6) : null;
                Games.add(new Game(name, path, image, background, explain, tut, backgroundVideo));
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

        // 通常ウィンドウはゲーム名のみ更新
        GameNameText.setText(Games.get(selectNumber).name);
        // 背景を選択中のゲームの backGround / 背景映像に更新
        Image background = Games.get(selectNumber).backGround != null ? Games.get(selectNumber).backGround.getImage() : null;
        Image backgroundVideo = Games.get(selectNumber).backgroundVideo != null ? Games.get(selectNumber).backgroundVideo.getImage() : null;
        backgroundPanel.setBackgroundMedia(background, backgroundVideo);

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
        if(selectNumber < Games.size()-1) {
            selectNumber++;
            updateSelectionTargets();
        }
    }

    public void StartGame(){
        if(Gaming) return;
        String GetPath=Games.get(selectNumber).path;
        Gaming=true;
        animateDarkOverlay(0f, 1f, FADE_DURATION, () -> {
            Thread gameThread = new Thread(() -> {
                try {
                    ProcessBuilder builder = new ProcessBuilder(GetPath);
                    Process process=builder.start();
                    process.waitFor();
                } catch (Exception e) {
                    System.out.println("Failed to launch game at path: " + GetPath + " (" + e.getMessage() + ")");
                } finally {
                    SwingUtilities.invokeLater(() -> animateDarkOverlay(1f, 0f, FADE_DURATION, () -> Gaming=false));
                }
            }, "game-runner-thread");
            gameThread.start();
        });
    }

    public void RequestApplicationExit(){
        if(Gaming || !exiting.compareAndSet(false, true)) return;
        animateDarkOverlay(0f, 1f, FADE_DURATION, () -> System.exit(0));
    }

    private Image loadImage(String path) {
        Path overlayPath = Paths.get(path);
        if (!Files.exists(overlayPath)) {
            return null;
        }
        return new ImageIcon(overlayPath.toString()).getImage();
    }

    private void updateLayerBounds(JLayeredPane root, JComponent... layers) {
        Rectangle bounds = root.getBounds();
        for (JComponent layer : layers) {
            layer.setBounds(0, 0, bounds.width, bounds.height);
        }
    }

    private void animateDarkOverlay(float startAlpha, float endAlpha, int durationMs, Runnable onComplete){
        if(backGroundPanelLayer == null){
            if(onComplete != null) onComplete.run();
            return;
        }
        backGroundPanelLayer.setLayerAlpha(startAlpha);
        if(durationMs <= 0){
            backGroundPanelLayer.setLayerAlpha(endAlpha);
            if(onComplete != null) onComplete.run();
            return;
        }
        if(fadeTimer != null && fadeTimer.isRunning()){
            fadeTimer.stop();
        }
        final long startedAt = System.currentTimeMillis();
        fadeTimer = new Timer(TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startedAt;
            float t = Math.min(1f, Math.max(0f, elapsed / (float)durationMs));
            float alpha = startAlpha + (endAlpha - startAlpha) * t;
            backGroundPanelLayer.setLayerAlpha(alpha);
            if(t >= 1f){
                fadeTimer.stop();
                backGroundPanelLayer.setLayerAlpha(endAlpha);
                if(onComplete != null) onComplete.run();
            }
        });
        fadeTimer.start();
    }

    public void OpenGameDetailWindow(){
        if(Gaming) return;
        if(selectNumber < 0 || selectNumber >= Games.size()) return;
        if(detailDialog != null && detailDialog.isDisplayable()) return;
        detailDialog = new GameDetailDialog(mainFrame, Games.get(selectNumber), this);
        detailDialog.setVisible(true);
        if(mainFrame != null){
            mainFrame.requestFocusInWindow();
        }
    }

    public void CloseGameDetailWindow(){
        if(detailDialog != null){
            detailDialog.dispose();
            detailDialog = null;
        }
        if(mainFrame != null){
            mainFrame.requestFocusInWindow();
        }
    }

    public void ConfirmGameFromDetailWindow(){
        if(detailDialog != null){
            detailDialog.dispose();
            detailDialog = null;
        }
        StartGame();
        if(mainFrame != null){
            mainFrame.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        new GameSelectorGUI();
    }
}

// 背景描画用パネル
// static 背景画像、または GIF などの背景映像（ループ再生）を表示する
class BackgroundPanel extends JPanel {
    private Image backgroundImage;
    private Image backgroundVideoImage;
    private float darkOverlayAlpha = 0f;

    public BackgroundPanel(Image img) {
        this.backgroundImage = img;
        setOpaque(false);
    }

    // 単体で背景を設定（静止画）
    public void setBackgroundImage(Image img) {
        this.backgroundImage = img;
        repaint();
    }

    // 背景と背景映像を同時に設定（映像があれば映像を優先表示）
    public void setBackgroundMedia(Image background, Image backgroundVideo) {
        this.backgroundImage = background;
        this.backgroundVideoImage = backgroundVideo;
        repaint();
    }

    public void setDarkOverlayAlpha(float alpha) {
        this.darkOverlayAlpha = Math.max(0f, Math.min(1f, alpha));
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

        Image drawTarget = backgroundVideoImage != null ? backgroundVideoImage : backgroundImage;
        if (drawTarget != null) {
            g2.drawImage(drawTarget, 0, 0, w, h, this);
        }
        if (darkOverlayAlpha > 0f) {
            g2.setComposite(AlphaComposite.SrcOver.derive(darkOverlayAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }
}

class ImageLayerPanel extends JComponent {
    private final Image layerImage;
    private float layerAlpha = 1f;

    public ImageLayerPanel(Image layerImage) {
        this.layerImage = layerImage;
        setOpaque(false);
    }

    public void setLayerAlpha(float alpha) {
        this.layerAlpha = Math.max(0f, Math.min(1f, alpha));
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (layerAlpha <= 0f || layerImage == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(layerAlpha));
        int w = getWidth();
        int h = getHeight();
        g2.drawImage(layerImage, 0, 0, w, h, this);
        g2.dispose();
    }
}
