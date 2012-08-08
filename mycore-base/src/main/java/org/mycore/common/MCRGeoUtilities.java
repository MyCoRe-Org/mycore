package org.mycore.common;

/**
 * @author shermann
 *
 */
public class MCRGeoUtilities {

    /**
     * Converts coordinates to decimal degree (as used by google maps).
     * 
     * @param degree
     * @param minutes
     * @param seconds
     * 
     * @return
     */
    public static double toDecimalDegrees(int degree, int minutes, double seconds) {
        return (((seconds / 60) + minutes) / 60) + degree;
    }

    /**
     * Converts decimal degree to ordinary coordinates.
     * 
     * @param inDecimalDegree
     * 
     * @return
     */
    public static String toDegreeMinuteSecond(double inDecimalDegree) {
        int degree = (int) inDecimalDegree;
        int minutes = (int) ((inDecimalDegree - degree) * 60);
        double seconds = ((inDecimalDegree - degree) * 60 - minutes) * 60;

        return degree + "° " + minutes + "' " + Math.round(seconds * 100000d) / 100000d;
    }

    /**
     * TODO support seconds :)
     * 
     * @param picaValue the value as stored in opac/pica
     * @return a human readable form like 38° 22′ S
     */
    public static String toDegreeMinuteSecond(String picaValue) {
        if (picaValue == null || picaValue.length() == 0) {
            return null;
        }
        String[] strings = picaValue.split(" ");
        if (strings.length < 3) {
            return null;
        }
        String degreeMinuteSecond = toDegreeMinuteSecond(toDecimalDegrees(Integer.valueOf(strings[1]), Integer.valueOf(strings[2]), 0d));

        return degreeMinuteSecond + " " + strings[0];
    }
}
