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
        for (byte aData : data)
            assertEquals(aData, (byte) (h.read()));
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
        for (byte aData : data)
            assertEquals(aData, (byte) (h.read()));
        assertEquals(-1, h.read());
        h.close();
    }
}
