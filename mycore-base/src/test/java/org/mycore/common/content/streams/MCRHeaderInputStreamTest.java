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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRHeaderInputStreamTest {

    @Test
    public void emptyStream() throws Exception {
        MCRHeaderInputStream h = new MCRHeaderInputStream(new ByteArrayInputStream(new byte[0]));
        assertEquals(0, h.getHeader().length);
        assertEquals(-1, h.read());
        h.close();
    }

    @Test
    public void smallStream() throws Exception {
        byte[] data = "data".getBytes();
        MCRHeaderInputStream h = new MCRHeaderInputStream(new ByteArrayInputStream(data));
        assertArrayEquals(data, h.getHeader());
        for (int i = 0; i < data.length; i++)
            assertEquals(data[i], (byte) (h.read()));
        assertEquals(-1, h.read());
        h.close();
    }

    @Test
    public void largeStream() throws Exception {
        byte[] data = new byte[MCRHeaderInputStream.MAX_HEADER_SIZE + 1000];
        for (int i = 0; i < data.length; i++)
            data[i] = (byte) i;

        MCRHeaderInputStream h = new MCRHeaderInputStream(new ByteArrayInputStream(data));
        assertEquals(MCRHeaderInputStream.MAX_HEADER_SIZE, h.getHeader().length);
        for (int i = 0; i < MCRHeaderInputStream.MAX_HEADER_SIZE; i++)
            assertEquals(data[i], h.getHeader()[i]);
        for (int i = 0; i < data.length; i++)
            assertEquals(data[i], (byte) (h.read()));
        assertEquals(-1, h.read());
        h.close();
    }
}
