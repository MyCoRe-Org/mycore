package org.mycore.iiif.model;


import java.util.Locale;

public class MCRIIIFImageTargetSize {

    private final int width;
    private final int height;

    public MCRIIIFImageTargetSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRIIIFImageTargetSize
                && ((MCRIIIFImageTargetSize) obj).width == this.width
                && ((MCRIIIFImageTargetSize) obj).height == this.height;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%d,%d]", width, height);
    }
}
