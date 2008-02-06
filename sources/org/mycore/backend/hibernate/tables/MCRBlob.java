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

package org.mycore.backend.hibernate.tables;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;

public class MCRBlob implements java.sql.Blob {
    byte[] data;

    MCRBlob(byte[] data) {
        this.data = data;
    }

    public InputStream getBinaryStream() {
        return new java.io.ByteArrayInputStream(this.data);
    }

    public OutputStream setBinaryStream(long l) {
        throw new IllegalArgumentException("not implemented");
    }

    public int setBytes(long l, byte[] a, int u, int v) {
        throw new IllegalArgumentException("not implemented");
    }

    public int setBytes(long l, byte[] a) {
        throw new IllegalArgumentException("not implemented");
    }

    public void truncate(long l) {
        throw new IllegalArgumentException("not implemented");
    }

    public byte[] getBytes(long pos, int length) throws SQLException {
        if (pos < 1)
            throw new SQLException("The ordinal position is less than 1: " + pos);
        if (pos > Integer.MAX_VALUE)
            throw new ArrayIndexOutOfBoundsException("Ordinal position grater than " + Integer.MAX_VALUE + " is not supported:" + pos);
        if (length < 0)
            throw new SQLException("The number of consecutive bytes to be copied must be 0 or greater: " + length);
        // first byte is position 1 according to java.sql API, but position 0 in
        // array
        int arrayPos = (int) (pos - 1);
        if ((arrayPos + length) > data.length) {
            length = (int) (data.length - arrayPos);
        }

        byte[] result = new byte[length];
        System.arraycopy(data, arrayPos, result, 0, length);

        return result;
    }

    public long length() {
        return this.data.length;
    }

    // Determines the byte position at which the specified byte pattern begins
    // within the BLOB value that this Blob object represents.
    public long position(byte[] pattern, long start) throws SQLException {
        if (start < 1)
            throw new SQLException("The start position is less than 1: " + start);
        if (start > Integer.MAX_VALUE)
            throw new ArrayIndexOutOfBoundsException("Start position grater than " + Integer.MAX_VALUE + " is not supported:" + start);
        int arrayPos;
        // first byte is position 1 according to java.sql API, but position 0 in
        // array
        searchloop: for (arrayPos = (int) start - 1; arrayPos < data.length; arrayPos++) {
            int s;
            int len = data.length - arrayPos;

            if (pattern.length > (data.length - arrayPos)) {
                break searchloop;
            }

            for (s = 0; s < len; s++) {
                if (pattern[s] != data[arrayPos]) {
                    continue searchloop;
                }
            }
            // again first byte in java.sql API is at position 1
            return arrayPos + 1;
        }

        return -1;
    }

    // Determines the byte position in the BLOB value designated by this Blob
    // object at which pattern begins.
    public long position(Blob pattern, long start) throws SQLException {
        byte[] b = pattern.getBytes(0, (int) pattern.length());

        return position(b, start);
    }

    public static byte[] getBytes(Blob blob) {
        try {
            if (blob.length() > Integer.MAX_VALUE) {
                throw new MCRException("Blobs longer than " + Integer.MAX_VALUE + " are not supported: " + blob.length());
            }
            return blob.getBytes(1, (int) blob.length());
        } catch (java.sql.SQLException e) {
            // TODO: check if this should be thrown
            Logger.getLogger(MCRBlob.class).error("Caught SQLException." + e);
            return null;
        }
    }

    public void free() throws SQLException {
        data = null;
    }

    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        if (pos < 1)
            throw new SQLException("The ordinal position is less than 1: " + pos);
        if (pos > data.length)
            throw new ArrayIndexOutOfBoundsException("Ordinal position is greater than the number of bytes (" + data.length + ") in the Blob: " + length);
        if (length < 0)
            throw new SQLException("The number of consecutive bytes to be copied must be 0 or greater: " + length);
        if ((pos + length) > data.length)
            throw new SQLException("The ordinal position(" + pos + ") + the length(" + length + ") in bytes are greater than the number of bytes ("
                    + data.length + ") in the Blob.");
        // first byte is position 1 according to java.sql API, but position 0 in
        // array
        return new ByteArrayInputStream(data, (int) pos - 1, (int) length);
    }
}
