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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStringContent;

/**
 * JUnit test for MCRFileStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileStoreTest extends MCRIFS2TestCase {

    @Test
    public void create() throws Exception {
        MCRFileCollection col = getStore().create();
        assertNotNull(col);
        assertTrue(col.getID() > 0);
    }

    @Test
    public void createInt() throws Exception {
        int id1 = getStore().getNextFreeID();
        MCRFileCollection col1 = getStore().create(id1);
        assertNotNull(col1);
        assertEquals(id1, col1.getID());
        assertTrue(getStore().exists(id1));
        MCRFileCollection col2 = getStore().create(id1 + 1);
        assertNotNull(col2);
        assertEquals(id1 + 1, col2.getID());
        assertTrue(getStore().exists(id1 + 1));
    }

    @Test
    public void delete() throws Exception {
        MCRFileCollection col = getStore().create();
        assertTrue(getStore().exists(col.getID()));

        getStore().delete(col.getID());
        assertFalse(getStore().exists(col.getID()));

        MCRFileCollection col2 = getStore().retrieve(col.getID());
        assertNull(col2);

        MCRFileCollection col3 = getStore().create();
        col3.delete();
        assertFalse(getStore().exists(col3.getID()));
    }

    @Test
    public void retrieve() throws Exception {
        MCRFileCollection col1 = getStore().create();
        MCRFileCollection col2 = getStore().retrieve(col1.getID());
        assertNotNull(col2);
        assertEquals(col1.getID(), col2.getID());
        assertEquals(col1.getLastModified(), col2.getLastModified());
        MCRFileCollection col3 = getStore().retrieve(col1.getID() + 1);
        assertNull(col3);
    }

    @Test
    public void exists() throws Exception {
        int id = getStore().getNextFreeID();
        assertFalse(getStore().exists(id));
        getStore().create(id);
        assertTrue(getStore().exists(id));
    }

    @Test
    public void getNextFreeID() throws Exception {
        int id1 = getStore().getNextFreeID();
        assertTrue(id1 >= 0);
        assertFalse(getStore().exists(id1));
        int id2 = getStore().create().getID();
        assertTrue(id2 > id1);
        int id3 = getStore().getNextFreeID();
        assertTrue(id3 > id2);
    }

    @Test
    public void listIDs() throws Exception {
        Iterator<Integer> IDs = getStore().listIDs(true);
        while (IDs.hasNext()) {
            getStore().delete(IDs.next());
        }
        assertFalse(getStore().exists(1));
        assertFalse(getStore().listIDs(true).hasNext());
        assertFalse(getStore().listIDs(false).hasNext());
        int expectedNumOfFileCollections = 3;

        createFileCollection(expectedNumOfFileCollections);

        ArrayList<Integer> l1 = new ArrayList<>();
        IDs = getStore().listIDs(true);
        assertTrue("IDs iterator has no next element? ", IDs.hasNext());
        while (IDs.hasNext()) {
            int id = IDs.next();
            if (!l1.isEmpty()) {
                assertTrue(id > l1.get(l1.size() - 1));
            }
            l1.add(id);
        }
        assertEquals("ID list size", expectedNumOfFileCollections, l1.size());
        ArrayList<Integer> l2 = new ArrayList<>();
        IDs = getStore().listIDs(false);
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

    private void createFileCollection(int numOfCollections) throws Exception {
        for (int i = 0; i < numOfCollections; i++) {
            MCRFileCollection fileCollection = getStore().create();
            int collectionID = fileCollection.getID();
            assertTrue("File collection with ID " + collectionID + " does not exists.",
                getStore().exists(collectionID));
        }
    }

    @Test
    public void basicFunctionality() throws Exception {
        Date first = new Date();
        synchronized (this) {
            wait(1000);
        }
        MCRFileCollection col = getStore().create();
        assertNotNull(col);
        assertTrue(col.getID() > 0);
        Date created = col.getLastModified();
        assertFalse(first.after(created));
        bzzz();
        MCRFile build = col.createFile("build.xml");
        assertNotNull(build);
        Date modified = col.getLastModified();
        assertTrue(modified.after(created));
        assertEquals(1, col.getNumChildren());
        assertEquals(1, col.getChildren().size());
        assertEquals(0, build.getSize());
        assertTrue(created.before(build.getLastModified()));
        build.setContent(new MCRJDOMContent(new Element("project")));
        assertTrue(build.getSize() > 0);
        assertNotNull(build.getContent().asByteArray());
        bzzz();
        MCRDirectory dir = col.createDir("documentation");
        assertEquals(2, col.getNumChildren());
        assertTrue(modified.before(col.getLastModified()));
        byte[] content = "Hello World!".getBytes("UTF-8");
        dir.createFile("readme.txt").setContent(new MCRByteContent(content, System.currentTimeMillis()));
        MCRFile child = (MCRFile) dir.getChild("readme.txt");
        assertNotNull(child);
        assertEquals(content.length, child.getSize());
    }

    @Test
    public void labels() throws Exception {
        MCRFileCollection col = getStore().create();
        assertTrue(col.getLabels().isEmpty());
        assertNull(col.getCurrentLabel());
        col.setLabel("de", "deutsch");
        col.setLabel("en", "english");
        String curr = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = col.getLabel(curr);
        assertEquals(label, col.getCurrentLabel());
        assertEquals(2, col.getLabels().size());
        assertEquals("english", col.getLabel("en"));
        MCRFileCollection col2 = getStore().retrieve(col.getID());
        assertEquals(2, col2.getLabels().size());
        col.clearLabels();
        assertTrue(col.getLabels().isEmpty());
    }

    @Test
    public void repairMetadata() throws Exception {
        MCRFileCollection col = getStore().create();
        Document xml1 = col.getMetadata().clone();
        col.repairMetadata();
        Document xml2 = col.getMetadata().clone();
        assertTrue(equals(xml1, xml2));

        MCRDirectory dir = col.createDir("foo");
        xml1 = col.getMetadata().clone();
        assertFalse(equals(xml1, xml2));
        dir.delete();
        xml1 = col.getMetadata().clone();
        assertTrue(equals(xml1, xml2));

        MCRDirectory dir2 = col.createDir("dir");
        MCRFile file1 = col.createFile("test1.txt");
        file1.setContent(new MCRStringContent("Test 1"));
        MCRFile readme = dir2.createFile("readme.txt");
        readme.setContent(new MCRStringContent("Hallo Welt!"));
        MCRFile file3 = col.createFile("test2.txt");
        file3.setContent(new MCRStringContent("Test 2"));
        file3.setLabel("de", "Die Testdatei");
        xml2 = col.getMetadata().clone();

        col.repairMetadata();
        xml1 = col.getMetadata().clone();
        assertTrue(equals(xml1, xml2));

        file3.clearLabels();
        xml2 = col.getMetadata().clone();

        col.fo.getChild("mcrdata.xml").delete();
        col = getStore().retrieve(col.getID());
        xml1 = col.getMetadata().clone();
        assertTrue(equals(xml1, xml2));

        col.fo.getChild("test1.txt").delete();
        FileObject tmp = col.fo.resolveFile("test3.txt");
        tmp.createFile();
        new MCRStringContent("Hallo Welt!").sendTo(tmp);
        col.repairMetadata();
        String xml3 = new MCRJDOMContent(col.getMetadata()).asString();
        assertFalse(xml3.contains("name=\"test1.txt\""));
        assertTrue(xml3.contains("name=\"test3.txt\""));
    }

    private boolean equals(Document a, Document b) throws Exception {
        sortChildren(a.getRootElement());
        sortChildren(b.getRootElement());
        String sa = new MCRJDOMContent(a).asString();
        String sb = new MCRJDOMContent(b).asString();
        return sa.equals(sb);
    }

    private void sortChildren(Element parent) throws Exception {
        @SuppressWarnings("unchecked")
        List<Element> children = parent.getChildren();
        if (children == null || children.size() == 0) {
            return;
        }

        ArrayList<Element> copy = new ArrayList<>();
        copy.addAll(children);

        copy.sort((a, b) -> {
            String sa = a.getName() + "/" + a.getAttributeValue("name");
            String sb = b.getName() + "/" + b.getAttributeValue("name");
            return sa.compareTo(sb);
        });

        parent.removeContent();
        parent.addContent(copy);

        for (Element child : copy) {
            sortChildren(child);
        }
    }
}
