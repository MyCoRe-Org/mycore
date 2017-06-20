package org.mycore.urn.services;

/**
 * Base implementation for IURNProviders
 * */
public abstract class MCRAbstractURNProvider implements MCRIURNProvider {

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-1</code> up to
     * <code>&lt;base-urn&gt;-amount</code>
     * 
     * @param amount the amount of urn to generate, must be &gt;= 1
     * @param base
     *            the base urn
     */
    public MCRURN[] generateURN(int amount, MCRURN base) {
        if (base == null || amount < 1)
            return null;
        MCRURN[] urn = new MCRURN[amount];

        for (int i = 1; i <= amount; i++) {
            urn[i - 1] = MCRURN.create(base.getSubNamespaces(),
                base.getNamespaceSpecificString() + "-" + this.addLeadingZeroes(amount, i));
        }
        return urn;
    }

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-setId-1</code> up to
     * <code>&lt;base-urn&gt;-setId-amount</code>
     * 
     * @param amount
     *            the amount of urn to generate, must be &gt;= 1
     * @param base
     *            the base urn
     * @param setId
     *            must represent an integer &gt;= 0, e.g. 1, 001 or 00004
     * @return an Array of {@link MCRURN} or <code>null</code> if the base urn is
     *         null or amount &lt;1 or the setID &lt;0
     */
    public MCRURN[] generateURN(int amount, MCRURN base, String setId) {
        if (base == null || amount < 1 || setId == null)
            return null;
        MCRURN[] urn = new MCRURN[amount];

        for (int i = 1; i <= amount; i++) {
            urn[i - 1] = MCRURN.create(base.getSubNamespaces(), base.getNamespaceSpecificString() + "-" + setId + "-"
                + this.addLeadingZeroes(amount, i));
        }
        return urn;
    }

    /**
     * Method adds leading zeroes to the value parameter
     * 
     * @param digits
     *            the amount of digits
     * @param value
     *            the value to which the zeroes to add
     * */
    String addLeadingZeroes(int digits, int value) {
        StringBuilder builder = new StringBuilder();
        String maxS = String.valueOf(digits);
        String valueS = String.valueOf(value);
        int valueSLen = valueS.length();
        int maxSLen = maxS.length();

        /* in this case we must add zeroes */
        if (valueSLen < maxSLen) {
            int zeroesToAdd = maxSLen - valueSLen;
            for (int i = 0; i < zeroesToAdd; i++) {
                builder.append(0);
            }
            return builder.append(valueS).toString();
        }
        /* no need to add zeroes at all */
        return valueS;
    }

    /**
     * @return null
     * */
    @Override
    public String getNISS() {
        return null;
    }

}
