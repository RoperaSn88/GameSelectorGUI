import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JLabel;

class BaseFrame extends JFrame implements KeyListener{
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
                if(!selecterGUI.Gaming)selecterGUI.OpenGameDetailWindow();
                break;
            case KeyEvent.VK_ENTER:
                if(!selecterGUI.Gaming)selecterGUI.OpenGameDetailWindow();
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
