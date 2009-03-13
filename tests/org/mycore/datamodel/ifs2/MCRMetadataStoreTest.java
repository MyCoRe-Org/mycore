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

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRTestCase;

/**
 * JUnit test for MCRMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStoreTest extends MCRTestCase {

    private static Logger LOGGER = Logger.getLogger(MCRMetadataStoreTest.class);

    private static MCRMetadataStore store;

    protected void createStore() throws Exception {
        File temp = File.createTempFile("base", "");
        String path = temp.getAbsolutePath();
        temp.delete();

        setProperty("MCR.IFS2.Store.TEST.Class", "org.mycore.datamodel.ifs2.MCRMetadataStore", true);
        setProperty("MCR.IFS2.Store.TEST.BaseDir", path, true);
        setProperty("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2", true);
        store = MCRMetadataStore.getStore("TEST");
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (store == null)
            createStore();
        else VFS.getManager().resolveFile(store.getBaseDir()).createFolder();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(store.getBaseDir()).delete(Selectors.SELECT_ALL);
    }

    public void testCreateDocument() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = store.create(xml1);
        assertNotNull(sm);
        int id1 = sm.getID();
        assertTrue(id1 > 0);
        MCRStoredMetadata sm2 = store.retrieve(id1);
        Document xml2 = sm2.getXML();
        assertEquals(xml1.toString(), xml2.toString());
        int id2 = store.create(xml1).getID();
        assertTrue(id2 > id1);
    }

    public void testCreateDocumentInt() throws Exception {
        int id = store.getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm1 = store.create(xml1, id);
        assertNotNull(sm1);
        Document xml2 = store.retrieve(id).getXML();
        assertEquals(xml1.toString(), xml2.toString());
        store.create(xml1, id + 1);
        Document xml3 = store.retrieve(id + 1).getXML();
        assertEquals(xml1.toString(), xml3.toString());
    }

    public void testDelete() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1).getID();
        assertTrue(store.exists(id));
        store.delete(id);
        assertFalse(store.exists(id));
        assertNull(store.retrieve(id));
    }

    public void testUpdate() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = store.create(xml1);
        Document xml3 = new Document(new Element("update"));
        sm.update(xml3);
        Document xml4 = store.retrieve(sm.getID()).getXML();
        assertEquals(xml3.toString(), xml4.toString());
    }

    public void testRetrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1).getID();
        MCRStoredMetadata sm1 = store.retrieve(id);
        Document xml2 = sm1.getXML();
        assertEquals(xml1.toString(), xml2.toString());
    }

    public void testLastModified() throws Exception {
        Document xml1 = new Document(new Element("root"));
        Date date1 = new Date();
        synchronized (this) {
            wait(1000);
        }
        MCRStoredMetadata sm = store.create(xml1);
        Date date2 = sm.getLastModified();
        assertTrue(date2.after(date1));
        synchronized (this) {
            wait(1000);
        }
        Document xml2 = new Document(new Element("root"));
        sm.update(xml2);
        assertTrue(sm.getLastModified().after(date2));
        Date date = new Date(2009, 1, 1);
        sm.setLastModified(date);
        sm = store.retrieve(sm.getID());
        assertEquals(sm.getLastModified(), date);
    }

    public void testExists() throws Exception {
        int id = store.getNextFreeID();
        assertFalse(store.exists(id));
        Document xml1 = new Document(new Element("root"));
        store.create(xml1, id);
        assertTrue(store.exists(id));
        store.delete(id);
        assertFalse(store.exists(id));
    }

    public void testGetNextFreeID() throws Exception {
        int id1 = store.getNextFreeID();
        assertTrue(id1 >= 0);
        assertFalse(store.exists(id1));
        Document xml1 = new Document(new Element("root"));
        int id2 = store.create(xml1).getID();
        assertTrue(id2 > id1);
        assertTrue(store.getNextFreeID() > id2);
    }

    public void testListIDs() throws Exception {
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

    public void testPerformance() throws Exception {
        Document xml = new Document(new Element("root"));
        LOGGER.info("Storing 1.000 XML documents in store:");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
            store.create(xml);
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        LOGGER.info("getLastModified of 1.000 XML documents from store:");
        time = System.currentTimeMillis();
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            store.retrieve(ids.nextElement()).getLastModified();
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Retrieving 1.000 XML documents from store:");
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            xml = store.retrieve(ids.nextElement()).getXML();
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        xml = new Document(new Element("update"));
        LOGGER.info("Updating 1.000 XML documents in store:");
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            store.retrieve(ids.nextElement()).update(xml);
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Listing 1.000 IDs in descending order:");
        Enumeration<Integer> IDs = store.listIDs(MCRStore.DESCENDING);
        while (IDs.hasMoreElements())
            IDs.nextElement();
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Deleting 1.000 XML documents from store:");
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            store.delete(ids.nextElement());
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");
    }
}
