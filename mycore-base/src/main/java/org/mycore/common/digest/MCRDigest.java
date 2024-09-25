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

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

import org.mycore.common.MCRUtils;

/**
 * Representing a message digest.
 *
 * @see MCRMD5Digest
 * @see MCRSHA512Digest
 */
public abstract class MCRDigest {

    protected byte[] value;

    /**
     * Constructor for creating a digest instance.
     *
     * @param value The string representation of the digest.
     * @throws MCRDigestValidationException If the digest value is invalid.
     */
    public MCRDigest(String value) throws MCRDigestValidationException {
        this.value = HexFormat.of().parseHex(value);
        validate();
    }

    /**
     * Constructor for creating a digest instance.
     *
     * @param value The string representation of the digest.
     * @throws MCRDigestValidationException If the digest value is invalid.
     */
    public MCRDigest(byte[] value) throws MCRDigestValidationException {
        this.value = value;
        validate();
    }

    /**
     * Returns the value of the digest.
     *
     * @return Digest value.
     */
    public String toHexString() {
        return MCRUtils.toHexString(this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MCRDigest mcrDigest = (MCRDigest) o;
        return Arrays.equals(value, mcrDigest.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Validates the digest value.
     *
     * @throws MCRDigestValidationException If the validation fails.
     */
    protected void validate() throws MCRDigestValidationException {
        // by default nothing to validate. this is implementation specific.
    }

    /**
     * Returns the algorithm used for the digest.
     *
     * @return The digest algorithm.
     */
    public abstract Algorithm getAlgorithm();

    /**
     * Digest Algorithm
     */
    public static abstract class Algorithm {

        final String name;

        protected Algorithm(String name) {
            this.name = name;
        }

        public String toLowerCase() {
            return name.toLowerCase(Locale.ROOT);
        }

        public String toUpperCase() {
            return name.toUpperCase(Locale.ROOT);
        }

    }

}