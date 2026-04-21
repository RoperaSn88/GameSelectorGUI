import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

class GameDetailDialog extends JDialog {
    private final GameSelectorGUI selectorGUI;

    public GameDetailDialog(JFrame owner, Game game, GameSelectorGUI selectorGUI){
        super(owner, game.name, true);
        this.selectorGUI = selectorGUI;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(3, 1));
        setSize(960, 720);
        setLocationRelativeTo(owner);

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(Color.BLACK);
        JLabel imageLabel = new JLabel(game.image);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel nameLabel = new JLabel(game.name != null ? game.name : "");
        nameLabel.setFont(new Font("BIZ UDPゴシック", Font.BOLD, 42));
        JLabel explainLabel = new JLabel(game.Explain != null ? game.Explain : "");
        explainLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 28));
        JLabel tutorialLabel = new JLabel(game.Tutorial != null ? game.Tutorial : "");
        tutorialLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 24));
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(16));
        infoPanel.add(explainLabel);
        infoPanel.add(Box.createVerticalStrut(16));
        infoPanel.add(tutorialLabel);

        JPanel guidePanel = new JPanel(new BorderLayout());
        guidePanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel guideLabel = new JLabel("<html>スペースキー・エンターキーでゲームを開始します。<br/>Escキーでウィンドウを閉じます</html>");
        guideLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 24));
        guidePanel.add(guideLabel, BorderLayout.NORTH);

        add(imagePanel);
        add(infoPanel);
        add(guidePanel);

        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "confirm");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actionMap.put("confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectorGUI.ConfirmGameFromDetailWindow();
            }
        });
        actionMap.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectorGUI.CloseGameDetailWindow();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if(selectorGUI.mainFrame != null){
                    selectorGUI.mainFrame.requestFocusInWindow();
                }
            }
        });
    }
}
