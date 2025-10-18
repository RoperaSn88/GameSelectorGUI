import java.awt.*;
import javax.swing.*;

public class Game {
    public String name;
    public String path;
    public String imagePath;
    public ImageIcon image;
    public ImageIcon backGround;
    public String Explain;

    public Game(String name,String path,String imagePath,String backPath,String Exp){
        this.name=name;
        this.path=path;
        this.imagePath=imagePath;
        this.image=new ImageIcon(imagePath);
        this.backGround = new ImageIcon(backPath);
        if (image.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("アイコンの読み込みに失敗しました: " + imagePath);
            image = new ImageIcon(); 
        }
        if (backGround.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("アイコンの読み込みに失敗しました: " + backPath);
            backGround = new ImageIcon(); 
        }

        this.Explain=Exp;

        System.out.println("name:" + this.name + " path:" + this.path + " imagePath:" + this.imagePath + " backPath:" + this.backGround + " exp:" + Explain);
    }
}
