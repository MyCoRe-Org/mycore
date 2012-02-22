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

import java.io.IOException;
import java.io.InputStream;

import org.mycore.common.MCRException;

/**
 * Reads the header of the stream that is read (the first 64k) for content type
 * detection purposes, and counts the number of bytes read so far. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRHeaderInputStream extends MCRBlockingInputStream {

    /** The total number of bytes read so far */
    protected long length;

    /** The header of the file read */
    protected byte[] header;

    public MCRHeaderInputStream(InputStream in, int headerSize) throws MCRException {
        super(in, headerSize);

        byte[] buffer = new byte[headerSize];

        try {
            super.mark(headerSize);
            int num = super.read(buffer, 0, buffer.length);
            header = new byte[Math.max(0, num)];

            if (num > 0) {
                System.arraycopy(buffer, 0, header, 0, num);
            }
            super.reset();
        } catch (IOException ex) {
            String msg = "Error while reading input stream header";
            throw new MCRException(msg, ex);
        }
    }

    /**
     * Returns the first 64 k of the underlying input stream. 
     * This can be used for content type detection.
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
}
