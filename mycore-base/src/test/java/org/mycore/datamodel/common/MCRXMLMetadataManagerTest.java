/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectIDTest;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 */
public class MCRXMLMetadataManagerTest extends MCRStoreTestCase {

    private XMLInfo mycoreDocument;
    private XMLInfo mycoreDocumentNew;
    private XMLInfo mcrDocument;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        String testId = "junit_document_00000001";
        if (MCRObjectID.getInstance(testId).toString().length() != testId.length()) {
            MCRObjectIDTest.resetObjectIDFormat();
        }
        mycoreDocument = new XMLInfo("MyCoRe_document_00000001",
            "<object id=\"MyCoRe_document_00000001\"/>".getBytes(StandardCharsets.UTF_8), new Date());
        mycoreDocumentNew = new XMLInfo("MyCoRe_document_00000001",
            "<object id=\"MyCoRe_document_00000001\" update=\"true\"/>".getBytes(StandardCharsets.UTF_8), new Date());
        mcrDocument = new XMLInfo("MCR_document_00000001",
            "<object id=\"MCR_document_00000001\"/>".getBytes(StandardCharsets.UTF_8), new Date());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        for (File projectDir : getStoreBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                Files.walkFileTree(typeDir.toPath(), new MCRRecursiveDeleter());
                typeDir.mkdir();
            }
        }
        for (File projectDir : getSvnBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                Files.walkFileTree(typeDir.toPath(), new MCRRecursiveDeleter());
                typeDir.mkdir();
                SVNRepositoryFactory.createLocalRepository(typeDir, true, false);
            }
        }
        super.tearDown();
    }

    static Document getDocument(InputStream in) throws JDOMException, IOException {
        try (in) {
            return SAX_BUILDER.build(in);
        }
    }

    @Test
    public void create() {
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        getStore().create(mcrDocument.id, mcrDocument.blob, mcrDocument.lastModified);
    }

    @Test
    public void delete() {
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        assertTrue(mycoreDocument.id + " should exist", getStore().exists(mycoreDocument.id));
        try {
            getStore().delete(mcrDocument.id);
        } catch (MCRPersistenceException e) {
            //is expected as MCR_document_00000001 does not exist
        }
        assertTrue(mycoreDocument.id + " should not have been deleted",
            getStore().exists(mycoreDocument.id));
    }

    @Test
    public void update() {
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        getStore().update(mycoreDocumentNew.id, mycoreDocumentNew.blob,
            mycoreDocumentNew.lastModified);
        try {
            getStore().update(mcrDocument.id, mcrDocument.blob, mcrDocument.lastModified);
            fail("Update for non existent " + mcrDocument.id + " succeeded.");
        } catch (RuntimeException e) {
            //this exception is expected here
        }
    }

    @Test
    public void retrieve() throws JDOMException, IOException, SAXException {
        assertEquals("Stored document ID do not match:", mycoreDocument.id.toString(),
            SAX_BUILDER.build(new ByteArrayInputStream(mycoreDocument.blob)).getRootElement()
                .getAttributeValue("id"));
        getStore().create(mycoreDocument.id,
            new MCRByteContent(mycoreDocument.blob, mcrDocument.lastModified.getTime()),
            mycoreDocument.lastModified);
        assertTrue(getStore().exists(mycoreDocument.id));
        Document doc = getStore().retrieveXML(mycoreDocument.id);
        assertEquals("Stored document ID do not match:", mycoreDocument.id.toString(), doc.getRootElement()
            .getAttributeValue("id"));
        try {
            doc = getStore().retrieveXML(mcrDocument.id);
            if (doc != null) {
                fail("Requested " + mcrDocument.id + ", retrieved wrong document:\n"
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
                    "org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2.getHighestStoredID()" +
                        " does not respect ProjectID");
            }
        }
    }

    @Test
    public void exists() {
        assertFalse("Object " + mycoreDocument.id + " should not exist.",
            getStore().exists(mycoreDocument.id));
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        assertTrue("Object " + mycoreDocument.id + " should exist.",
            getStore().exists(mycoreDocument.id));
    }

    @Test
    public void retrieveAllIDs() {
        assertEquals("Store should not contain any objects.", 0, getStore().listIDs().size());
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        assertTrue("Store does not contain object " + mycoreDocument.id,
            getStore().listIDs().contains(mycoreDocument.id.toString()));
    }

    @Test
    public void listIDs() {
        assertTrue(getStore().listIDsForBase("foo_bar").isEmpty());
        assertTrue(getStore().listIDsOfType("bar").isEmpty());
        assertTrue(getStore().listIDs().isEmpty());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Manager.Class", MCRDefaultXMLMetadataManagerAdapter.class.getCanonicalName());
        testProperties.put("MCR.Metadata.Type.document", "true");
        testProperties.put("MCR.Metadata.ObjectID.NumberPattern", "00000000");
        return testProperties;
    }

    private static class XMLInfo {
        MCRObjectID id;
        byte[] blob;
        Date lastModified;

        XMLInfo(String id, byte[] blob, Date lastModified) {
            this.id = MCRObjectID.getInstance(id);
            this.blob = blob;
            this.lastModified = lastModified;
        }

    }

}
