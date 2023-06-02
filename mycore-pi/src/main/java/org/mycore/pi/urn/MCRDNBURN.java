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

package org.mycore.pi.urn;

/**
 * Base class for every DNBURN
 *
 * @author Sebastian Hofmann
 * @author shermann
 * @author Robert Stephan
 */
public class MCRDNBURN extends MCRUniformResourceName {
    public static final String TYPE = "dnbUrn";

    public static final String URN_NID = "nbn:de:";

    public MCRDNBURN(String subNamespace, String namespaceSpecificString) {
        super(subNamespace, namespaceSpecificString);
    }

    /**
     * Returns the integer value for a given String for checksum calculation
     *
     * @throws IllegalArgumentException when the given char is not allowed
     */
    private static int getIntegerAlias(char c) throws IllegalArgumentException {
        return switch (c) {
            case '0' -> 1;
            case '1' -> 2;
            case '2' -> 3;
            case '3' -> 4;
            case '4' -> 5;
            case '5' -> 6;
            case '6' -> 7;
            case '7' -> 8;
            case '8' -> 9;
            case '9' -> 41;

            /* Letters */
            case 'A', 'a' -> 18;
            case 'B', 'b' -> 14;
            case 'C', 'c' -> 19;
            case 'D', 'd' -> 15;
            case 'E', 'e' -> 16;
            case 'F', 'f' -> 21;
            case 'G', 'g' -> 22;
            case 'H', 'h' -> 23;
            case 'I', 'i' -> 24;
            case 'J', 'j' -> 25;
            case 'K', 'k' -> 42;
            case 'L', 'l' -> 26;
            case 'M', 'm' -> 27;
            case 'N', 'n' -> 13;
            case 'O', 'o' -> 28;
            case 'P', 'p' -> 29;
            case 'Q', 'q' -> 31;
            case 'R', 'r' -> 12;
            case 'S', 's' -> 32;
            case 'T', 't' -> 33;
            case 'U', 'u' -> 11;
            case 'V', 'v' -> 34;
            case 'W', 'w' -> 35;
            case 'X', 'x' -> 36;
            case 'Y', 'y' -> 37;
            case 'Z', 'z' -> 38;

            /* Special chars */
            case '-' -> 39;
            case ':' -> 17;
            case '_' -> 43;
            case '.' -> 47;
            case '/' -> 45;
            case '+' -> 49;
            default -> throw new IllegalArgumentException("Invalid Character specified: " + c);
        };
    }

    @Override
    public String getPREFIX() {
        return super.getPREFIX() + URN_NID;
    }

    /**
     * Method adds leading zeroes to the value parameter
     *
     * @param digits the amount of digits
     * @param value  the value to which the zeroes to add
     */
    protected String addLeadingZeroes(int digits, int value) {
        StringBuilder builder = new StringBuilder();
        String maxS = String.valueOf(digits);
        String valueS = String.valueOf(value);
        int valueSLen = valueS.length();
        int maxSLen = maxS.length();

        /* in this case we must add zeroes */
        if (valueSLen < maxSLen) {
            int zeroesToAdd = maxSLen - valueSLen;
            builder.append("0".repeat(zeroesToAdd));
            return builder.append(valueS).toString();
        }
        /* no need to add zeroes at all */
        return valueS;
    }

    public MCRDNBURN toGranular(String setID, String index) {
        return new MCRDNBURN(getSubNamespace(), getGranularNamespaceSpecificString(setID, index));
    }

    public MCRDNBURN withSuffix(String suffix) {
        return new MCRDNBURN(getSubNamespace(), getNamespaceSpecificString() + suffix);
    }

    public MCRDNBURN withNamespaceSuffix(String suffix) {
        return new MCRDNBURN(getSubNamespace() + suffix, getNamespaceSpecificString());
    }

    public MCRDNBURN toGranular(String setID, int i, int max) {
        return toGranular(setID, addLeadingZeroes(max, i));
    }

    private String getGranularNamespaceSpecificString(String setID, String index) {
        return getNamespaceSpecificString() + "-" + setID + "-" + index;
    }

    @Override
    public String getNamespaceSpecificString() {
        return super.getNamespaceSpecificString() + calculateChecksum();
    }

    /**
     * Calculates the checksum of this urn. Checksum is calculated for urn with
     * the following structure <code>urn:nbn:de:&lt;your stuff here&gt;</code>.
     * For other schemas the calculated checksum may not be correct.
     *
     * @return the calculated checksum
     * @see <a href="http://www.persistent-identifier.de/?link=316"
     * >http://www.persistent-identifier.de/?link=316</a>
     */
    public int calculateChecksum() {
        String urnbase = getPREFIX() + subNamespace + this.namespaceSpecificString;
        char[] urn = urnbase.toCharArray();

        /* Convert the String into an integer representation */
        StringBuilder sourceURNConverted = new StringBuilder();
        for (int i = 1; i <= urn.length; i++) {
            sourceURNConverted.append(getIntegerAlias(urn[i - 1]));
        }
        /* Split the string again to calculate the product sum */
        urn = sourceURNConverted.toString().toCharArray();

        int productSum = 0;
        for (int i = 1; i <= urn.length; i++) {
            productSum += i * Character.getNumericValue(urn[i - 1]);
        }
        /*
         * calculation of the ratio, dividing the productSum by the last element
         * of the converted urn
         */
        int q = productSum / Character.getNumericValue(urn[urn.length - 1]);

        return q % 10;
    }
}
