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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

/**
 * JUnit test for MCRHeaderInputStream
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRHeaderInputStreamTest {

    @Test
    public void test() throws Exception {
        test(12, 12);
        test(12, 5);
        test(1, 12);
        test(0, 12);
    }

    private void test(int streamLength, int headerSize) throws Exception {
        String encoding = "UTF-8";
        String source = "Hello World!".substring(0, streamLength);
        byte[] input = source.getBytes(encoding);
        InputStream in = new ByteArrayInputStream(input);
        MCRHeaderInputStream his = new MCRHeaderInputStream(in, headerSize);

        byte[] header = his.getHeader();
        assertEquals(source.substring(0, Math.min(streamLength, headerSize)), new String(header, encoding));

        byte[] content = new byte[input.length];
        int numRead = his.read(content);
        assertEquals(content.length, numRead);
        assertEquals(source, new String(content, encoding));
        assertEquals(-1, his.read());
    }
}
