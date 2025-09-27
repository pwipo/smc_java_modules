package ru.smcsystem.modules.module;

import javax.imageio.ImageIO;
import javax.swing.text.Element;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

public class MyImageView extends ImageView {
    private static Method privateMethodUpdateImageSize = null;
    private Image myImage;

    public MyImageView(Element elem, byte[] bytes) {
        super(elem);
        this.myImage = null;
        if (bytes != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                BufferedImage image = ImageIO.read(bis);
                bis.close();
                this.myImage = image;
            } catch (Exception ignore) {
            }
        }

        if (privateMethodUpdateImageSize == null) {
            try {
                privateMethodUpdateImageSize = ImageView.class.getDeclaredMethod("updateImageSize");
                privateMethodUpdateImageSize.setAccessible(true);
                privateMethodUpdateImageSize.invoke(this);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public Image getImage() {
        return myImage != null ? myImage : super.getImage();
    }

    public Image getMyImage() {
        return myImage;
    }

    public void setMyImage(Image myImage) {
        this.myImage = myImage;
        if (privateMethodUpdateImageSize != null) {
            try {
                privateMethodUpdateImageSize.invoke(this);
            } catch (Throwable ignore) {
            }
        }
    }

}
