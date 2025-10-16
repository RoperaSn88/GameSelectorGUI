import java.awt.*;
import javax.swing.*;

public class Game {
    public String name;
    public String path;
    public String imagePath;
    public ImageIcon image;
    public ImageIcon backGround;
    public String Explain;

    public Game(String name,String path,String imagePath,String Exp){
        this.name=name;
        this.path=path;
        this.imagePath=imagePath;
        this.image=new ImageIcon(imagePath);
        this.backGround = new ImageIcon(imagePath);
        if (image.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("画像の読み込みに失敗しました: " + path);
            image = new ImageIcon(); 
            backGround = new ImageIcon();
        }

        this.Explain=Exp;
    }
}
