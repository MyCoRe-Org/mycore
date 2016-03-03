package org.mycore.iiif.model;


public class MCRIIIFImageTargetRotation {

    private final boolean mirrored;
    private final double degrees;

    public MCRIIIFImageTargetRotation(boolean mirrored, double degrees) {
        this.mirrored = mirrored;
        this.degrees = degrees;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public double getDegrees() {
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
