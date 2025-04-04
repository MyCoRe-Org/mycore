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

import java.io.IOException;
import java.io.InputStream;

import org.mycore.common.MCRException;

/**
 * Provides the header of the stream that is read. 
 * This may be useful for content type detection purposes.
 * Immediately after stream construction, getHeader() can be called.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRHeaderInputStream extends MCRBlockingInputStream {

    /** The number of bytes that will be read for content type detection */
    public static final int MAX_HEADER_SIZE = 65_536;

    /** The header of the stream read */
    protected byte[] header;

    public MCRHeaderInputStream(InputStream in) throws IOException, MCRException {
        super(in, MAX_HEADER_SIZE);
        super.mark(MAX_HEADER_SIZE);

        byte[] buffer = new byte[MAX_HEADER_SIZE];

        try {
            int num = read(buffer, 0, buffer.length);
            header = new byte[Math.max(0, num)];

            if (num > 0) {
                System.arraycopy(buffer, 0, header, 0, num);
            }
        } catch (IOException ex) {
            String msg = "Error while reading content input stream header";
            throw new MCRException(msg, ex);
        }
        super.reset();
    }

    /**
     * Returns the header of the underlying input stream, at maximum
     * MAX_HEADER_SIZE bytes.
     */
    public byte[] getHeader() {
        return header.clone();
    }
}
