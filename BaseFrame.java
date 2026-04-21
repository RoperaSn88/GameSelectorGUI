import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JLabel;

class BaseFrame extends JFrame implements KeyListener{
    GameSelectorGUI selectorGUI;
    Container c;

    public BaseFrame(String name,GameSelectorGUI selector){
        super(name);
        c = getContentPane();
        this.selectorGUI = selector;
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
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                if (!selectorGUI.Gaming)selectorGUI.PushUpAction();
                break;
            case KeyEvent.VK_DOWN:
                if (!selectorGUI.Gaming)selectorGUI.PushDownAction();
                break;
            case KeyEvent.VK_W:
                if (!selectorGUI.Gaming)selectorGUI.PushUpAction();
                break;
            case KeyEvent.VK_S:
                if (!selectorGUI.Gaming)selectorGUI.PushDownAction();
                break;
            case KeyEvent.VK_SPACE:
                if (!selectorGUI.Gaming)selectorGUI.OpenGameDetailWindow();
                break;
            case KeyEvent.VK_ENTER:
                if (!selectorGUI.Gaming)selectorGUI.OpenGameDetailWindow();
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
