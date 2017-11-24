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

package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 */
public class MCRXMLMetadataManagerTest extends MCRStoreTestCase {

    private XMLInfo MyCoRe_document_00000001, MyCoRe_document_00000001_new, MCR_document_00000001;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MyCoRe_document_00000001 = new XMLInfo("MyCoRe_document_00000001",
            "<object id=\"MyCoRe_document_00000001\"/>".getBytes("UTF-8"), new Date());
        MyCoRe_document_00000001_new = new XMLInfo("MyCoRe_document_00000001",
            "<object id=\"MyCoRe_document_00000001\" update=\"true\"/>".getBytes("UTF-8"), new Date());
        MCR_document_00000001 = new XMLInfo("MCR_document_00000001",
            "<object id=\"MCR_document_00000001\"/>".getBytes("UTF-8"), new Date());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        for (File projectDir : getStoreBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                VFS.getManager().resolveFile(typeDir.getAbsolutePath()).delete(Selectors.SELECT_ALL);
                typeDir.mkdir();
            }
        }
        for (File projectDir : getSvnBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                VFS.getManager().resolveFile(typeDir.getAbsolutePath()).delete(Selectors.SELECT_ALL);
                typeDir.mkdir();
                SVNRepositoryFactory.createLocalRepository(typeDir, true, false);
            }
        }
        super.tearDown();
    }

    static Document getDocument(InputStream in) throws JDOMException, IOException {
        try {
            return SAX_BUILDER.build(in);
        } finally {
            in.close();
        }
    }

    @Test
    public void create() throws IOException {
        getStore().create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob,
            MyCoRe_document_00000001.lastModified);
        getStore().create(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
    }

    @Test
    public void delete() throws IOException {
        MCRStoredMetadata metadata = getStore().create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob,
            MyCoRe_document_00000001.lastModified);
        assertFalse(MyCoRe_document_00000001.id + " should not have been deleted", metadata.isDeleted());
        assertTrue(MyCoRe_document_00000001.id + " should exist", getStore().exists(MyCoRe_document_00000001.id));
        try {
            getStore().delete(MCR_document_00000001.id);
        } catch (IOException e) {
            //is expected as MCR_document_00000001 does not exist
        }
        assertTrue(MyCoRe_document_00000001.id + " should not have been deleted",
            getStore().exists(MyCoRe_document_00000001.id));
    }

    @Test
    public void update() throws IOException {
        getStore().create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob,
            MyCoRe_document_00000001.lastModified);
        getStore().update(MyCoRe_document_00000001_new.id, MyCoRe_document_00000001_new.blob,
            MyCoRe_document_00000001_new.lastModified);
        try {
            getStore().update(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
            fail("Update for non existent " + MCR_document_00000001.id + " succeeded.");
        } catch (RuntimeException e) {
            //this exception is expected here
        }
    }

    @Test
    public void retrieve() throws JDOMException, IOException, SAXException {
        assertEquals("Stored document ID do not match:", MyCoRe_document_00000001.id.toString(),
            SAX_BUILDER.build(new ByteArrayInputStream(MyCoRe_document_00000001.blob)).getRootElement()
                .getAttributeValue("id"));
        getStore().create(MyCoRe_document_00000001.id,
            new MCRByteContent(MyCoRe_document_00000001.blob, MCR_document_00000001.lastModified.getTime()),
            MyCoRe_document_00000001.lastModified);
        MCRVersionedMetadata data = getStore().getVersionedMetaData(MyCoRe_document_00000001.id);
        assertFalse(data.isDeleted());
        assertFalse(data.isDeletedInRepository());
        Document doc = getStore().retrieveXML(MyCoRe_document_00000001.id);
        assertEquals("Stored document ID do not match:", MyCoRe_document_00000001.id.toString(), doc.getRootElement()
            .getAttributeValue("id"));
        try {
            doc = getStore().retrieveXML(MCR_document_00000001.id);
            if (doc != null) {
                fail("Requested " + MCR_document_00000001.id + ", retrieved wrong document:\n"
                    + new XMLOutputter(Format.getPrettyFormat()).outputString(doc));
            }
        } catch (Exception e) {
            //this is ok
        }
    }

    @Test
    public void getHighestStoredID() {
        Method[] methods = getStore().getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("getHighestStoredID") && method.getParameterTypes().length == 0) {
                fail(
                    "org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2.getHighestStoredID() does not respect ProjectID");
            }
        }
    }

    @Test
    public void exists() throws IOException {
        assertFalse("Object " + MyCoRe_document_00000001.id + " should not exist.",
            getStore().exists(MyCoRe_document_00000001.id));
        getStore().create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob,
            MyCoRe_document_00000001.lastModified);
        assertTrue("Object " + MyCoRe_document_00000001.id + " should exist.",
            getStore().exists(MyCoRe_document_00000001.id));
    }

    @Test
    public void retrieveAllIDs() throws IOException {
        assertEquals("Store should not contain any objects.", 0, getStore().listIDs().size());
        getStore().create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob,
            MyCoRe_document_00000001.lastModified);
        assertTrue("Store does not contain object " + MyCoRe_document_00000001.id,
            getStore().listIDs().contains(MyCoRe_document_00000001.id.toString()));
    }

    private static class XMLInfo {
        public XMLInfo(String id, byte[] blob, Date lastModified) {
            this.id = MCRObjectID.getInstance(id);
            this.blob = blob;
            this.lastModified = lastModified;
        }

        MCRObjectID id;

        byte[] blob;

        Date lastModified;
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.document", "true");
        testProperties.put("MCR.Metadata.ObjectID.NumberPattern", "00000000");
        return testProperties;
    }
}
