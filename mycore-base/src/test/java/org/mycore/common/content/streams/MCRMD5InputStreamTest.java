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
        for (byte aData : data)
            assertEquals(aData, (byte) (m.read()));
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
