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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.RandomAccessContent;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRJDOMContent;

/**
 * JUnit test for MCRFile
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileTest extends MCRIFS2TestCase {
    private MCRFileCollection col;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        col = getStore().create();
    }

    @Test
    public void fileName() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        Date created = file.getLastModified();
        assertEquals("foo.txt", file.getName());
        assertEquals("txt", file.getExtension());
        bzzz();
        file.renameTo("readme");
        assertEquals("readme", file.getName());
        assertEquals("", file.getExtension());
        assertTrue(file.getLastModified().after(created));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setLastModified() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        Date other = new Date(109, 1, 1);
        file.setLastModified(other);
        assertEquals(other, file.getLastModified());
    }

    @Test
    public void getMD5() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        assertEquals(MCRFile.MD5_OF_EMPTY_FILE, file.getMD5());
        byte[] content = "Hello World".getBytes("UTF-8");
        file.setContent(new MCRByteContent(content, System.currentTimeMillis()));
        assertFalse(MCRFile.MD5_OF_EMPTY_FILE.equals(file.getMD5()));
        MCRFileCollection col2 = getStore().retrieve(col.getID());
        MCRFile child = (MCRFile) col2.getChild("foo.txt");
        assertEquals(file.getMD5(), child.getMD5());
    }

    @Test
    public void delete() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        file.delete();
        assertNull(col.getChild("foo.txt"));
    }

    @Test
    public void children() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        assertNull(file.getChild("foo"));
        assertEquals(0, file.getChildren().size());
        assertFalse(file.hasChildren());
        assertEquals(0, file.getNumChildren());
    }

    @Test
    public void contentFile() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        File src = File.createTempFile("foo", "txt");
        src.deleteOnExit();
        byte[] content = "Hello World".getBytes("UTF-8");
        try (FileOutputStream fo = new FileOutputStream(src)) {
            IOUtils.copy(new ByteArrayInputStream(content), fo);
        }
        file.setContent(new MCRFileContent(src));
        assertFalse(MCRFile.MD5_OF_EMPTY_FILE.equals(file.getMD5()));
        assertEquals(11, file.getSize());
        file.getContent().sendTo(src);
        assertEquals(11, src.length());
    }

    @Test
    public void contentXML() throws Exception {
        MCRFile file = col.createFile("foo.xml");
        Document xml = new Document(new Element("root"));
        file.setContent(new MCRJDOMContent(xml));
        assertFalse(MCRFile.MD5_OF_EMPTY_FILE.equals(file.getMD5()));
        Document xml2 = file.getContent().asXML();
        assertEquals("root", xml2.getRootElement().getName());
    }

    @Test
    public void randomAccessContent() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        byte[] content = "Hello World".getBytes("UTF-8");
        file.setContent(new MCRByteContent(content, System.currentTimeMillis()));
        RandomAccessContent rac = file.getRandomAccessContent();
        rac.skipBytes(6);
        InputStream in = rac.getInputStream();
        char c = (char) in.read();
        assertEquals('W', c);
        in.close();
        rac.close();
    }

    @Test
    public void type() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
    }

    @Test
    public void labels() throws Exception {
        MCRFile file = col.createFile("foo.txt");
        assertTrue(file.getLabels().isEmpty());
        assertNull(file.getCurrentLabel());
        file.setLabel("de", "deutsch");
        file.setLabel("en", "english");
        String curr = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = file.getLabel(curr);
        assertEquals(label, file.getCurrentLabel());
        assertEquals(2, file.getLabels().size());
        assertEquals("english", file.getLabel("en"));
        MCRFileCollection col2 = getStore().retrieve(col.getID());
        MCRFile child = (MCRFile) col2.getChild("foo.txt");
        assertEquals(2, child.getLabels().size());
        file.clearLabels();
        assertTrue(file.getLabels().isEmpty());
    }
}
