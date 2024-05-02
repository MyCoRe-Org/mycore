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

/**
 * Digest implementation for SHA-512.
 */
public final class MCRSHA512Digest extends MCRDigest {

    /**
     * Creates a new SHA-512 digest instance.
     *
     * @param digest The SHA-512 digest value.
     * @throws MCRDigestValidationException If the digest value is invalid.
     */
    public MCRSHA512Digest(String digest) throws MCRDigestValidationException {
        super(digest);
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.SHA512;
    }

}
