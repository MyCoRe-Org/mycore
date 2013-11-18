package org.mycore.services.urn;

import java.net.URI;

/**
 * This class represents an urn.
 * 
 * @author shermann
 */
public class MCRURN {
    private static final String[] DEFAULT_NAMESPACE_IDENTIFIERS = new String[] { "nbn", "de" };

    final private static String DEFAULT_SCHEMA = "urn";

    /** the part after the schema of the urn */
    private String[] namespaceIdentifiers;

    /** the part making the urn unique */
    private String namespaceSpecificPart;

    /** the checksum */
    private int checksum;

    /**
     * @param namespaceIdentifiers
     *            [] Namespace identifiers
     * @param namespaceSpecificPart
     *            namespace specific part of the uri
     * @throws IllegalArgumentException
     *             if one of the arguments is <code>null</code>
     */
    public MCRURN(String[] namespaceIdentifiers, String namespaceSpecificPart) {

        if (namespaceIdentifiers == null || namespaceSpecificPart == null) {
            throw new IllegalArgumentException("All Arguments must not be null.");
        }
        this.namespaceIdentifiers = namespaceIdentifiers;
        this.namespaceSpecificPart = namespaceSpecificPart;
        this.checksum = -1;
    }

    /**
     * @return the schema, always returns <code>urn</code>
     */
    public String getSchema() {
        return MCRURN.DEFAULT_SCHEMA;
    }

