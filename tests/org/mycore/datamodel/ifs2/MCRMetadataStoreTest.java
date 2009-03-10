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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRTestCase;

/**
 * JUnit test for MCRMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStoreTest extends MCRTestCase {

    private static String path;

    protected void setUp() throws Exception {
        super.setUp();

        if (path != null)
            return;

        File temp = File.createTempFile("base", "");
        path = temp.getAbsolutePath();
        temp.delete();
        VFS.getManager().resolveFile(path).createFolder();

        setProperty("MCR.IFS2.MetadataStore.TEST.BaseDir", path, true);
        setProperty("MCR.IFS2.MetadataStore.TEST.SlotLayout", "4-2-2", true);
        setProperty("MCR.IFS2.MetadataStore.TEST.CacheSize", "1", true);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(path).delete();
    }

    public void testGetStore() throws Exception {
        MCRMetadataStore store1 = MCRMetadataStore.getStore("TEST");
        assertNotNull(store1);
        MCRMetadataStore store2 = MCRMetadataStore.getStore("FOO");
        assertNull(store2);
    }

    public void testCreateDocument() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Document xml1 = new Document(new Element("root"));
        int id1 = store.create(xml1);
        assertTrue(id1 > 0);
        Document xml2 = store.retrieve(id1, false);
        assertEquals(xml1.toString(), xml2.toString());
        int id2 = store.create(xml1);
        assertTrue(id2 > id1);
    }

    public void testCreateDocumentInt() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        int id = store.getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        store.create(xml1, id);
        Document xml2 = store.retrieve(id, false);
        assertEquals(xml1.toString(), xml2.toString());
        store.create(xml1, id + 1);
        Document xml3 = store.retrieve(id + 1, false);
        assertEquals(xml1.toString(), xml3.toString());
    }

    public void testDelete() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1);
        assertTrue(store.exists(id));
        store.delete(id);
        assertFalse(store.exists(id));
        Document xml2 = store.retrieve(id, false);
        assertNull(xml2);
    }

    public void testUpdate() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1);
        Document xml3 = new Document(new Element("root"));
        store.update(xml3, id);
        Document xml4 = store.retrieve(id, false);
        assertEquals(xml3.toString(), xml4.toString());
    }

    public void testRetrieve() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1);
        Document xml2 = store.retrieve(id, false);
        assertEquals(xml1.toString(), xml2.toString());
        Document xml3 = store.retrieve(id, false);
        assertSame(xml2, xml3);
        Document xml4 = store.retrieve(id, true);
        assertNotSame(xml3, xml4);
    }

    public void testGetLastModified() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Document xml1 = new Document(new Element("root"));
        Date date1 = new Date();
        int id = store.create(xml1);
        Date date2 = store.getLastModified(id);
        assertTrue(date2.after(date1) || date2.equals(date1));
        Document xml2 = new Document(new Element("root"));
        store.update(xml2, id);
        Date date3 = store.getLastModified(id);
        assertTrue(date3.after(date2) || date3.equals(date2));
    }

    public void testExists() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        int id = store.getNextFreeID();
        assertFalse(store.exists(id));
        Document xml1 = new Document(new Element("root"));
        store.create(xml1, id);
        assertTrue(store.exists(id));
        store.delete(id);
        assertFalse(store.exists(id));
    }

    public void testGetNextFreeID() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        int id1 = store.getNextFreeID();
        assertTrue(id1 >= 0);
        assertFalse(store.exists(id1));
        Document xml1 = new Document(new Element("root"));
        int id2 = store.create(xml1);
        assertTrue(id2 > id1);
        int id3 = store.getNextFreeID();
        assertTrue(id3 > id2);
    }

    public void testListIDs() throws Exception {
        MCRMetadataStore store = MCRMetadataStore.getStore("TEST");
        Enumeration<Integer> IDs = store.listIDs(true);
        while (IDs.hasMoreElements())
            store.delete(IDs.nextElement());
        assertFalse(store.exists(1));
        assertFalse(store.listIDs(true).hasMoreElements());
        assertFalse(store.listIDs(false).hasMoreElements());
        Document xml1 = new Document(new Element("root"));
        store.create(xml1);
        store.create(xml1);
        store.create(xml1);
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        IDs = store.listIDs(true);
        while (IDs.hasMoreElements()) {
            int id = IDs.nextElement();
            if (!l1.isEmpty())
                assertTrue(id > l1.get(l1.size() - 1));
            l1.add(id);
        }
        assertTrue(l1.size() == 3);
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        IDs = store.listIDs(false);
        while (IDs.hasMoreElements()) {
            int id = IDs.nextElement();
            if (!l2.isEmpty())
                assertTrue(id < l2.get(l2.size() - 1));
            l2.add(id);
        }
        assertTrue(l2.size() == 3);
        Collections.sort(l2);
        assertEquals(l1, l2);
    }
}
