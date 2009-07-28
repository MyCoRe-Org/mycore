/**
 * 
 */
package org.mycore.datamodel.ifs2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRObjectMetadataStoreIFS2Test extends MCRTestCase {

    private MCRObjectMetadataStoreIFS2 store;

    private File baseDirectory;

    private static Logger LOGGER = Logger.getLogger(MCRObjectMetadataStoreIFS2Test.class);

    private class XMLInfo {
        public XMLInfo(String id, byte[] blob, Date lastModified) {
            this.id = id;
            this.blob = blob;
            this.lastModified = lastModified;
        }

        String id;

        byte[] blob;

        Date lastModified;
    }

    private XMLInfo MyCoRe_document_00000001, MyCoRe_document_00000001_new, MCR_document_00000001;

    private static final SAXBuilder SAX_BUILDER = new SAXBuilder(false);

    protected void createStore() throws Exception {
        baseDirectory = File.createTempFile("base", "");
        baseDirectory.delete();
        setProperty("MCR.Metadata.Type.document", "true", true);
        setProperty("MCR.IFS2.Store.document.Class", "org.mycore.datamodel.ifs2.MCRMetadataStore", true);
        setProperty("MCR.IFS2.Store.document.BaseDir", baseDirectory.getAbsolutePath(), true);
        setProperty("MCR.IFS2.Store.document.SlotLayout", "4-2-2", true);
        store = new MCRObjectMetadataStoreIFS2();
        store.init("document");
    }

    protected void setUp() throws Exception {
        super.setUp();
        createStore();
        MyCoRe_document_00000001 = new XMLInfo("MyCoRe_document_00000001", "<object id=\"MyCoRe_document_00000001\"/>".getBytes(Charset
                .forName("UTF-8")), new Date());
        MyCoRe_document_00000001_new = new XMLInfo("MyCoRe_document_00000001", "<object id=\"MyCoRe_document_00000001\" update=\"true\"/>"
                .getBytes(Charset.forName("UTF-8")), new Date());
        MCR_document_00000001 = new XMLInfo("MCR_document_00000001", "<object id=\"MCR_document_00000001\"/>".getBytes(Charset
                .forName("UTF-8")), new Date());
    }

    protected void tearDown() throws Exception {
        List<String> ids = store.retrieveAllIDs();
        for (String id : ids) {
            LOGGER.debug("delete object " + id);
            store.delete(id);
        }
        deleteDirectory(baseDirectory);
        super.tearDown();
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists() && path.isDirectory()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    LOGGER.info("deleting directory: " + files[i].getAbsolutePath());
                    deleteDirectory(files[i]);
                } else {
                    LOGGER.info("deleting file: " + files[i].getAbsolutePath());
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    static Document getDocument(InputStream in) throws JDOMException, IOException {
        try {
            Document doc = SAX_BUILDER.build(in);
            return doc;
        } finally {
            in.close();
        }
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#create(java.lang.String, byte[], java.util.Date)}.
     */
    public void testCreate() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.create(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#delete(java.lang.String)}.
     */
    public void testDelete() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.delete(MCR_document_00000001.id);
        assertTrue(MyCoRe_document_00000001.id + " should not have been deleted", store.exists(MyCoRe_document_00000001.id));
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#update(java.lang.String, byte[], java.util.Date)}.
     */
    public void testUpdate() {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        store.update(MyCoRe_document_00000001_new.id, MyCoRe_document_00000001_new.blob, MyCoRe_document_00000001_new.lastModified);
        try {
            store.update(MCR_document_00000001.id, MCR_document_00000001.blob, MCR_document_00000001.lastModified);
            fail("Update for non existent " + MCR_document_00000001.id + " succeeded.");
        } catch (RuntimeException e) {
            //this exception is expected here
        }
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#retrieve(java.lang.String)}.
     * @throws IOException 
     * @throws JDOMException 
     */
    public void testRetrieve() throws JDOMException, IOException {
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        Document doc = getDocument(store.retrieve(MyCoRe_document_00000001.id));
        assertEquals("Stored document ID do not match:", MyCoRe_document_00000001.id, doc.getRootElement().getAttributeValue("id"));
        try {
            doc = getDocument(store.retrieve(MCR_document_00000001.id));
            if (doc != null) {
                fail("Requested " + MCR_document_00000001.id + ", retrieved wrong document:\n"
                        + new XMLOutputter(Format.getPrettyFormat()).outputString(doc));
            }
        } catch (Exception e) {
            //this is ok
        }
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#getHighestStoredID()}.
     */
    public void testGetHighestStoredID() {
        Method[] methods = store.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("getHighestStoredID") && method.getParameterTypes().length == 0)
                fail("org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2.getHighestStoredID() does not respect ProjectID");
        }
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#exists(java.lang.String)}.
     */
    public void testExists() {
        assertFalse("Object " + MyCoRe_document_00000001.id + " should not exist.", store.exists(MyCoRe_document_00000001.id));
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        assertTrue("Object " + MyCoRe_document_00000001.id + " should exist.", store.exists(MyCoRe_document_00000001.id));
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#retrieveAllIDs()}.
     */
    public void testRetrieveAllIDs() {
        assertEquals("Store should not contain any objects.", 0, store.retrieveAllIDs().size());
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        assertTrue("Store does not contain object " + MyCoRe_document_00000001.id, store.retrieveAllIDs().contains(
                MyCoRe_document_00000001.id));
    }

    /**
     * Test method for {@link org.mycore.datamodel.ifs2.MCRObjectMetadataStoreIFS2#listObjectDates(java.lang.String)}.
     */
    public void testListObjectDates() {
        assertEquals("Store should not contain any objects.", 0, store.listObjectDates("document").size());
        store.create(MyCoRe_document_00000001.id, MyCoRe_document_00000001.blob, MyCoRe_document_00000001.lastModified);
        assertEquals("Store should contain 1 object.", 1, store.listObjectDates("document").size());
        assertEquals("Last modified date of object do not match.", getRoundDate(MyCoRe_document_00000001.lastModified), getRoundDate(store
                .listObjectDates("document").get(0).getLastModified()));
    }

    private static Date getRoundDate(Date date) {
        long time = date.getTime();
        time -= time % 1000;
        return new Date(time);
    }

}
