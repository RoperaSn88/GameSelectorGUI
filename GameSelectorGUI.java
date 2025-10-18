// ...existing code...
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
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
import javax.swing.*;

public class GameSelectorGUI{
    ArrayList<Game> Games=new ArrayList<>();
    ArrayList<JLabel> GameTexts=new ArrayList<>();
    int selectNumber=0;
    public Boolean Gaming=false;
    JLabel GameIcon;
    JLabel ExplainText;

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
         //ゲームのリストを作成する
        Games.add(new Game("Re Skipper", "GameDatas\\reskipper\\Re Skipper.exe", "Images\\reskipper.png",null,"<html><body>宇宙空間の中<br />せまりくる敵と隕石を退治して<br />スコアを稼ぐゲームです</body></html>"));
        Games.add(new Game("ええから成仏せぇ", "GameDatas\\eekara\\u1w20240812.exe", "Images\\eekara.png","backGround\\eekara_back.png","<html><body>蘇ってきたゾンビを<br />ハンマーでぶったたいて<br />地に還すゲームです</body></html>"));
        Games.add(new Game("Imaginary", "GameDatas\\Imaginary\\Imaginary.exe", "Images\\imaginary.png",null,"<html><body>数学IIIの教材「複素数平面」を<BR>パク参考にしたアクションゲームです</body></html>"));

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
        backgroundPanel.setLayout(new GridLayout(1,2));
        ImageIcon cv = new ImageIcon("backGroundCover.png");
        Image cover = cv.getImage();
        backgroundPanelCover=new BackgroundPanel(cover);
        backgroundPanelCover.setLayout(new GridLayout(1,2));
        f.setContentPane(backgroundPanel);
        //f.setContentPane(backgroundPanelCover);
 
        // 文字を配置する際、JFrame.addでは各方角に1つしか配置できないらしいのでPanelを使用する。
        JPanel textPanel=new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel,BoxLayout.Y_AXIS));
        // 透明にして背景を透かす
        textPanel.setOpaque(false);

        Stream<Game> GameStream=Games.stream();
        GameStream.forEach(x -> {
            JLabel GameText=new JLabel();
            GameText.setText(x.name);
            GameTexts.add(GameText);
            // 起動時の大きさを揃えるため SMALL_SIZE を初期フォントにする
            GameText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,SMALL_SIZE));
            textPanel.add(GameText);
        });
        //Exit用
        Game Exit=new Game("[Exit]","null","null","ウィンドウを閉じます",null);
        Games.add(Exit);
        JLabel GameText=new JLabel();
        GameText.setText(Exit.name);
        GameTexts.add(GameText);
        // ここも SMALL_SIZE に揃える
        GameText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,SMALL_SIZE));
        textPanel.add(GameText);

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

        //右側
         //右側
        var iconPanel=new JPanel();
        iconPanel.setOpaque(false); // 透過にする
        GameIcon=new JLabel(Games.get(selectNumber).image);
        iconPanel.add(GameIcon,BorderLayout.NORTH);
        ExplainText=new JLabel();
        ExplainText.setText(Games.get(selectNumber).Explain);
        ExplainText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,36));
        iconPanel.add(ExplainText,BorderLayout.SOUTH);
        // // 右側（背景はもう Game.image を使うため、アイコン用ラベルは不要）
        // var iconPanel=new JPanel(new BorderLayout());
        // iconPanel.setOpaque(false); // 透過にする（背景を透かす）
        // ExplainText=new JLabel();
        // ExplainText.setText(Games.get(selectNumber).Explain);
        // ExplainText.setFont(new Font("BIZ UDPゴシック",Font.PLAIN,36));
        // iconPanel.add(ExplainText, BorderLayout.CENTER);

        // 背景パネル上に他コンポーネントを追加（透過設定を維持）
        backgroundPanel.add(textPanel);
        backgroundPanel.add(iconPanel);

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
            System.out.println("SelectNumber: "+ selectNumber);
            updateSelectionTargets();
        }
    }

    public void PushDownAction(){
        if(selectNumber < GameTexts.size()-1) {
            selectNumber++;
            System.out.println("SelectNumber: "+ selectNumber);
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
class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(Image img) {
        this.backgroundImage = img;
        // パネル自体は透明にしておく（子コンポーネントの透過を利用）
        setOpaque(false);
    }

    public void setBackgroundImage(Image img) {
        this.backgroundImage = img;
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        // 先に親の処理（必要なら背景色）を行う
        super.paintComponent(g);
        if (backgroundImage != null) {
            // パネルサイズに合わせてスケーリングして描画（高品質レンダリングは必要に応じて追加）
            int w = getWidth();
            int h = getHeight();
            g.drawImage(backgroundImage, 0, 0, w, h, this);
        }
    }
}

