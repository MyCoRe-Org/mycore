package org.mycore.iiif.model;


public class MCRIIIFImageTargetRotation {

    private final boolean mirrored;
    private final int degrees;

    public MCRIIIFImageTargetRotation(boolean mirrored, int degrees) {
        this.mirrored = mirrored;
        this.degrees = degrees;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public int getDegrees() {
        return degrees;
    }

    @Override
    public String toString() {
        return "[" + mirrored + "," + degrees + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRIIIFImageTargetRotation
                && ((MCRIIIFImageTargetRotation) obj).degrees == degrees
                && ((MCRIIIFImageTargetRotation) obj).mirrored == mirrored;
    }
}
