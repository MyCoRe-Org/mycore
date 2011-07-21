package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 */
public class MCRXMLMetadataManagerTest extends MCRTestCase {

    private static MCRXMLMetadataManager store;

    private static File baseDirectory;

    private static File svnDirectory;

    private XMLInfo MyCoRe_document_00000001, MyCoRe_document_00000001_new, MCR_document_00000001;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder(false);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        setProperty("MCR.Metadata.Type.document", "true", true);
        setProperty("MCR.Metadata.ObjectID.NumberPattern", "00000000", true);

        MyCoRe_document_00000001 = new XMLInfo("MyCoRe_document_00000001", "<object id=\"MyCoRe_document_00000001\"/>".getBytes("UTF-8"), new Date());
        MyCoRe_document_00000001_new = new XMLInfo("MyCoRe_document_00000001", "<object id=\"MyCoRe_document_00000001\" update=\"true\"/>".getBytes("UTF-8"),
                new Date());
        MCR_document_00000001 = new XMLInfo("MCR_document_00000001", "<object id=\"MCR_document_00000001\"/>".getBytes("UTF-8"), new Date());

        if (store == null) {
            baseDirectory = File.createTempFile("base", "");
            baseDirectory.delete();
            setProperty("MCR.Metadata.Store.BaseDir", baseDirectory.getAbsolutePath(), true);

            svnDirectory = File.createTempFile("svn", "");
            svnDirectory.delete();
            String uri = "file:///" + svnDirectory.getAbsolutePath().replace('\\', '/');
            setProperty("MCR.Metadata.Store.SVNBase", uri, true);

            store = MCRXMLMetadataManager.instance();
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        for (File projectDir : baseDirectory.listFiles()) {
            for (File typeDir : projectDir.listFiles()) {
                VFS.getManager().resolveFile(typeDir.getAbsolutePath()).delete(Selectors.SELECT_ALL);
                typeDir.mkdir();
            }
        }
        for (File projectDir : svnDirectory.listFiles()) {
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
            Document doc = SAX_BUILDER.build(in);
            return doc;
        } finally {
            in.close();
        }
    }

    @Test
    public void create() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.create(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
    }

    @Test
    public void delete() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.delete(MCR_document_00000001.id);
        assertTrue(MyCoRe_document_00000001.id + " should not have been deleted", store.exists(MyCoRe_document_00000001.id));
    }

    @Test
    public void update() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.update(MyCoRe_document_00000001_new.id, MyCoRe_document_00000001_new.blob, MyCoRe_document_00000001_new.lastModified);
        try {
            store.update(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
            fail("Update for non existent " + MCR_document_00000001.id + " succeeded.");
        } catch (RuntimeException e) {
            //this exception is expected here
        }
    }

    @Test
    public void retrieve() throws JDOMException, IOException {
        store.create(MyCoRe_document_00000001.id, MCRContent.readFrom(MyCoRe_document_00000001.blob), MyCoRe_document_00000001.lastModified);
        Document doc = store.retrieveXML(MyCoRe_document_00000001.id);
        assertEquals("Stored document ID do not match:", MyCoRe_document_00000001.id.toString(), doc.getRootElement().getAttributeValue("id"));
        try {
            doc = store.retrieveXML(MCR_document_00000001.id);
            if (doc != null) {
                fail("Requested " + MCR_document_00000001.id + ", retrieved wrong document:\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(doc));
            }
        } catch (Exception e) {
            //this is ok
        }
    }

    @Test
    public void getHighestStoredID() {
        Method[] methods = store.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("getHighestStoredID") && method.getParameterTypes().length == 0) {
                fail("org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2.getHighestStoredID() does not respect ProjectID");
            }
        }
    }

    @Test
    public void exists() {
        assertFalse("Object " + MyCoRe_document_00000001.id + " should not exist.", store.exists(MyCoRe_document_00000001.id));
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        assertTrue("Object " + MyCoRe_document_00000001.id + " should exist.", store.exists(MyCoRe_document_00000001.id));
    }

    @Test
    public void retrieveAllIDs() {
        assertEquals("Store should not contain any objects.", 0, store.listIDs().size());
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        assertTrue("Store does not contain object " + MyCoRe_document_00000001.id, store.listIDs().contains(MyCoRe_document_00000001.id.toString()));
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
}
