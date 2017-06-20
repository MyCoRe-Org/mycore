package org.mycore.iview.tests.image.api;

import java.awt.Color;

public class ColorFilter implements PixelFilter {

    public ColorFilter(Color colorToFilter, boolean throwOut, int tolerance) {
        this.colorToFilter = colorToFilter;
        this.throwOut = throwOut;
        this.tolerance = tolerance;
    }

    private final Color colorToFilter;

    private final boolean throwOut;

    private final int tolerance;

    @Override
    public boolean filter(Pixel pixel) {
        Color color = pixel.getColor();
        if (color.equals(colorToFilter)) {
            return true ^ !(throwOut);
        } else {
            int red = color.getRed();
            int blue = color.getBlue();
            int green = color.getGreen();

            int redDiff = Math.abs(red - colorToFilter.getRed());
            int blueDiff = Math.abs(green - colorToFilter.getGreen());
            int greenDiff = Math.abs(blue - colorToFilter.getBlue());

            return (redDiff + blueDiff + greenDiff > tolerance) ^ !(throwOut);
        }
    }
}
