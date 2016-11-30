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

package org.mycore.common.content.streams;

import java.io.IOException;
import java.io.InputStream;

import org.mycore.common.MCRException;

/**
 * Provides the header of the stream that is read. 
 * This may be useful for content type detection purposes.
 * Immediately after stream construction, getHeader() can be called.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRHeaderInputStream extends MCRBlockingInputStream {

    /** The number of bytes that will be read for content type detection */
    public final static int MAX_HEADER_SIZE = 65536;

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
        return header;
    }
}
