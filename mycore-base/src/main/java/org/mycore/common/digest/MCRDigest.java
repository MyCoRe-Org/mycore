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

package org.mycore.common.digest;

import java.util.Locale;

/**
 * Representing a message digest.
 * This class is sealed to specific digest implementations, ensuring
 * that only permitted classes can extend it.
 *
 * @see MCRMD5Digest
 * @see MCRSHA512Digest
 */
public abstract sealed class MCRDigest permits MCRMD5Digest, MCRSHA512Digest {

    /**
     * Enumeration of supported digest algorithms.
     */
    public enum Algorithm {
        MD5("md5"), SHA512("sha-512");

        final String name;

        Algorithm(String name) {
            this.name = name;
        }

        public String toLowerCase() {
            return name.toLowerCase(Locale.ROOT);
        }

        public String toUpperCase() {
            return name.toUpperCase(Locale.ROOT);
        }

    }

    protected String value;

    /**
     * Constructor for creating a digest instance.
     *
     * @param value The string representation of the digest.
     * @throws MCRDigestValidationException If the digest value is invalid.
     */
    public MCRDigest(String value) throws MCRDigestValidationException {
        this.value = value;
        validate();
    }

    /**
     * Returns the value of the digest.
     *
     * @return Digest value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Validates the digest value.
     *
     * @throws MCRDigestValidationException If the validation fails.
     */
    protected void validate() throws MCRDigestValidationException {
    }

    /**
     * Returns the algorithm used for the digest.
     *
     * @return The digest algorithm.
     */
    public abstract Algorithm getAlgorithm();

}
