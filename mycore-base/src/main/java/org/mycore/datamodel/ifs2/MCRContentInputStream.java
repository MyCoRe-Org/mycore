/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;

/**
 * This input stream is used by the MyCoRe filesystem classes to read the
 * content of a file and import it into the System. MCRContentInputStream
 * provides the header of the file that is read (the first 64k) for content type
 * detection purposes, counts the number of bytes read and builds an MD5
 * checksum String while content goes through this input stream.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRContentInputStream extends MCRHeaderInputStream {

    /**
     * Constructs a new MCRContentInputStream
     * 
     * @param in
     *            the InputStream to read from
     * @throws MCRConfigurationException
     *             if java classes supporting MD5 checksums are not found
     */
    public MCRContentInputStream(InputStream in) throws MCRException {
        super(new DigestInputStream(in, buildMD5Digest()));
    }

    private MessageDigest getDigest() {
        return ((DigestInputStream) in).getMessageDigest();
    }

    /**
     * Returns the MD5 message digest that has been built during reading of the
     * underlying input stream.
     * 
     * @return the MD5 message digest checksum of all bytes that have been read
     */
    public byte[] getMD5() {
        return getDigest().digest();
    }

    /**
     * Returns the MD5 checksum as a String
     * 
     * @return the MD5 checksum as a String of hex digits
     */
    public String getMD5String() {
        return getMD5String(getDigest());
    }

    /**
     * Given an MD5 message digest, returns the MD5 checksum as a String
     * 
     * @return the MD5 checksum as a String of hex digits
     */
    public static String getMD5String(MessageDigest digest) {
        byte[] bytes = digest.digest();
        StringBuffer sb = new StringBuffer();

        for (byte b : bytes) {
            String sValue = "0" + Integer.toHexString(b);
            sb.append(sValue.substring(sValue.length() - 2));
        }

        return sb.toString();
    }

    /**
     * Builds a MessageDigest instance for MD5 checksum computation.
     * 
     * @throws MCRConfigurationException
     *             if no java classes that support MD5 algorithm could be found
     */
    public static MessageDigest buildMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exc) {
            String msg = "Could not find java classes that support MD5 checksum algorithm";
            throw new MCRConfigurationException(msg, exc);
        }
    }
}
