package org.mycore.iiif.model;


import java.util.Locale;

public enum MCRIIIFImageQuality {
    color, gray, bitonal;


    public static final MCRIIIFImageQuality fromString(String str) {
        switch (str.toLowerCase(Locale.ENGLISH)) {
            case "color":
            case "default":
                return color;
            case "gray":
                return gray;
            case "bitonal":
                return bitonal;
            default:
                throw new IllegalArgumentException(str + " is no valid ImageQuality!");
        }
    }
}
