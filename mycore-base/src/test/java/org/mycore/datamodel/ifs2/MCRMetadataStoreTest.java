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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * JUnit test for MCRMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStoreTest extends MCRIFS2MetadataTestCase {

    private static Logger LOGGER = Logger.getLogger(MCRMetadataStoreTest.class);

//    private MCRMetadataStore store;
//
//    protected void createStore() throws Exception {
//        File temp = File.createTempFile("base", "");
//        String path = temp.getAbsolutePath();
//        temp.delete();
//    
//        setProperty("MCR.IFS2.Store.TEST.Class", "org.mycore.datamodel.ifs2.MCRMetadataStore", true);
//        setProperty("MCR.IFS2.Store.TEST.BaseDir", path, true);
//        setProperty("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2", true);
//        setMetaDataStore(MCRMetadataStore.getStore("TEST"));
//    }
//
//    @Override
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        if (getMetaDataStore() == null) {
//            createStore();
//        } else {
//            VFS.getManager().resolveFile(getMetaDataStore().getBaseDir()).createFolder();
//        }
//    }
//
//    @Override
//    @After
//    public void tearDown() throws Exception {
//        super.tearDown();
//        VFS.getManager().resolveFile(getMetaDataStore().getBaseDir()).delete(Selectors.SELECT_ALL);
//    }

    @Test
    public void createDocument() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = getMetaDataStore().create(MCRContent.readFrom(xml1));
        assertNotNull(sm);
        int id1 = sm.getID();
        assertTrue(id1 > 0);
        MCRStoredMetadata sm2 = getMetaDataStore().retrieve(id1);
        MCRContent xml2 = sm2.getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml2.asString());
        int id2 = getMetaDataStore().create(MCRContent.readFrom(xml1)).getID();
        assertTrue(id2 > id1);
    }

    @Test
    public void createDocumentInt() throws Exception {
        int id = getMetaDataStore().getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm1 = getMetaDataStore().create(MCRContent.readFrom(xml1), id);
        assertNotNull(sm1);
        MCRContent xml2 = getMetaDataStore().retrieve(id).getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml2.asString());
        getMetaDataStore().create(MCRContent.readFrom(xml1), id + 1);
        MCRContent xml3 = getMetaDataStore().retrieve(id + 1).getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml3.asString());
    }

    @Test
    public void delete() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getMetaDataStore().create(MCRContent.readFrom(xml1)).getID();
        assertTrue(getMetaDataStore().exists(id));
        getMetaDataStore().delete(id);
        assertFalse(getMetaDataStore().exists(id));
        assertNull(getMetaDataStore().retrieve(id));
    }

    @Test
    public void update() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = getMetaDataStore().create(MCRContent.readFrom(xml1));
        Document xml2 = new Document(new Element("update"));
        sm.update(MCRContent.readFrom(xml2));
        MCRContent xml3 = getMetaDataStore().retrieve(sm.getID()).getMetadata();
        assertEquals(MCRContent.readFrom(xml2).asString(), xml3.asString());
    }

    @Test
    public void retrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getMetaDataStore().create(MCRContent.readFrom(xml1)).getID();
        MCRStoredMetadata sm1 = getMetaDataStore().retrieve(id);
        MCRContent xml2 = sm1.getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml2.asString());
    }

    @Test
    public void lastModified() throws Exception {
        Document xml1 = new Document(new Element("root"));
        Date date1 = new Date();
        synchronized (this) {
            wait(1000);
        }
        MCRStoredMetadata sm = getMetaDataStore().create(MCRContent.readFrom(xml1));
        Date date2 = sm.getLastModified();
        assertTrue(date2.after(date1));
        synchronized (this) {
            wait(1000);
        }
        Document xml2 = new Document(new Element("root"));
        sm.update(MCRContent.readFrom(xml2));
        assertTrue(sm.getLastModified().after(date2));
        @SuppressWarnings("deprecation")
        Date date = new Date(109, 1, 1);
        sm.setLastModified(date);
        sm = getMetaDataStore().retrieve(sm.getID());
        assertEquals(date, sm.getLastModified());
    }

    @Test
    public void exists() throws Exception {
        int id = getMetaDataStore().getNextFreeID();
        assertFalse(getMetaDataStore().exists(id));
        Document xml1 = new Document(new Element("root"));
        getMetaDataStore().create(MCRContent.readFrom(xml1), id);
        assertTrue(getMetaDataStore().exists(id));
        getMetaDataStore().delete(id);
        assertFalse(getMetaDataStore().exists(id));
    }

    @Test
    public void getNextFreeID() throws Exception {
        int id1 = getMetaDataStore().getNextFreeID();
        assertTrue(id1 >= 0);
        assertFalse(getMetaDataStore().exists(id1));
        Document xml1 = new Document(new Element("root"));
        int id2 = getMetaDataStore().create(MCRContent.readFrom(xml1)).getID();
        assertTrue(id2 > id1);
        assertTrue(getMetaDataStore().getNextFreeID() > id2);
    }

    @Test
    public void listIDs() throws Exception {
        Iterator<Integer> IDs = getMetaDataStore().listIDs(true);
        while (IDs.hasNext()) {
            getMetaDataStore().delete(IDs.next());
        }
        assertFalse(getMetaDataStore().exists(1));
        assertFalse(getMetaDataStore().listIDs(true).hasNext());
        assertFalse(getMetaDataStore().listIDs(false).hasNext());
        Document xml1 = new Document(new Element("root"));
        getMetaDataStore().create(MCRContent.readFrom(xml1));
        getMetaDataStore().create(MCRContent.readFrom(xml1));
        getMetaDataStore().create(MCRContent.readFrom(xml1));
        ArrayList<Integer> l1 = new ArrayList<Integer>();
        IDs = getMetaDataStore().listIDs(true);
        while (IDs.hasNext()) {
            int id = IDs.next();
            if (!l1.isEmpty()) {
                assertTrue(id > l1.get(l1.size() - 1));
            }
            l1.add(id);
        }
        assertTrue(l1.size() == 3);
        ArrayList<Integer> l2 = new ArrayList<Integer>();
        IDs = getMetaDataStore().listIDs(false);
        while (IDs.hasNext()) {
            int id = IDs.next();
            if (!l2.isEmpty()) {
                assertTrue(id < l2.get(l2.size() - 1));
            }
            l2.add(id);
        }
        assertTrue(l2.size() == 3);
        Collections.sort(l2);
        assertEquals(l1, l2);
    }

    @Test
    public void performance() throws Exception {
        Document xml = new Document(new Element("root"));
        LOGGER.info("Storing 1.000 XML documents in store:");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            getMetaDataStore().create(MCRContent.readFrom(xml));
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        LOGGER.info("getLastModified of 1.000 XML documents from store:");
        time = System.currentTimeMillis();
        for (Iterator<Integer> ids = getMetaDataStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            getMetaDataStore().retrieve(ids.next()).getLastModified();
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Retrieving 1.000 XML documents from store:");
        for (Iterator<Integer> ids = getMetaDataStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            xml = getMetaDataStore().retrieve(ids.next()).getMetadata().asXML();
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        xml = new Document(new Element("update"));
        LOGGER.info("Updating 1.000 XML documents in store:");
        for (Iterator<Integer> ids = getMetaDataStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            getMetaDataStore().retrieve(ids.next()).update(MCRContent.readFrom(xml));
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Listing 1.000 IDs in descending order:");
        Iterator<Integer> IDs = getMetaDataStore().listIDs(MCRStore.DESCENDING);
        while (IDs.hasNext()) {
            IDs.next();
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Deleting 1.000 XML documents from store:");
        for (Iterator<Integer> ids = getMetaDataStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            getMetaDataStore().delete(ids.next());
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");
    }
}
