package org.mycore.iview.tests.image.api;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class DebugBufferedImageWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private BufferedImage imageToShow;

    public DebugBufferedImageWindow(BufferedImage imageToShow) {
        super("Debug-Window");
        this.imageToShow = imageToShow;
        setSize(imageToShow.getWidth(), imageToShow.getHeight());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.drawImage(imageToShow, 0, 0, null);
    }
}
