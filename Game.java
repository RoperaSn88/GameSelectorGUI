import java.awt.*;
import javax.swing.*;

public class Game {
    public String name;
    public String path;
    public String imagePath;
    public ImageIcon image;
    public ImageIcon backGround;
    public String Explain;
    public String Tutorial;

    public Game(String name,String path,String imagePath,String backPath,String Exp,String tu){
        this.name=name;
        this.path=path;
        this.imagePath=imagePath;
        this.image = loadIcon(imagePath);
        if (image == null) {
            System.err.println("アイコンの読み込みに失敗しました: " + imagePath);
            image = new ImageIcon();
        }
        // 背景が未指定・読み込み失敗の場合はアイコン画像を背景として使う
        this.backGround = loadIcon(backPath);
        if (backGround == null) {
            if (backPath != null && !backPath.trim().isEmpty()) {
                System.err.println("背景の読み込みに失敗しました: " + backPath);
            }
            backGround = image;
        }
        this.Explain=Exp;
        this.Tutorial = tu;
    }

    // 画像を読み込む。パスが空、または読み込みに失敗した場合は null を返す
    private static ImageIcon loadIcon(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        ImageIcon icon = new ImageIcon(path);
        return icon.getImageLoadStatus() == MediaTracker.COMPLETE ? icon : null;
    }
}
