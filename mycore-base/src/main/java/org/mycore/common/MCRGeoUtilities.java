/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * @author shermann
 *
 */
public class MCRGeoUtilities {

    /**
     * Converts coordinates to decimal degree (as used by google maps).
     * 
     */
    public static double toDecimalDegrees(int degree, int minutes, double seconds) {
        return (((seconds / 60) + minutes) / 60) + degree;
    }

    /**
     * Converts coordinates in pica format to decimal degree (as used by google maps).
     *
     * @return the decimal degree representation of the coordinates
     */
    public static double toDecimalDegrees(String picaValue) {
        if (picaValue == null || picaValue.length() == 0 || (!isValid(picaValue))) {
            return 0d;
        }
        String[] strings = picaValue.split(" ");
        if (strings.length < 3) {
            return 0d;
        }

        int degree = Integer.parseInt(strings[1]);
        int minutes = Integer.parseInt(strings[2]);
        double seconds = 0d;

        if (strings.length >= 4) {
            seconds = Double.parseDouble(strings[3]);
        }

        int factor = Objects.equals(strings[0], "W") || Objects.equals(strings[0], "S") ? -1 : 1;
        return ((((seconds / 60) + minutes) / 60) + degree) * factor;
    }

    /**
     * Converts decimal degree to ordinary coordinates.
     * 
     */
    public static String toDegreeMinuteSecond(double inDecimalDegree) {
        int degree = (int) inDecimalDegree;
        int minutes = (int) ((inDecimalDegree - degree) * 60);
        double seconds = ((inDecimalDegree - degree) * 60 - minutes) * 60;

        return new MessageFormat("{0}° {1}'' {2}", Locale.ROOT).format(
            new Object[] { degree, minutes, Math.round(seconds * 100d / 100d) });
    }

    /**
     * @param picaValue the value as stored in opac/pica
     * @return a human readable form like 38° 22′ S
     * 
     * @see #toDegreeMinuteSecond(double)
     */
    public static String toDegreeMinuteSecond(String picaValue) {
        if (picaValue == null || picaValue.length() == 0 || (!isValid(picaValue))) {
            return null;
        }
        String[] strings = picaValue.split(" ");
        if (strings.length < 3) {
            return null;
        }
        double seconds = 0d;

        if (strings.length >= 4) {
            seconds = Double.parseDouble(strings[3]);
        }

        return new MessageFormat("{0}° {1}'' {2} {3}", Locale.ROOT).format(
            new Object[] { Integer.valueOf(strings[1]), Integer.valueOf(strings[2]), Math.round(seconds * 100d / 100d),
                strings[0] });
    }

    private static boolean isValid(String picaValue) {
        String regex = "[EWSN]{1}\\s[0-9]{3}\\s[0-9]{2}(\\s[0-9]*)*";
        return picaValue.matches(regex);
    }
}
