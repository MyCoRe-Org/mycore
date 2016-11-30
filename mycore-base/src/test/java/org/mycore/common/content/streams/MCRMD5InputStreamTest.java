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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs2.MCRFile;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMD5InputStreamTest {

    @Test
    public void emptyStream() throws Exception {
        MCRMD5InputStream m = new MCRMD5InputStream(new ByteArrayInputStream(new byte[0]));
        assertEquals(-1, m.read());
        m.close();
        assertEquals(MCRFile.MD5_OF_EMPTY_FILE, m.getMD5String());
    }

    @Test
    public void smallStream() throws Exception {
        byte[] data = "data".getBytes();
        MCRMD5InputStream m = new MCRMD5InputStream(new ByteArrayInputStream(data));
        for (int i = 0; i < data.length; i++)
            assertEquals(data[i], (byte) (m.read()));
        assertEquals(-1, m.read());
        m.close();
        assertFalse(MCRFile.MD5_OF_EMPTY_FILE.equals(m.getMD5String()));
        assertEquals(16, m.getMD5().length);
        assertEquals(32, m.getMD5String().length());
    }

    @Test
    public void testUtils() throws Exception {
        String md5 = MCRUtils.getMD5Sum(new ByteArrayInputStream(new byte[0]));
        assertEquals(MCRFile.MD5_OF_EMPTY_FILE, md5);

    }
}
