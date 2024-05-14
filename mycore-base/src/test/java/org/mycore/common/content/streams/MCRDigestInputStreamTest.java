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

import org.junit.Test;
import org.mycore.common.MCRUtils;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.datamodel.ifs2.MCRFile;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Frank Lützenkirchen
 */
public class MCRDigestInputStreamTest {

    @Test
    public void emptyStream() throws Exception {
        byte[] data = new byte[0];
        MCRDigestInputStream m = new MCRDigestInputStream(new ByteArrayInputStream(data), MCRMD5Digest.ALGORITHM);
        assertEquals(-1, m.read());
        m.close();
        assertEquals(MCRFile.MD5_OF_EMPTY_FILE, m.getDigestAsHexString());
    }

    @Test
    public void smallStream() throws Exception {
        byte[] data = "data".getBytes();
        MCRDigestInputStream m = new MCRDigestInputStream(new ByteArrayInputStream(data), MCRMD5Digest.ALGORITHM);
        for (byte aData : data) {
            assertEquals(aData, (byte) (m.read()));
        }
        assertEquals(-1, m.read());
        m.close();
        assertNotEquals(MCRFile.MD5_OF_EMPTY_FILE, m.getDigestAsHexString());
        assertEquals(16, m.getDigest().length);
        assertEquals(32, m.getDigestAsHexString().length());
    }

    @Test
    public void testUtils() throws Exception {
        String md5 = MCRUtils.getMD5Sum(new ByteArrayInputStream(new byte[0]));
        assertEquals(MCRFile.MD5_OF_EMPTY_FILE, md5);

    }
}
