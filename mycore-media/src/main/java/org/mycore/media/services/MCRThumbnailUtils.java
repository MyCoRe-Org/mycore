package org.mycore.media.services;

import java.awt.image.BufferedImage;

public class MCRThumbnailUtils {

    public static int getImageType(BufferedImage image) {
        int colorType = 12;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int colorTypeTemp = getColorType(image.getRGB(x,y));
                if (colorTypeTemp == BufferedImage.TYPE_4BYTE_ABGR) {
                    return colorTypeTemp;
                }
                colorType = colorTypeTemp < colorType ? colorTypeTemp : colorType;
            }
        }
        return colorType;
    }

    public static int getColorType(int color) {
        int alpha = (color >> 24) & 0xff;
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = (color) & 0xff;

        if (alpha < 255) {
            return BufferedImage.TYPE_4BYTE_ABGR;
        }
        if (red == green && green == blue) {
            if (red == 255 || red == 0) {
                return BufferedImage.TYPE_BYTE_BINARY;
            }
            return BufferedImage.TYPE_BYTE_GRAY;
        }
        return BufferedImage.TYPE_3BYTE_BGR;
    }
}
