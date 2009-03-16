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
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRTestCase;

/**
 * JUnit test for MCRFileStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileStoreTest extends MCRTestCase {

    private static MCRFileStore store;

    protected void createStore() throws Exception {
        File temp = File.createTempFile("base", "");
        String path = temp.getAbsolutePath();
        temp.delete();

        setProperty("MCR.IFS2.Store.TEST.Class", "org.mycore.datamodel.ifs2.MCRFileStore", true);
        setProperty("MCR.IFS2.Store.TEST.BaseDir", path, true);
        setProperty("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2", true);
        store = MCRFileStore.getStore("TEST");
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (store == null)
            createStore();
        else
            VFS.getManager().resolveFile(store.getBaseDir()).createFolder();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(store.getBaseDir()).delete(Selectors.SELECT_ALL);
    }

    public void testCreate() throws Exception {
        MCRFileCollection col = store.create();
        assertNotNull(col);
        assertTrue(col.getID() > 0);
    }

    public void testCreateInt() throws Exception {
        int id1 = store.getNextFreeID();
        MCRFileCollection col1 = store.create(id1);
        assertNotNull(col1);
        assertEquals(id1, col1.getID());
        assertTrue(store.exists(id1));
        MCRFileCollection col2 = store.create(id1 + 1);
        assertNotNull(col2);
        assertEquals(col2.getID(), id1 + 1);
        assertTrue(store.exists(id1 + 1));
    }

    public void testDelete() throws Exception {
        MCRFileCollection col = store.create();
        assertTrue(store.exists(col.getID()));
        store.delete(col.getID());
        assertFalse(store.exists(col.getID()));
        MCRFileCollection col2 = store.retrieve(col.getID());
        assertNull(col2);
    }

    public void testRetrieve() throws Exception {
        MCRFileCollection col1 = store.create();
        MCRFileCollection col2 = store.retrieve(col1.getID());
        assertNotNull(col2);
        assertEquals(col1.getID(), col2.getID());
        assertEquals(col1.getLastModified(), col2.getLastModified());
        MCRFileCollection col3 = store.retrieve(col1.getID() + 1);
        assertNull(col3);
    }

    public void testExists() throws Exception {
        int id = store.getNextFreeID();
        assertFalse(store.exists(id));
        store.create(id);
        assertTrue(store.exists(id));
    }

    public void testGetNextFreeID() throws Exception {
        int id1 = store.getNextFreeID();
        assertTrue(id1 >= 0);
        assertFalse(store.exists(id1));
        int id2 = store.create().getID();
        assertTrue(id2 > id1);
        int id3 = store.getNextFreeID();
        assertTrue(id3 > id2);
    }

    public void testListIDs() throws Exception {
        Enumeration<Integer> IDs = store.listIDs(true);
        while (IDs.hasMoreElements())
            store.delete(IDs.nextElement());
        assertFalse(store.exists(1));
        assertFalse(store.listIDs(true).hasMoreElements());
        assertFalse(store.listIDs(false).hasMoreElements());
        store.create();
        store.create();
        store.create();
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

    public void testBasicFunctionality() throws Exception {
        Date first = new Date();
        synchronized (this) {
            wait(1000);
        }
        MCRFileCollection col = store.create();
        assertNotNull(col);
        assertTrue(col.getID() > 0);
        Date created = col.getLastModified();
        assertTrue(created.after(first));
        synchronized (this) {
            wait(1000);
        }
        MCRFile build = new MCRFile(col, "build.xml");
        assertNotNull(build);
        Date modified = col.getLastModified();
        assertTrue(modified.after(created));
        assertEquals(col.getNumChildren(), 1);
        assertEquals(col.getChildren().size(), 1);
        assertEquals(build.getSize(), 0);
        assertTrue(created.before(build.getLastModified()));
        build.setContent(new MCRContent(new Document(new Element("project"))));
        assertTrue(build.getSize() > 0);
        assertNotNull(build.getContent().getBytes());
        synchronized (this) {
            wait(1000);
        }
        MCRDirectory dir = new MCRDirectory(col, "documentation");
        assertEquals(col.getNumChildren(), 2);
        assertTrue(modified.before(col.getLastModified()));
        byte[] content = "Hello World!".getBytes("UTF-8");
        new MCRFile(dir, "readme.txt").setContent(new MCRContent(content));
        MCRFile child = (MCRFile) (dir.getChild("readme.txt"));
        assertNotNull(child);
        assertEquals(child.getSize(), content.length);
    }
}
