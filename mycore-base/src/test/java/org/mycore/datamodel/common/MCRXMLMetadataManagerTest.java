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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRByteContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectIDTest;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 */
@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Manager.Class", classNameOf = MCRDefaultXMLMetadataManager.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.document", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.ObjectID.NumberPattern", string = "00000000")
})
public class MCRXMLMetadataManagerTest {

    private XMLInfo mycoreDocument;
    private XMLInfo mycoreDocumentNew;
    private XMLInfo mcrDocument;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder();

    @BeforeEach
    public void setUp() throws Exception {
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

    @AfterEach
    public void tearDown(MCRMetadataExtension.BaseDirs baseDirs) throws Exception {
        for (File projectDir : baseDirs.storeBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                Files.walkFileTree(typeDir.toPath(), new MCRRecursiveDeleter());
                typeDir.mkdir();
            }
        }
        for (File projectDir : baseDirs.storeBaseDir().toFile().listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                //does not work on Windows (AccessdeniedExceptions):
                //Files.walkFileTree(typeDir.toPath(), new MCRRecursiveDeleter());
                SVNFileUtil.deleteAll(typeDir, true);
                typeDir.mkdir();
                SVNRepositoryFactory.createLocalRepository(typeDir, true, false);
            }
        }
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
        assertTrue(getStore().exists(mycoreDocument.id), mycoreDocument.id + " should exist");
        try {
            getStore().delete(mcrDocument.id);
        } catch (MCRPersistenceException e) {
            //is expected as MCR_document_00000001 does not exist
        }
        assertTrue(getStore().exists(mycoreDocument.id),
            mycoreDocument.id + " should not have been deleted");
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
        assertEquals(mycoreDocument.id.toString(),
            SAX_BUILDER.build(new ByteArrayInputStream(mycoreDocument.blob)).getRootElement()
                .getAttributeValue("id"),
            "Stored document ID do not match:");
        getStore().create(mycoreDocument.id,
            new MCRByteContent(mycoreDocument.blob, mcrDocument.lastModified.getTime()),
            mycoreDocument.lastModified);
        assertTrue(getStore().exists(mycoreDocument.id));
        Document doc = getStore().retrieveXML(mycoreDocument.id);
        assertEquals(mycoreDocument.id.toString(), doc.getRootElement()
            .getAttributeValue("id"), "Stored document ID do not match:");
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
                fail("org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2.getHighestStoredID()" +
                    " does not respect ProjectID");
            }
        }
    }

    @Test
    public void exists() {
        assertFalse(getStore().exists(mycoreDocument.id),
            "Object " + mycoreDocument.id + " should not exist.");
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        assertTrue(getStore().exists(mycoreDocument.id), "Object " + mycoreDocument.id + " should exist.");
    }

    @Test
    public void retrieveAllIDs() {
        assertEquals(0, getStore().listIDs().size(), "Store should not contain any objects.");
        getStore().create(mycoreDocument.id, mycoreDocument.blob,
            mycoreDocument.lastModified);
        assertTrue(getStore().listIDs().contains(mycoreDocument.id.toString()),
            "Store does not contain object " + mycoreDocument.id);
    }

    @Test
    public void listIDs() {
        assertTrue(getStore().listIDsForBase("foo_bar").isEmpty());
        assertTrue(getStore().listIDsOfType("bar").isEmpty());
        assertTrue(getStore().listIDs().isEmpty());
    }

    private MCRXMLMetadataManager getStore() {
        return MCRXMLMetadataManager.getInstance();
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
