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

package org.mycore.common.content.streams;

import java.io.InputStream;
import java.security.DigestInputStream;

import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRDigest;

/**
 * Builds a checksum String while content goes through this input stream.
 */
public class MCRDigestInputStream extends DigestInputStream {

    public MCRDigestInputStream(InputStream stream, MCRDigest.Algorithm digestAlgorithm) {
        super(stream, MCRUtils.buildMessageDigest(digestAlgorithm));
    }

    /**
     * Returns the message digest that has been built during reading of the underlying input stream.
     *
     * @return the message digest checksum of all bytes that have been read
     */
    public byte[] getDigest() {
        return getMessageDigest().digest();
    }

    /**
     * Returns the digest checksum as a hex String
     *
     * @return the digest checksum as a String of hex digits
     */
    public String getDigestAsHexString() {
        return MCRUtils.toHexString(getDigest());
    }

}
