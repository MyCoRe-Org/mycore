package org.mycore.datamodel.metadata;

import java.io.File;
import java.util.Date;

import org.mycore.common.MCRHibTestCase;
import org.mycore.datamodel.common.MCRXMLTableManager;

public class MCRObjectIDTest extends MCRHibTestCase {
    private static final String BASE_ID = "MyCoRe_test";

    private static File storeBaseDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storeBaseDir = File.createTempFile(MCRObjectIDTest.class.getSimpleName(), null);
        storeBaseDir.delete();
        storeBaseDir.mkdir();
        setProperty("MCR.Metadata.Type.test", Boolean.TRUE.toString(), true);
        setProperty("MCR.Metadata.Store.BaseDir", storeBaseDir.getAbsolutePath(), true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        recursiveDelete(storeBaseDir);
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

    public void testSetNextFreeIdString() {
        MCRObjectID id1 = new MCRObjectID();
        id1.setNextFreeId(BASE_ID);
        assertEquals("First id should be int 1", 1, id1.getNumberAsInteger());
        MCRObjectID id2 = new MCRObjectID();
        id2.setNextFreeId(BASE_ID);
        assertEquals("Second id should be int 2", 2, id2.getNumberAsInteger());
        MCRXMLTableManager.instance().create(id2, new byte[] { 'A' }, new Date());
        MCRObjectID id3 = new MCRObjectID();
        id3.setNextFreeId(BASE_ID);
        assertEquals("Second id should be int 3", 3, id3.getNumberAsInteger());
    }
}
