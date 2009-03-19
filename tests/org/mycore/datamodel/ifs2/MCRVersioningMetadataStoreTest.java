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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * JUnit test for MCRVersioningMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStoreTest extends MCRTestCase {

    private static Logger LOGGER = Logger.getLogger(MCRVersioningMetadataStoreTest.class);

    protected static MCRVersioningMetadataStore store;

    protected void createStore() throws Exception {
        File temp = File.createTempFile("base", "");
        String path = temp.getAbsolutePath();
        temp.delete();

        setProperty("MCR.IFS2.Store.TEST.Class", "org.mycore.datamodel.ifs2.MCRVersioningMetadataStore", true);
        setProperty("MCR.IFS2.Store.TEST.BaseDir", path, true);
        setProperty("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2", true);

        temp = File.createTempFile("base", "");
        path = "file:///" + temp.getAbsolutePath().replace('\\', '/');
        temp.delete();

        setProperty("MCR.IFS2.Store.TEST.SVNRepositoryURL", path, true);
        store = MCRVersioningMetadataStore.getStore("TEST");
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (store == null)
            createStore();
        else {
            VFS.getManager().resolveFile(store.getBaseDir()).createFolder();
            SVNRepositoryFactory.createLocalRepository(new File(store.getRepositoryURL().getPath()), true, false);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(store.getBaseDir()).delete(Selectors.SELECT_ALL);
        VFS.getManager().resolveFile(store.repURL.getPath()).delete(Selectors.SELECT_ALL);
    }

    public void testCreateDocument() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm = store.create(new MCRContent(xml1));
        assertNotNull(vm);
        assertTrue(vm.getID() > 0);
        assertTrue(vm.getRevision() > 0);
        MCRVersionedMetadata vm2 = store.retrieve(vm.getID());
        Document xml2 = vm2.getMetadata().getXML();
        assertEquals(xml1.toString(), xml2.toString());

        MCRVersionedMetadata vm3 = store.create(new MCRContent(xml1));
        assertTrue(vm3.getID() > vm.getID());
        assertTrue(vm3.getRevision() > vm.getRevision());
    }

    public void testCreateDocumentInt() throws Exception {
        int id = store.getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm1 = store.create(new MCRContent(xml1), id);
        assertNotNull(vm1);
        Document xml2 = store.retrieve(id).getMetadata().getXML();
        assertEquals(xml1.toString(), xml2.toString());
        store.create(new MCRContent(xml1), id + 1);
        Document xml3 = store.retrieve(id + 1).getMetadata().getXML();
        assertEquals(xml1.toString(), xml3.toString());
    }

    public void testDelete() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = store.create(new MCRContent(xml1)).getID();
        assertTrue(store.exists(id));
        store.delete(id);
        assertFalse(store.exists(id));
        assertNull(store.retrieve(id));
    }

    public void testUpdate() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm = store.create(new MCRContent(xml1));
        Document xml3 = new Document(new Element("update"));
        long rev = vm.getRevision();
        vm.update(new MCRContent(xml3));
        assertTrue(vm.getRevision() > rev);
        Document xml4 = store.retrieve(vm.getID()).getMetadata().getXML();
        assertEquals(xml3.toString(), xml4.toString());
    }

    public void testRetrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = store.create(new MCRContent(xml1)).getID();
        MCRVersionedMetadata sm1 = store.retrieve(id);
        Document xml2 = sm1.getMetadata().getXML();
        assertEquals(xml1.toString(), xml2.toString());
    }

    public void testVersioning() throws Exception {
        Document xml1 = new Document(new Element("bingo"));
        MCRVersionedMetadata vm = store.create(new MCRContent(xml1));
        long baseRev = vm.getRevision();
        assertTrue(vm.isUpToDate());

        List<MCRMetadataVersion> versions = vm.listVersions();
        assertNotNull(versions);
        assertEquals(versions.size(), 1);
        MCRMetadataVersion mv = versions.get(0);
        assertSame(mv.getMetadataObject(), vm);
        assertEquals(mv.getRevision(), baseRev);
        assertEquals(mv.getUser(), MCRSessionMgr.getCurrentSession().getCurrentUserID());
        assertEquals(mv.getType(), MCRMetadataVersion.CREATED);

        Document xml2 = new Document(new Element("bango"));
        vm.update(new MCRContent(xml2));
        assertTrue(vm.getRevision() > baseRev);
        assertTrue(vm.isUpToDate());

        versions = vm.listVersions();
        assertEquals(versions.size(), 2);
        mv = versions.get(0);
        assertEquals(mv.getRevision(), baseRev);
        mv = versions.get(1);
        assertEquals(mv.getRevision(), vm.getRevision());
        assertEquals(mv.getType(), MCRMetadataVersion.UPDATED);

        Document xml3 = new Document(new Element("bongo"));
        vm.update(new MCRContent(xml3));

        versions = vm.listVersions();
        assertEquals(versions.size(), 3);
        mv = versions.get(0);
        assertEquals(mv.getRevision(), baseRev);
        mv = versions.get(2);
        assertEquals(mv.getRevision(), vm.getRevision());
        assertTrue(versions.get(0).getRevision() < versions.get(1).getRevision());
        assertTrue(versions.get(1).getRevision() < versions.get(2).getRevision());
        assertTrue(versions.get(0).getDate().before(versions.get(1).getDate()));
        assertTrue(versions.get(1).getDate().before(versions.get(2).getDate()));

        xml1 = versions.get(0).retrieve().getXML();
        assertNotNull(xml1);
        assertEquals("bingo", xml1.getRootElement().getName());
        xml2 = versions.get(1).retrieve().getXML();
        assertNotNull(xml2);
        assertEquals("bango", xml2.getRootElement().getName());
        xml3 = versions.get(2).retrieve().getXML();
        assertNotNull(xml1);
        assertEquals("bongo", xml3.getRootElement().getName());

        versions.get(1).restore();
        assertTrue(vm.getRevision() > versions.get(2).getRevision());
        assertTrue(vm.getLastModified().after(versions.get(2).getDate()));
        assertEquals(vm.getMetadata().getXML().getRootElement().getName(), "bango");
        assertEquals(vm.listVersions().size(), 4);
    }

    public void testCreateUpdateDeleteCreate() throws Exception {
        Element root = new Element("bingo");
        Document xml1 = new Document(root);
        MCRVersionedMetadata vm = store.create(new MCRContent(xml1));
        root.setName("bango");
        vm.update(new MCRContent(xml1));
        vm.delete();
        root.setName("bongo");
        vm = store.create(new MCRContent(xml1), vm.getID());
        List<MCRMetadataVersion> versions = vm.listVersions();
        assertEquals(4, versions.size());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(0).getType());
        assertEquals(MCRMetadataVersion.UPDATED, versions.get(1).getType());
        assertEquals(MCRMetadataVersion.DELETED, versions.get(2).getType());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(3).getType());
        versions.get(1).restore();
        assertEquals("bango",vm.getMetadata().getXML().getRootElement().getName());
    }

    public void testPerformance() throws Exception {
        Document xml = new Document(new Element("root"));
        LOGGER.info("Storing 10 XML documents in store:");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++)
            store.create(new MCRContent(xml));
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        xml = new Document(new Element("update"));
        LOGGER.info("Updating 10 XML documents in store:");
        for (Iterator<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasNext();)
            store.retrieve(ids.next()).update(new MCRContent(xml));
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Deleting 10 XML documents from store:");
        for (Iterator<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasNext();) {
            ids.next();
            ids.remove();
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");
    }
}
