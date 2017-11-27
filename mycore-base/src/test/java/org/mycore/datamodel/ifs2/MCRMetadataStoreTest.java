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

package org.mycore.datamodel.ifs2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;

/**
 * JUnit test for MCRMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStoreTest extends MCRIFS2MetadataTestCase {

    @Test
    public void createDocument() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = getMetaDataStore().create(new MCRJDOMContent(xml1));
        assertNotNull(sm);
        int id1 = sm.getID();
        assertTrue(id1 > 0);
        MCRStoredMetadata sm2 = getMetaDataStore().retrieve(id1);
        MCRContent xml2 = sm2.getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml2.asString());
        int id2 = getMetaDataStore().create(new MCRJDOMContent(xml1)).getID();
        assertTrue(id2 > id1);
    }

    @Test
    public void createDocumentInt() throws Exception {
        Document xml1 = new Document(new Element("root"));
        try {
            getMetaDataStore().create(new MCRJDOMContent(xml1), 0);
            fail("metadata store allows to save with id \"0\".");
        } catch (Exception e) {
            //test passed
        }
        int id = getMetaDataStore().getNextFreeID();
        assertTrue(id > 0);
        MCRStoredMetadata sm1 = getMetaDataStore().create(new MCRJDOMContent(xml1), id);
        assertNotNull(sm1);
        MCRContent xml2 = getMetaDataStore().retrieve(id).getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml2.asString());
        getMetaDataStore().create(new MCRJDOMContent(xml1), id + 1);
        MCRContent xml3 = getMetaDataStore().retrieve(id + 1).getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml3.asString());
    }

    @Test
    public void delete() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getMetaDataStore().create(new MCRJDOMContent(xml1)).getID();
        assertTrue(getMetaDataStore().exists(id));
        getMetaDataStore().delete(id);
        assertFalse(getMetaDataStore().exists(id));
        assertNull(getMetaDataStore().retrieve(id));
    }

    @Test
    public void update() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRStoredMetadata sm = getMetaDataStore().create(new MCRJDOMContent(xml1));
        Document xml2 = new Document(new Element("update"));
        sm.update(new MCRJDOMContent(xml2));
        MCRContent xml3 = getMetaDataStore().retrieve(sm.getID()).getMetadata();
        assertEquals(new MCRJDOMContent(xml2).asString(), xml3.asString());
    }

    @Test
    public void retrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getMetaDataStore().create(new MCRJDOMContent(xml1)).getID();
        MCRStoredMetadata sm1 = getMetaDataStore().retrieve(id);
        MCRContent xml2 = sm1.getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml2.asString());
    }

    @Test
    public void lastModified() throws Exception {
        Document xml1 = new Document(new Element("root"));
        Date date1 = new Date();
        synchronized (this) {
            wait(1000);
        }
        MCRStoredMetadata sm = getMetaDataStore().create(new MCRJDOMContent(xml1));
        Date date2 = sm.getLastModified();
        assertTrue(date2.after(date1));
        synchronized (this) {
            wait(1000);
        }
        Document xml2 = new Document(new Element("root"));
        sm.update(new MCRJDOMContent(xml2));
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
        getMetaDataStore().create(new MCRJDOMContent(xml1), id);
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
        int id2 = getMetaDataStore().create(new MCRJDOMContent(xml1)).getID();
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
        getMetaDataStore().create(new MCRJDOMContent(xml1));
        getMetaDataStore().create(new MCRJDOMContent(xml1));
        getMetaDataStore().create(new MCRJDOMContent(xml1));
        ArrayList<Integer> l1 = new ArrayList<>();
        IDs = getMetaDataStore().listIDs(true);
        while (IDs.hasNext()) {
            int id = IDs.next();
            if (!l1.isEmpty()) {
                assertTrue(id > l1.get(l1.size() - 1));
            }
            l1.add(id);
        }
        assertTrue(l1.size() == 3);
        ArrayList<Integer> l2 = new ArrayList<>();
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
}
