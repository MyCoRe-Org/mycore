/**
 * 
 */
package org.mycore.handle;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Class provides functionality for calculating the checksum of a {@link MCRHandle}. 
 * 
 * @author shermann
 */
public class MCRGbvHandleChecksumProvider {

    private static final Charset HANDLE_CHARSET = StandardCharsets.UTF_8;

    /**
     * Calculates the checksum of the given handle. 
     */
    public int checksum(MCRHandle handle) {
        String combine = MCRGbvHandleProvider.URN_PREFIX + handle.getLocalName();

        byte[] bytes = combine.getBytes(HANDLE_CHARSET);

        Checksum chk = new CRC32();
        chk.update(bytes, 0, bytes.length);

        return Math.abs((int) chk.getValue() % 10);
    }

    /**
     * Calculates the checksum of the given handle as described in <a href="http://www.persistent-identifier.de/?link=316">"Beschreibung des Algorithmus zur Berechnung der URN-Prüfziffer "</a>
     * @return the single digit checksum for this handle
     */
    public int checksum2(MCRHandle handle) {
        String handleAsURN = MCRGbvHandleProvider.URN_PREFIX + handle.getLocalName();

        /*
         * get the String representation of this urn and split it. Every single
         * part of the string is one element in the array
         */
        char[] urn = handleAsURN.toCharArray();

        /* Convert the String into an integer representation */
        StringBuilder sourceURNConverted = new StringBuilder();
        for (int i = 0; i < urn.length; i++) {
            sourceURNConverted.append(getIntegerAlias(urn[i]));
        }
        /* Split the string again to calculate the product sum */
        urn = sourceURNConverted.toString().toCharArray();
        int productSum = 0;
        for (int i = 0; i < urn.length; i++) {
            productSum += i * Character.getNumericValue(urn[i]);
        }
        /*
         * calculation of the ratio, dividing the productSum by the last element
         * of the converted urn
         */
        int q = productSum / Character.getNumericValue(urn[urn.length - 1]);
        return q % 10;
    }

    /**
     * Returns the integer value for a given String
     * 
     * @see MCRURN.getIntegerAlias()
     * @throws IllegalArgumentException
     *             when the given String is null or its size does not equals 1
     */
    private static int getIntegerAlias(char c) throws IllegalArgumentException {
        switch (c) {
            case '0':
                return 1;
            case '1':
                return 2;
            case '2':
                return 3;
            case '3':
                return 4;
            case '4':
                return 5;
            case '5':
                return 6;
            case '6':
                return 7;
            case '7':
                return 8;
            case '8':
                return 9;
            case '9':
                return 41;

            /* Letters */
            case 'A':
            case 'a':
                return 18;
            case 'B':
            case 'b':
                return 14;
            case 'C':
            case 'c':
                return 19;
            case 'D':
            case 'd':
                return 15;
            case 'E':
            case 'e':
                return 16;
            case 'F':
            case 'f':
                return 21;
            case 'G':
            case 'g':
                return 22;
            case 'H':
            case 'h':
                return 23;
            case 'I':
            case 'i':
                return 24;
            case 'J':
            case 'j':
                return 25;
            case 'K':
            case 'k':
                return 42;
            case 'L':
            case 'l':
                return 26;
            case 'M':
            case 'm':
                return 27;
            case 'N':
            case 'n':
                return 13;
            case 'O':
            case 'o':
                return 28;
            case 'P':
            case 'p':
                return 29;
            case 'Q':
            case 'q':
                return 31;
            case 'R':
            case 'r':
                return 12;
            case 'S':
            case 's':
                return 32;
            case 'T':
            case 't':
                return 33;
            case 'U':
            case 'u':
                return 11;
            case 'V':
            case 'v':
                return 34;
            case 'W':
            case 'w':
                return 35;
            case 'X':
            case 'x':
                return 36;
            case 'Y':
            case 'y':
                return 37;
            case 'Z':
            case 'z':
                return 38;

            /* Special chars */
            case '-':
                return 39;
            case ':':
                return 17;
            case '_':
                return 43;
            case '.':
                return 47;
            case '/':
                return 45;
            case '+':
                return 49;
        }
        throw new IllegalArgumentException("Invalid Character specified: " + c);
    }
}
