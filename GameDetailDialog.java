import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
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
        setLayout(new GridBagLayout());
        setSize(960, 720);
        setLocationRelativeTo(owner);

        JPanel imagePanel = new JPanel() {
            private final Image img = (game.image != null) ? game.image.getImage() : null;
            private final int imgW = (img != null) ? img.getWidth(null) : 0;
            private final int imgH = (img != null) ? img.getHeight(null) : 0;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (img == null || imgW <= 0 || imgH <= 0) return;
                int panelW = getWidth();
                int panelH = getHeight();
                double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
                int newW = (int) (imgW * scale);
                int newH = (int) (imgH * scale);
                int x = (panelW - newW) / 2;
                int y = (panelH - newH) / 2;
                g.drawImage(img, x, y, newW, newH, this);
            }
        };
        imagePanel.setBackground(Color.BLACK);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel nameLabel = new JLabel(game.name != null ? game.name : "");
        nameLabel.setFont(new Font("BIZ UDPゴシック", Font.BOLD, 42));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel explainLabel = new JLabel(game.Explain != null ? game.Explain : "");
        explainLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 28));
        explainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tutorialLabel = new JLabel(game.Tutorial != null ? game.Tutorial : "");
        tutorialLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 24));
        tutorialLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(16));
        infoPanel.add(explainLabel);
        infoPanel.add(Box.createVerticalStrut(16));
        infoPanel.add(tutorialLabel);
        infoPanel.add(Box.createVerticalGlue());

        JPanel guidePanel = new JPanel(new BorderLayout());
        guidePanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JLabel guideLabel = new JLabel("Space/Enterで開始、Escでウィンドウを閉じます。");
        guideLabel.setFont(new Font("BIZ UDPゴシック", Font.PLAIN, 16));
        guideLabel.setHorizontalAlignment(JLabel.CENTER);
        guidePanel.add(guideLabel, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints lowerGbc = new GridBagConstraints();
        lowerGbc.gridx = 0;
        lowerGbc.fill = GridBagConstraints.BOTH;
        lowerGbc.weightx = 1.0;

        lowerGbc.gridy = 0;
        lowerGbc.weighty = 1.0;
        lowerPanel.add(infoPanel, lowerGbc);

        lowerGbc.gridy = 1;
        lowerGbc.weighty = 1.0;
        lowerPanel.add(guidePanel, lowerGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        gbc.weighty = 20.0;
        add(imagePanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        add(lowerPanel, gbc);

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
