package org.mycore.urn.services;

import java.net.URI;

/**
 * This class represents a URN in the namespace of the German National Library (DNB):
 * urn:nbn:de.
 *
 * Attention:
 * This implementation uses  the DNB namespace specific checksum calculation
 * so it cannot be used for URNs in other namespaces.
 *
 * The syntax is described in:
 * "Policy für die Vergabe von URNs im Namensraum urn:nbn:de"
 * (http://d-nb.info/1029114455/34)
 *
 * Use MCRURN.create(String urnBase) to create a URN from a base string and calculate the checksum
 * and MCRURN.parse(String urn) to parse a complete URN (with checksum included).
 *
 *
 * @author shermann
 * @author Robert Stephan
 */
@Deprecated
public class MCRURN {

    final private static String URN_PREFIX = "urn:nbn:de:";

    /** the part after the prefix
     *     usually the acronym of a German library network and a library specific number number
     *     or a project id*/
    private String subNamespaces;

    /** the part after the first "-" */
    private String namespaceSpecificString;

    /** the checksum */
    private int checksum = -1;

    /**
     * @param the subnamespace
     *            after urn:nbn:de till first "-"
     * @param namespaceSpecificString
     *            after first "-"
     * @throws IllegalArgumentException
     *             if one of the arguments is <code>null</code>
     */
    private MCRURN(String subNamespaces, String namespaceSpecificString, int checksum) {

        if (subNamespaces == null || namespaceSpecificString == null) {
            throw new IllegalArgumentException("All Arguments must not be null.");
        }
        if (subNamespaces.startsWith(URN_PREFIX)) {
            subNamespaces = subNamespaces.substring(URN_PREFIX.length());
        }
        this.subNamespaces = subNamespaces;
        this.namespaceSpecificString = namespaceSpecificString;
        this.checksum = checksum;
    }

    /**
     *
     * @param urn the URN as string (complete, with checksum included)
     *
     * @return the MCRURN Object
     */
    public static MCRURN parse(String urn) {
        if (isValid(urn)) {
            return create(urn.substring(0, urn.length() - 1));
        } else {
            throw new IllegalArgumentException("The given URN is invalid - maybe the checksum is wrong.");
        }
    }

    public static boolean isValid(String urn) {
        MCRURN result = create(urn.substring(0, urn.length() - 1));
        char givenChecksum = urn.charAt(urn.length() - 1);
        if (Character.isDigit(givenChecksum)) {
            int givenChecksumInt = Character.getNumericValue(givenChecksum);
            if (givenChecksumInt == result.getChecksum()) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param subNamespaces
     * @param namespaceSpecificString - the namespace specific string (NISS)
     */
    public static MCRURN create(String subNamespaces, String namespaceSpecificString) {
        return new MCRURN(subNamespaces, namespaceSpecificString,
            calculateChecksum(subNamespaces, namespaceSpecificString));
    }

    /**
     * Creates a new urn by a given string (without checksum).
     * The checksum will calculated and added to the URN.
     *
     * @param urnBaseWithoutChecksum
     *            the string the urn should be created from
     */
    public static MCRURN create(String urnBaseWithoutChecksum) {
        if (urnBaseWithoutChecksum == null) {
            throw new IllegalArgumentException("The parameter cannot be null.");
        }
        // just check if the urn has the correct syntax
        try {
            URI.create(urnBaseWithoutChecksum);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        if (!urnBaseWithoutChecksum.startsWith(URN_PREFIX)) {
            throw new IllegalArgumentException("The URN must start with 'urn:nbn:de:'.");
        }
        String subNamespaces = urnBaseWithoutChecksum.substring(URN_PREFIX.length(),
            urnBaseWithoutChecksum.indexOf("-"));
        String namespaceSpecificString = urnBaseWithoutChecksum.substring(urnBaseWithoutChecksum.indexOf("-") + 1);

        return create(subNamespaces, namespaceSpecificString);

    }

    /**
     * Calculates the checksum of this urn. Checksum is calculated for urn with
     * the following structure <code>urn:nbn:de:&lt;your stuff here&gt;</code>.
     * For other schemas the calculated checksum may not be correct. Thus one
     * should subclass {@link org.mycore.urn.services.MCRURN} and override this
     * method.
     *
     * @return the calculated checksum
     * @see <a href="http://www.persistent-identifier.de/?link=316"
     *      >http://www.persistent-identifier.de/?link=316</a>
     */
    public static int calculateChecksum(String subNamespaces, String namespaceSpecificString) {
        String urnbase = URN_PREFIX + subNamespaces + "-" + namespaceSpecificString;
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

    /**
     * Returns the integer value for a given String for checksum calculation
     *
     * @throws IllegalArgumentException
     *             when the given char is not allowed
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

    /**
     * @return a String representation of this urn
     */
    @Override
    public String toString() {
        return URN_PREFIX + subNamespaces + "-" + namespaceSpecificString + Integer.toString(checksum);
    }

    public String getSubNamespaces() {
        return subNamespaces;
    }

    public String getNamespaceSpecificString() {
        return namespaceSpecificString;
    }

    public int getChecksum() {
        return checksum;
    }
}
