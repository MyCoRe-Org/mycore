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

package org.mycore.urn.services;

/**
 * @author shermann
 *
 */
public interface MCRIURNProvider {
    /** Generates a single URN */
    MCRURN generateURN();

    /**
     * Generates multiple urns
     * 
     * @param amount the amount of urn to generate, must be &gt;= 1
     */
    MCRURN[] generateURN(int amount);

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-1</code> up to
     * <code>&lt;base-urn&gt;-amount</code>
     * 
     * @param amount the amount of urn to generate, must be &gt;= 1
     * @param base
     *            the base urn
     */
    MCRURN[] generateURN(int amount, MCRURN base);

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
    MCRURN[] generateURN(int amount, MCRURN base, String setId);

    /**
     * @return the Namespace Specific String (NISS)
     */
    String getNISS();
}
