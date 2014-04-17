/**
 * 
 */
package org.mycore.urn.services;

/**
 * @author shermann
 *
 */
public interface MCRIURNProvider {
    /** Generates a single URN */
    public MCRURN generateURN();

    /**
     * Generates multiple urns
     * 
     * @param amount the amount of urn to generate, must be &gt;= 1
     */
    public MCRURN[] generateURN(int amount);

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-1</code> up to
     * <code>&lt;base-urn&gt;-amount</code>
     * 
     * @param amount the amount of urn to generate, must be &gt;= 1
     * @param base
     *            the base urn
     */
    public MCRURN[] generateURN(int amount, MCRURN base);

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
    public MCRURN[] generateURN(int amount, MCRURN base, String setId);

    /**
     * @return the Namespace Specific String (NISS)
     */
    public String getNISS();
}
