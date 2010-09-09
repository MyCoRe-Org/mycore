package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRHibTestCase;
import org.mycore.datamodel.common.MCRXMLTableManager;

public class MCRObjectIDTest extends MCRHibTestCase {
    private static final String BASE_ID = "MyCoRe_test";

    private static File storeBaseDir;
    private static File svnBaseDir;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        storeBaseDir = File.createTempFile(MCRObjectIDTest.class.getSimpleName(), null);
        storeBaseDir.delete();
        storeBaseDir.mkdir();
        setProperty("MCR.Metadata.Type.test", Boolean.TRUE.toString(), true);
        setProperty("MCR.Metadata.Store.BaseDir", storeBaseDir.getAbsolutePath(), true);
        svnBaseDir = File.createTempFile(MCRObjectIDTest.class.getSimpleName()+"_svn", null);
        svnBaseDir.delete();
        svnBaseDir.mkdir();
        String url = "file:///" + svnBaseDir.getAbsolutePath().replace('\\', '/');
        setProperty("MCR.Metadata.Store.SVNBase", url, true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        recursiveDelete(storeBaseDir);
        recursiveDelete(svnBaseDir);
    }

    /**
     * Recursively delete file or directory
     * @param fileOrDir
     *          the file or dir to delete
     * @return
     *          true if all files are successfully deleted
     */
    private static boolean recursiveDelete(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            // recursively delete contents
            for (File innerFile : fileOrDir.listFiles()) {
                if (!recursiveDelete(innerFile)) {
                    return false;
                }
            }
        }

        return fileOrDir.delete();
    }

    @Test
    public void setNextFreeIdString() {
        MCRObjectID id1 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("First id should be int 1", 1, id1.getNumberAsInteger());
        MCRObjectID id2 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 2", 2, id2.getNumberAsInteger());
        MCRXMLTableManager.instance().create(id2, new Document(new Element("test")), new Date());
        MCRObjectID id3 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 3", 3, id3.getNumberAsInteger());
    }
}
