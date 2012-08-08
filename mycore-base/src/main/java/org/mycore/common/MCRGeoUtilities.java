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
        return degree + "° " + minutes + "' " + seconds;
    }
}
