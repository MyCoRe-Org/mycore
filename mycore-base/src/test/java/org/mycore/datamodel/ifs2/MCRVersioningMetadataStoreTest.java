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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;

/**
 * JUnit test for MCRVersioningMetadataStore
 *
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStoreTest extends MCRIFS2VersioningTestCase {

    @Test
    public void createDocument() throws Exception {
        Document testXmlDoc = new Document(new Element("root"));
        MCRContent testContent = new MCRJDOMContent(testXmlDoc);

        MCRVersionedMetadata versionedMetadata = getVersStore().create(testContent);
        MCRContent contentFromStore = getVersStore().retrieve(versionedMetadata.getID()).getMetadata();
        String contentStrFromStore = contentFromStore.asString();

        MCRContent mcrContent = new MCRJDOMContent(testXmlDoc);
        String expectedContentStr = mcrContent.asString();

        assertNotNull(versionedMetadata);
        assertEquals(expectedContentStr, contentStrFromStore);

        assertTrue(versionedMetadata.getID() > 0);
        assertTrue(versionedMetadata.getRevision() > 0);

        MCRVersionedMetadata vm3 = getVersStore().create(new MCRJDOMContent(testXmlDoc));
        assertTrue(vm3.getID() > versionedMetadata.getID());
        assertTrue(vm3.getRevision() > versionedMetadata.getRevision());
    }

    @Test
    public void createDocumentInt() throws Exception {
        int id = getVersStore().getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm1 = getVersStore().create(new MCRJDOMContent(xml1), id);
        MCRContent xml2 = getVersStore().retrieve(id).getMetadata();

        assertNotNull(vm1);
        assertEquals(new MCRJDOMContent(xml1).asString(), xml2.asString());
        getVersStore().create(new MCRJDOMContent(xml1), id + 1);
        MCRContent xml3 = getVersStore().retrieve(id + 1).getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml3.asString());
    }

    @Test
    public void delete() throws Exception {
        System.out.println("TEST DELETE");
        Document xml1 = new Document(new Element("root"));
        int id = getVersStore().create(new MCRJDOMContent(xml1)).getID();
        assertTrue(getVersStore().exists(id));
        getVersStore().delete(id);
        assertFalse(getVersStore().exists(id));
    }

    @Test
    public void update() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm = getVersStore().create(new MCRJDOMContent(xml1));
        Document xml3 = new Document(new Element("update"));
        long rev = vm.getRevision();
        vm.update(new MCRJDOMContent(xml3));
        assertTrue(vm.getRevision() > rev);
        MCRContent xml4 = getVersStore().retrieve(vm.getID()).getMetadata();
        assertEquals(new MCRJDOMContent(xml3).asString(), xml4.asString());
    }

    @Test
    public void retrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getVersStore().create(new MCRJDOMContent(xml1)).getID();
        MCRVersionedMetadata sm1 = getVersStore().retrieve(id);
        MCRContent xml2 = sm1.getMetadata();
        assertEquals(new MCRJDOMContent(xml1).asString(), xml2.asString());
    }

    @Test
    public void versioning() throws Exception {
        Document xml1 = new Document(new Element("bingo"));
        MCRVersionedMetadata vm = getVersStore().create(new MCRJDOMContent(xml1));
        long baseRev = vm.getRevision();
        assertTrue(vm.isUpToDate());

        List<MCRMetadataVersion> versions = vm.listVersions();
        assertNotNull(versions);
        assertEquals(1, versions.size());
        MCRMetadataVersion mv = versions.get(0);
        assertSame(mv.getMetadataObject(), vm);
        assertEquals(baseRev, mv.getRevision());
        assertEquals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID(), mv.getUser());
        assertEquals(MCRMetadataVersion.CREATED, mv.getType());

        bzzz();
        Document xml2 = new Document(new Element("bango"));
        vm.update(new MCRJDOMContent(xml2));
        assertTrue(vm.getRevision() > baseRev);
        assertTrue(vm.isUpToDate());

        versions = vm.listVersions();
        assertEquals(2, versions.size());
        mv = versions.get(0);
        assertEquals(baseRev, mv.getRevision());
        mv = versions.get(1);
        assertEquals(vm.getRevision(), mv.getRevision());
        assertEquals(MCRMetadataVersion.UPDATED, mv.getType());

        bzzz();
        Document xml3 = new Document(new Element("bongo"));
        vm.update(new MCRJDOMContent(xml3));

        versions = vm.listVersions();
        assertEquals(3, versions.size());
        mv = versions.get(0);
        assertEquals(baseRev, mv.getRevision());
        mv = versions.get(2);
        assertEquals(vm.getRevision(), mv.getRevision());
        assertTrue(versions.get(0).getRevision() < versions.get(1).getRevision());
        assertTrue(versions.get(1).getRevision() < versions.get(2).getRevision());
        assertTrue(versions.get(0).getDate().before(versions.get(1).getDate()));
        assertTrue(versions.get(1).getDate().before(versions.get(2).getDate()));

        xml1 = versions.get(0).retrieve().asXML();
        assertNotNull(xml1);
        assertEquals("bingo", xml1.getRootElement().getName());
        xml2 = versions.get(1).retrieve().asXML();
        assertNotNull(xml2);
        assertEquals("bango", xml2.getRootElement().getName());
        xml3 = versions.get(2).retrieve().asXML();
        assertNotNull(xml1);
        assertEquals("bongo", xml3.getRootElement().getName());

        bzzz();
        versions.get(1).restore();
        assertTrue(vm.getRevision() > versions.get(2).getRevision());
        assertTrue(vm.getLastModified().after(versions.get(2).getDate()));
        assertEquals("bango", vm.getMetadata().asXML().getRootElement().getName());
        assertEquals(4, vm.listVersions().size());
    }

    @Test
    public void createUpdateDeleteCreate() throws Exception {
        Element root = new Element("bingo");
        Document xml1 = new Document(root);
        MCRVersionedMetadata vm = getVersStore().create(new MCRJDOMContent(xml1));
        root.setName("bango");
        vm.update(new MCRJDOMContent(xml1));
        vm.delete();
        root.setName("bongo");
        vm = getVersStore().create(new MCRJDOMContent(xml1), vm.getID());
        List<MCRMetadataVersion> versions = vm.listVersions();
        assertEquals(4, versions.size());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(0).getType());
        assertEquals(MCRMetadataVersion.UPDATED, versions.get(1).getType());
        assertEquals(MCRMetadataVersion.DELETED, versions.get(2).getType());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(3).getType());
        versions.get(1).restore();
        assertEquals("bango", vm.getMetadata().asXML().getRootElement().getName());
    }

    @Test
    public void deletedVersions() throws Exception {
        Element root = new Element("bingo");
        Document xml1 = new Document(root);
        MCRVersionedMetadata vm = getVersStore().create(new MCRJDOMContent(xml1));
        assertFalse(vm.isDeleted());

        vm.delete();
        assertTrue(vm.isDeleted());
        assertFalse(getVersStore().exists(vm.getID()));

        vm = getVersStore().retrieve(vm.getID());
        assertTrue(vm.isDeleted());
        List<MCRMetadataVersion> versions = vm.listVersions();
        MCRMetadataVersion v1 = versions.get(0);
        MCRMetadataVersion v2 = versions.get(1);

        boolean cannotRestoreDeleted = false;
        try {
            v2.restore();
        } catch (MCRUsageException ex) {
            cannotRestoreDeleted = true;
        }
        assertTrue(cannotRestoreDeleted);

        v1.restore();
        assertFalse(vm.isDeleted());
        assertEquals(root.getName(), vm.getMetadata().asXML().getRootElement().getName());
    }

    @Test
    public void verifyRevPropsFail() throws Exception {
        getVersStore().verify();
        deletedVersions();
        getVersStore().verify();
        File baseDIR = new File(URI.create(getVersStore().repURL.toString()));
        File revProp = new File(baseDIR.toURI().resolve("db/revprops/0/2"));
        assertTrue("is not a file " + revProp, revProp.isFile());
        revProp.setWritable(true);
        new PrintWriter(revProp).close();
        revProp.setWritable(false);
        try {
            getVersStore().verify();
        } catch (MCRPersistenceException e) {
            return;
        }
        fail("Verify finished without error");
    }

    @Test
    public void verifyRevFail() throws Exception {
        getVersStore().verify();
        deletedVersions();
        getVersStore().verify();
        File baseDIR = new File(URI.create(getVersStore().repURL.toString()));
        File revProp = new File(baseDIR.toURI().resolve("db/revs/0/2"));
        assertTrue("is not a file " + revProp, revProp.isFile());
        new PrintWriter(revProp).close();
        try {
            getVersStore().verify();
        } catch (MCRPersistenceException e) {
            return;
        }
        fail("Verify finished without error");
    }
}
