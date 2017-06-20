/*
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.ifs;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRBlockingInputStream;

/**
 * This input stream is used by the MyCoRe filesystem classes to read the
 * content of a file and import it into the System. MCRContentInputStream
 * provides the header of the file that is read (the first 64k) for content type
 * detection purposes, counts the number of bytes read and builds an MD5
 * checksum String while content goes through this input stream.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRContentInputStream extends FilterInputStream {
    /** The number of bytes that will be read for content type detection */
    protected final static int headerSize = 65536;

    /** The MD5 checksum of all bytes read through this stream */
    protected byte[] md5 = null;

    /** The message digest to build the MD5 checksum */
    protected MessageDigest digest = null;

    /** The total number of bytes read so far */
    protected long length;

    /** The header of the file read */
    protected byte[] header;

    /**
     * Constructs a new MCRContentInputStream
     * 
     * @param in
     *            the InputStream to read from
     * @throws MCRConfigurationException
     *             if java classes supporting MD5 checksums are not found
     */
    public MCRContentInputStream(InputStream in) throws MCRException {
        super(null);

        digest = buildMD5Digest();

        DigestInputStream dis = new DigestInputStream(in, digest);
        MCRBlockingInputStream bis = new MCRBlockingInputStream(dis, headerSize);

        byte[] buffer = new byte[headerSize];

        try {
            int num = bis.read(buffer, 0, buffer.length);
            header = new byte[Math.max(0, num)];

            if (num > 0) {
                System.arraycopy(buffer, 0, header, 0, num);
            }
        } catch (IOException ex) {
            String msg = "Error while reading content input stream header";
            throw new MCRException(msg, ex);
        }

        this.in = bis;
    }

    public int consume() throws IOException {
        byte[] buffer = new byte[4096];
        int numRead, totalRead = 0;
        do {
            numRead = read(buffer);
            if (numRead > 0) {
                totalRead += numRead;
            }
        } while (numRead != -1);
        return totalRead;
    }

    @Override
    public int read() throws IOException {
        int b;

        // if current position is in header buffer, return value from there
        if (header.length > 0 && length < header.length) {
            b = header[(int) length];
            length++;
        } else {
            b = super.read();
            if (b != -1) {
                length++;
            }
        }

        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        // if current position is in header buffer, return bytes from there
        if (header.length > 0 && length < header.length) {
            int numAvail = header.length - (int) length;
            len = Math.min(len, numAvail);
            System.arraycopy(header, (int) length, buf, off, len);
            length += len;
            return len;
        } else {
            len = super.read(buf, off, len);
            if (len != -1) {
                length += len;
            }
            return len;
        }
    }

    /**
     * Returns the first 64 k of the underlying input stream. This is used for
     * content type detection during file import into MyCoRe.
     * 
     * @return the first 64 k of the input stream
     */
    public byte[] getHeader() {
        return header;
    }

    /**
     * Returns the number of bytes read so far
     * 
     * @return the number of bytes read
     */
    public long getLength() {
        return length;
    }

    /**
     * Returns the MD5 message digest that has been built during reading of the
     * underlying input stream.
     * 
     * @return the MD5 message digest checksum of all bytes that have been read
     */
    public byte[] getMD5() {
        if (md5 == null)
            md5 = digest.digest();
        return md5;
    }

    /**
     * Returns the MD5 checksum as a String
     * 
     * @return the MD5 checksum as a String of hex digits
     */
    public String getMD5String() {
        return getMD5String(getMD5());
    }

    /**
     * Given an MD5 message digest, returns the MD5 checksum as a String
     * 
     * @return the MD5 checksum as a String of hex digits
     */
    public static String getMD5String(byte[] digest) {
        StringBuilder md5SumBuilder = new StringBuilder();
        for (byte b : digest) {
            md5SumBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        String md5Sum = md5SumBuilder.toString();
        return md5Sum;
    }

    /**
     * Builds a MessageDigest instance for MD5 checksum computation.
     * 
     * @throws MCRConfigurationException
     *             if no java classes that support MD5 algorithm could be found
     */
    private static MessageDigest buildMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exc) {
            String msg = "Could not find java classes that support MD5 checksum algorithm";
            throw new MCRConfigurationException(msg, exc);
        }
    }
}
