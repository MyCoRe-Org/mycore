/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

/**
 * Digest implementation for MD5.
 */
public final class MCRMD5Digest extends MCRDigest {

    public static MD5Algorithm ALGORITHM;

    static {
        ALGORITHM = new MD5Algorithm();
    }

    /**
     * Creates a new MD5 digest instance.
     *
     * @param digest The MD5 digest value.
     * @throws MCRDigestValidationException If the digest value is invalid.
     */
    public MCRMD5Digest(String digest) throws MCRDigestValidationException {
        super(digest);
    }

    @Override
    public MD5Algorithm getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    protected void validate() throws MCRDigestValidationException {
        if (this.value.length != 16) {
            throw new MCRDigestValidationException("Not a valid digest: " + toHexString());
        }
    }

    public static class MD5Algorithm extends Algorithm {

        public MD5Algorithm() {
            super("md5");
        }

    }

}
