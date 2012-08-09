package org.mycore.common;

import java.text.MessageFormat;

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
     * Converts coordinates in pica format to decimal degree (as used by google maps).
     *
     * @param picaValue
     * 
     * @return the decimal degree representation of the coordinates
     */
    public static double toDecimalDegrees(String picaValue) {
        if (picaValue == null || picaValue.length() == 0) {
            return 0d;
        }
        String[] strings = picaValue.split(" ");
        if (strings.length < 3) {
            return 0d;
        }

        int degree = Integer.valueOf(strings[1]);
        int minutes = Integer.valueOf(strings[2]);
        double seconds = 0d;

        if (strings.length >= 4) {
            seconds = Double.valueOf(strings[3]);
        }

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

        return MessageFormat.format("{0}° {1}'' {2}", degree, minutes, Math.round(seconds * 100d) / 100d);
    }

    /**
     * @param picaValue the value as stored in opac/pica
     * @return a human readable form like 38° 22′ S
     * 
     * @see {@link MCRGeoUtilities#toDegreeMinuteSecond(double)}
     */
    public static String toDegreeMinuteSecond(String picaValue) {
        if (picaValue == null || picaValue.length() == 0) {
            return null;
        }
        String[] strings = picaValue.split(" ");
        if (strings.length < 3) {
            return null;
        }
        double seconds = 0d;

        if (strings.length >= 4) {
            seconds = Double.valueOf(strings[3]);
        }

        return MessageFormat.format("{0}° {1}'' {2} {3}", Integer.valueOf(strings[1]), Integer.valueOf(strings[2]),
                Math.round(seconds * 100d) / 100d, strings[0]);
    }
}
