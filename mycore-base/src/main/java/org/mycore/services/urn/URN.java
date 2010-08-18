package org.mycore.services.urn;

import java.net.URI;

/**
 * This class represents an urn.
 * 
 * @author shermann
 */
public class URN {
    private static final String[] DEFAULT_NAMESPACE_IDENTIFIERS = new String[] { "nbn", "de" };

    final private static String DEFAULT_SCHEMA = "urn";

    /** the part after the schema of the urn */
    private String[] namespaceIdentifiers;

    /** the part making the urn unique */
    private String namespaceSpecificPart;

    /** the checksum */
    private int checksum;

    /**
     * @param String
     *            [] Namespace identiefiers
     * @param String
     *            namespace specific part of the uri
     * @throws IllegalArgumentException
     *             if one of the arguments is <code>null</code>
     * */
    public URN(String[] namespaceIdentifiers, String namespaceSpecificPart) {

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
        return URN.DEFAULT_SCHEMA;
    }

    /**
     * Creates a new urn by a given string.
     * 
     * @param String
     *            the string the urn should be created from
     * @see URN#normalize()
     * */
    public static URN valueOf(String s) {
        if (s == null)
            return null;
        String[] parts = s.split(":");

        if (!parts[0].equals(URN.DEFAULT_SCHEMA))
            return null;
        try {
            // just check wether the urn has the correct syntax
            URI.create(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
        // part[0] is "urn" by default
        String[] namespaceIdentifiersOfTheURNToBe = new String[parts.length - 2];
        for (int i = 0; i < parts.length - 2; i++) {
            namespaceIdentifiersOfTheURNToBe[i] = parts[i + 1];
        }

        URN toReturn = new URN(namespaceIdentifiersOfTheURNToBe, parts[parts.length - 1]);
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
     * should subclass {@link org.mycore.services.urn.URN} and override this
     * method.
     * 
     * @return the calculated checksum
     * @see <a href="http://www.persistent-identifier.de/?link=316">http://www.

     *      persistent-identifier.de/?link=316</a>
     */
    public int checksum() throws Exception {
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
     * @see IConcordanceTable
     * @throws IllegalArgumentException
     *             when the given String is null or its size does not equals 1
     */
    private int getIntegerAlias(String s) throws IllegalArgumentException {
        if (s == null || s.length() != 1)
            throw new IllegalArgumentException("Invalid String specified: " + s);
        if (s.equalsIgnoreCase(IConcordanceTable.A))
            return IConcordanceTable._A;
        if (s.equalsIgnoreCase(IConcordanceTable.B))
            return IConcordanceTable._B;
        if (s.equalsIgnoreCase(IConcordanceTable.C))
            return IConcordanceTable._C;
        if (s.equalsIgnoreCase(IConcordanceTable.D))
            return IConcordanceTable._D;
        if (s.equalsIgnoreCase(IConcordanceTable.E))
            return IConcordanceTable._E;
        if (s.equalsIgnoreCase(IConcordanceTable.F))
            return IConcordanceTable._F;
        if (s.equalsIgnoreCase(IConcordanceTable.G))
            return IConcordanceTable._G;
        if (s.equalsIgnoreCase(IConcordanceTable.H))
            return IConcordanceTable._H;
        if (s.equalsIgnoreCase(IConcordanceTable.I))
            return IConcordanceTable._I;
        if (s.equalsIgnoreCase(IConcordanceTable.J))
            return IConcordanceTable._J;
        if (s.equalsIgnoreCase(IConcordanceTable.K))
            return IConcordanceTable._K;
        if (s.equalsIgnoreCase(IConcordanceTable.L))
            return IConcordanceTable._L;
        if (s.equalsIgnoreCase(IConcordanceTable.M))
            return IConcordanceTable._M;
        if (s.equalsIgnoreCase(IConcordanceTable.N))
            return IConcordanceTable._N;
        if (s.equalsIgnoreCase(IConcordanceTable.O))
            return IConcordanceTable._O;
        if (s.equalsIgnoreCase(IConcordanceTable.P))
            return IConcordanceTable._P;
        if (s.equalsIgnoreCase(IConcordanceTable.Q))
            return IConcordanceTable._Q;
        if (s.equalsIgnoreCase(IConcordanceTable.R))
            return IConcordanceTable._R;
        if (s.equalsIgnoreCase(IConcordanceTable.S))
            return IConcordanceTable._S;
        if (s.equalsIgnoreCase(IConcordanceTable.T))
            return IConcordanceTable._T;
        if (s.equalsIgnoreCase(IConcordanceTable.U))
            return IConcordanceTable._U;
        if (s.equalsIgnoreCase(IConcordanceTable.V))
            return IConcordanceTable._V;
        if (s.equalsIgnoreCase(IConcordanceTable.W))
            return IConcordanceTable._W;
        if (s.equalsIgnoreCase(IConcordanceTable.X))
            return IConcordanceTable._X;
        if (s.equalsIgnoreCase(IConcordanceTable.Y))
            return IConcordanceTable._Y;
        if (s.equalsIgnoreCase(IConcordanceTable.Z))
            return IConcordanceTable._Z;
        if (s.equalsIgnoreCase(":"))
            return IConcordanceTable._COLON;
        if (s.equalsIgnoreCase("-"))
            return IConcordanceTable._MINUS;
        if (s.equalsIgnoreCase("0"))
            return IConcordanceTable._0;
        if (s.equalsIgnoreCase("1"))
            return IConcordanceTable._1;
        if (s.equalsIgnoreCase("2"))
            return IConcordanceTable._2;
        if (s.equalsIgnoreCase("3"))
            return IConcordanceTable._3;
        if (s.equalsIgnoreCase("4"))
            return IConcordanceTable._4;
        if (s.equalsIgnoreCase("5"))
            return IConcordanceTable._5;
        if (s.equalsIgnoreCase("6"))
            return IConcordanceTable._6;
        if (s.equalsIgnoreCase("7"))
            return IConcordanceTable._7;
        if (s.equalsIgnoreCase("8"))
            return IConcordanceTable._8;
        if (s.equalsIgnoreCase("9"))
            return IConcordanceTable._9;
        throw new IllegalArgumentException("Invalid String specified: " + s);
    }

    /**
     * Checks whether the URN has a checksum attached or not. Use this method
     * after a an URN has been created through {@link URN#valueOf(String)}.
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
            if (URN.valueOf(toTest).checksum() == chk) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes the checksum from the urn, if any
     * 
     * @see URN#valueOf(String)
     */
    public void normalize() {
        if (this.hasChecksumAttached() == false)
            return;
        namespaceSpecificPart = namespaceSpecificPart.substring(0, namespaceSpecificPart.length() - 1);
    }

    /** @return a String representation of this urn without the trailing checksum */
    @Override
    public String toString() {
        if (namespaceIdentifiers == null || namespaceSpecificPart == null)
            return null;
        StringBuilder urn = new StringBuilder();
        urn.append(URN.DEFAULT_SCHEMA);
        for (int i = 0; i < namespaceIdentifiers.length; i++) {
            urn.append(":" + namespaceIdentifiers[i]);
        }
        urn.append(":" + namespaceSpecificPart);
        return urn.toString();
    }

    /***/
    public static String[] getDefaultNamespaceIdentifiers() {

        String[] arr = new String[URN.DEFAULT_NAMESPACE_IDENTIFIERS.length];
        for (int i = 0; i < URN.DEFAULT_NAMESPACE_IDENTIFIERS.length; i++) {
            arr[i] = new String(URN.DEFAULT_NAMESPACE_IDENTIFIERS[i]);
        }
        return arr;
    }

    /**
     * Generates the checksum and permanently attaches the checksum to the urn.
     * That means if the {@link URN#toString()} is called the checksum will be
     * the last digit.
     * 
     * @return <code>true</code> if the checksum was attached,
     *         <code>false</code> otherwise
     * @see {@link URN#hasChecksumAttached()}
     */
    public boolean attachChecksum() throws Exception {
        if (this.hasChecksumAttached()) {
            return false;
        }
        this.checksum = checksum();
        this.namespaceSpecificPart += String.valueOf(this.checksum);
        return true;
    }
}