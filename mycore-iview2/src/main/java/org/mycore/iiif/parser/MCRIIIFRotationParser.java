package org.mycore.iiif.parser;


import org.mycore.iiif.model.MCRIIIFImageTargetRotation;

public class MCRIIIFRotationParser {

    private final String rotation;

    public MCRIIIFRotationParser(String rotation) {
        this.rotation = rotation;
    }

    public MCRIIIFImageTargetRotation parse() {
        boolean mirror = this.rotation.startsWith("!");

        String rotationNumberString = mirror ? this.rotation.substring(1) : this.rotation;
        Double rotationNumber;
        try {
            rotationNumber = Double.parseDouble(rotationNumberString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rotationNumberString + " cannot parsed to a rotation value!");
        }

        if (rotationNumber < 0 || rotationNumber > 360) {
            throw new IllegalArgumentException(rotationNumber + " is not a valid rotation value!");
        }

        return new MCRIIIFImageTargetRotation(mirror, rotationNumber);
    }
}
