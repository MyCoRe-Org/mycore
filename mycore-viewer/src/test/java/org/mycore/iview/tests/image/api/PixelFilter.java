package org.mycore.iview.tests.image.api;

public interface PixelFilter {
    /**
     * Filters a Pixel
     * @param pixel the pixel to check
     * @return true if the pixel should be appear in new Selection
     */
    boolean filter(Pixel pixel);
}
