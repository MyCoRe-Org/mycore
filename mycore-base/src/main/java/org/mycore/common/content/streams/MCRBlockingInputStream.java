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

package org.mycore.common.content.streams;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements a special kind of BufferedInputStream that blocks
 * invocations of the read method until the number of bytes that were requested
 * are fully read from the underlying InputStream. In contrast,
 * BufferedInputStream does NOT block when the requested number of bytes is not
 * available yet.
 * 
 * @see BufferedInputStream
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRBlockingInputStream extends BufferedInputStream {
    /**
     * Constructs a new MCRBlockingInputStream, using the default buffer size of
     * BufferedInputStreams.
     * 
     * @param in
     *            the InputStream to read data from
     */
    public MCRBlockingInputStream(InputStream in) {
        super(in);
    }

    /**
     * Constructs a new MCRBlockingInputStream that uses the given buffer size.
     * 
     * @param in
     *            the InputStream to read data from
     * @param size
     *            the size of the read buffer to be used
     */
    public MCRBlockingInputStream(InputStream in, int size) {
        super(in, size);
    }

    /**
     * Reads 'len' bytes from the underlying input stream and writes the bytes
     * that have been read to the buffer 'buf', starting at offset 'off' in the
     * buffer byte array. In contrast to BufferedInputStream.read(), this method
     * blocks the read request until 'len' bytes have been read completely or
     * the end of the input stream is reached.
     * 
     * @return the number of bytes read. If the end of the input stream is not
     *         reached yet, this is always the same as the number of bytes
     *         requested in the 'len' parameter.
     */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int total = 0;
        int num = 0;

        while (len > 0) {
            num = super.read(buf, off, len);

            if (num == 0) {
                continue;
            }

            if (num == -1) {
                break;
            }

            total += num;
            off += num;
            len -= num;
        }

        return total == 0 ? num : total;
    }
}
