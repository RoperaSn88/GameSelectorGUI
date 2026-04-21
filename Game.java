import java.awt.*;
import javax.swing.*;

public class Game {
    public String name;
    public String path;
    public String imagePath;
    public ImageIcon image;
    public ImageIcon backGround;
    public String backgroundVideoPath;
    public ImageIcon backgroundVideo;
    public String Explain;
    public String Tutorial;

    public Game(String name,String path,String imagePath,String backPath,String Exp,String tu){
        this(name, path, imagePath, backPath, Exp, tu, null);
    }

    public Game(String name,String path,String imagePath,String backPath,String Exp,String tu, String backgroundVideoPath){
        this.name=name;
        this.path=path;
        this.imagePath=imagePath;
        this.backgroundVideoPath = backgroundVideoPath;
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
        if (backgroundVideoPath != null && !backgroundVideoPath.isBlank()) {
            backgroundVideo = new ImageIcon(backgroundVideoPath);
            if (backgroundVideo.getImageLoadStatus() != MediaTracker.COMPLETE) {
                System.err.println("背景映像の読み込みに失敗しました: " + backgroundVideoPath);
                backgroundVideo = new ImageIcon();
            }
        } else {
            backgroundVideo = new ImageIcon();
        }

        this.Explain=Exp;
        this.Tutorial = tu;
    }
}
