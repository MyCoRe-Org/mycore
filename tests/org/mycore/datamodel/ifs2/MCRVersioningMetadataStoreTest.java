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
import java.util.Enumeration;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
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
        path = "file:///" + temp.getAbsolutePath().replace( '\\', '/' );
        temp.delete();
        
        setProperty("MCR.IFS2.Store.TEST.SVNRepositoryURL", path, true);
        store = MCRVersioningMetadataStore.getStore("TEST");
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (store == null)
            createStore();
        else
        {
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
        MCRVersionedMetadata sm = store.create(xml1);
        assertNotNull(sm);
        int id1 = sm.getID();
        assertTrue(id1 > 0);
        MCRVersionedMetadata sm2 = store.retrieve(id1);
        Document xml2 = sm2.getXML();
        assertEquals(xml1.toString(), xml2.toString());
        int id2 = store.create(xml1).getID();
        assertTrue(id2 > id1);
    }

    public void testCreateDocumentInt() throws Exception {
        int id = store.getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata sm1 = store.create(xml1, id);
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
        MCRVersionedMetadata sm = store.create(xml1);
        Document xml3 = new Document(new Element("update"));
        sm.update(xml3);
        Document xml4 = store.retrieve(sm.getID()).getXML();
        assertEquals(xml3.toString(), xml4.toString());
    }

    public void testRetrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = store.create(xml1).getID();
        MCRVersionedMetadata sm1 = store.retrieve(id);
        Document xml2 = sm1.getXML();
        assertEquals(xml1.toString(), xml2.toString());
    }

    public void testPerformance() throws Exception {
        Document xml = new Document(new Element("root"));
        LOGGER.info("Storing 10 XML documents in store:");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++)
            store.create(xml);
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        xml = new Document(new Element("update"));
        LOGGER.info("Updating 10 XML documents in store:");
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            store.retrieve(ids.nextElement()).update(xml);
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Deleting 10 XML documents from store:");
        for (Enumeration<Integer> ids = store.listIDs(MCRMetadataStore.ASCENDING); ids.hasMoreElements();)
            store.delete(ids.nextElement());
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");
    }
}