    /**
     * Creates a new urn by a given string.
     * 
     * @param s
     *            the string the urn should be created from
     * @see MCRURN#normalize()
     */
    public static MCRURN valueOf(String s) {
        if (s == null)
            return null;
        String[] parts = s.split(":");

        if (!parts[0].equals(MCRURN.DEFAULT_SCHEMA))
            return null;
        try {
            // just check wether the urn has the correct syntax
            URI.create(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
        // part[0] is "urn" by default
        String[] namespaceIdentifiersOfTheURNToBe = new String[parts.length - 2];
        System.arraycopy(parts, 1, namespaceIdentifiersOfTheURNToBe, 0, parts.length - 2);

        MCRURN toReturn = new MCRURN(namespaceIdentifiersOfTheURNToBe, parts[parts.length - 1]);
        return toReturn;
    }

    /**
     * @return a copy of the namespace identfiers specific part.
     */
    public String getNamespaceIdentfiersSpecificPart() {
        return new String(namespaceSpecificPart);
    }

    /**
     * @return a copy of the NamespaceIdentfiers array
     */
    public String[] getNamespaceIdentfiers() {
        String[] copy = new String[namespaceIdentifiers.length];
        System.arraycopy(namespaceIdentifiers, 0, copy, 0, namespaceIdentifiers.length);
        return copy;
    }

    /**
     * Calculates the checksum of this urn. Checksum is calculated for urn with
     * the following structure <code>urn:nbn:de:&lt;your stuff here&gt;</code>.
     * For other schemas the calculated checksum may not be correct. Thus one
     * should subclass {@link org.mycore.services.urn.MCRURN} and override this
     * method.
     * 
     * @return the calculated checksum
     * @see <a href="http://www.persistent-identifier.de/?link=316">http://www.

     *      persistent-identifier.de/?link=316</a>
     */
    public int checksum() {
        if (this.checksum != -1) {
            return this.checksum;
        }
        /*
         * get the String representation of this urn and split it. Every single
         * part of the string is one element in the array
         */
        String[] urn = toString().split("");

        /* Convert the String into an integer representation */
        StringBuilder sourceURNConverted = new StringBuilder();
        for (int i = 1; i < urn.length; i++) {
            sourceURNConverted.append(getIntegerAlias(urn[i]));
        }
        /* Split the string again to calculate the product sum */
        urn = sourceURNConverted.toString().split("");
        int productSum = 0;
        for (int i = 1; i < urn.length; i++) {
            productSum += i * Integer.parseInt(urn[i]);
        }
        /*
         * calculation of the ratio, dividing the productSum by the last element
         * of the converted urn
         */
        int q = productSum / Integer.parseInt(urn[urn.length - 1]);
        String[] arr = String.valueOf(q).split("");
        this.checksum = Integer.valueOf(arr[arr.length - 1]);
        return this.checksum;
    }

    /**
     * Returns the integer value for a given String
     * 
     * @see MCRIConcordanceTable
     * @throws IllegalArgumentException
     *             when the given String is null or its size does not equals 1
     */
    private int getIntegerAlias(String s) throws IllegalArgumentException {
        if (s == null || s.length() != 1)
            throw new IllegalArgumentException("Invalid String specified: " + s);
        if (s.equalsIgnoreCase(MCRIConcordanceTable.A))
            return MCRIConcordanceTable._A;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.B))
            return MCRIConcordanceTable._B;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.C))
            return MCRIConcordanceTable._C;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.D))
            return MCRIConcordanceTable._D;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.E))
            return MCRIConcordanceTable._E;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.F))
            return MCRIConcordanceTable._F;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.G))
            return MCRIConcordanceTable._G;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.H))
            return MCRIConcordanceTable._H;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.I))
            return MCRIConcordanceTable._I;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.J))
            return MCRIConcordanceTable._J;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.K))
            return MCRIConcordanceTable._K;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.L))
            return MCRIConcordanceTable._L;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.M))
            return MCRIConcordanceTable._M;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.N))
            return MCRIConcordanceTable._N;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.O))
            return MCRIConcordanceTable._O;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.P))
            return MCRIConcordanceTable._P;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Q))
            return MCRIConcordanceTable._Q;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.R))
            return MCRIConcordanceTable._R;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.S))
            return MCRIConcordanceTable._S;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.T))
            return MCRIConcordanceTable._T;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.U))
            return MCRIConcordanceTable._U;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.V))
            return MCRIConcordanceTable._V;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.W))
            return MCRIConcordanceTable._W;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.X))
            return MCRIConcordanceTable._X;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Y))
            return MCRIConcordanceTable._Y;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Z))
            return MCRIConcordanceTable._Z;
        if (s.equalsIgnoreCase(":"))
            return MCRIConcordanceTable._COLON;
        if (s.equalsIgnoreCase("-"))
            return MCRIConcordanceTable._MINUS;
        if (s.equalsIgnoreCase("0"))
            return MCRIConcordanceTable._0;
        if (s.equalsIgnoreCase("1"))
            return MCRIConcordanceTable._1;
        if (s.equalsIgnoreCase("2"))
            return MCRIConcordanceTable._2;
        if (s.equalsIgnoreCase("3"))
            return MCRIConcordanceTable._3;
        if (s.equalsIgnoreCase("4"))
            return MCRIConcordanceTable._4;
        if (s.equalsIgnoreCase("5"))
            return MCRIConcordanceTable._5;
        if (s.equalsIgnoreCase("6"))
            return MCRIConcordanceTable._6;
        if (s.equalsIgnoreCase("7"))
            return MCRIConcordanceTable._7;
        if (s.equalsIgnoreCase("8"))
            return MCRIConcordanceTable._8;
        if (s.equalsIgnoreCase("9"))
            return MCRIConcordanceTable._9;
        throw new IllegalArgumentException("Invalid String specified: " + s);
    }

    /**
     * Checks whether the URN has a checksum attached or not. Use this method
     * after a an URN has been created through {@link MCRURN#valueOf(String)}.
     * 
     * @return <code>true</code> if the last digit of the urn is a valid
     *         checksum <code>false</code> otherwise.
     */
    public boolean hasChecksumAttached() {
        String toValidate = this.toString();
        String[] singleLetters = toValidate.split("");
        String lastLetter = singleLetters[singleLetters.length - 1];
        int chk = -1;
        try {
            chk = Integer.valueOf(lastLetter);
        } catch (Exception ex) {
            // checksum must end with an integer
            return false;
        }
        String toTest = toValidate.substring(0, toValidate.length() - 1);
        try {
            return MCRURN.valueOf(toTest).checksum() == chk;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes the checksum from the urn, if any
     * 
     * @see MCRURN#valueOf(String)
     */
    public void normalize() {
        if (!this.hasChecksumAttached())
            return;
        namespaceSpecificPart = namespaceSpecificPart.substring(0, namespaceSpecificPart.length() - 1);
    }

    /**
     * @return a String representation of this urn
     */
    @Override
    public String toString() {
        if (namespaceIdentifiers == null || namespaceSpecificPart == null)
            return null;
        StringBuilder urn = new StringBuilder();
        urn.append(MCRURN.DEFAULT_SCHEMA);
        for (String namespaceIdentifier : namespaceIdentifiers) {
            urn.append(":" + namespaceIdentifier);
        }
        urn.append(":" + namespaceSpecificPart);
        return urn.toString();
    }

    /***/
    public static String[] getDefaultNamespaceIdentifiers() {

        String[] arr = new String[MCRURN.DEFAULT_NAMESPACE_IDENTIFIERS.length];
        for (int i = 0; i < MCRURN.DEFAULT_NAMESPACE_IDENTIFIERS.length; i++) {
            arr[i] = new String(MCRURN.DEFAULT_NAMESPACE_IDENTIFIERS[i]);
        }
        return arr;
    }

    /**
     * Generates the checksum and permanently attaches the checksum to the urn.
     * That means if the {@link MCRURN#toString()} is called the checksum will
     * be the last digit.
     * 
     * @return <code>true</code> if the checksum was attached,
     *         <code>false</code> otherwise
     * @see MCRURN#hasChecksumAttached()
     */
    public boolean attachChecksum() {
        this.checksum = checksum();
        this.namespaceSpecificPart += String.valueOf(this.checksum);
        return true;
    }
}