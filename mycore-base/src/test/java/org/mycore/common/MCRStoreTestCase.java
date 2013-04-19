package org.mycore.common;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

public abstract class MCRStoreTestCase extends MCRHibTestCase {

    private static MCRXMLMetadataManager store;

    private static Path storeBaseDir;

    private static Path svnBaseDir;

    @BeforeClass
    public static void init() throws Exception {
        storeBaseDir = Files.createTempDirectory(MCRStoreTestCase.class.getSimpleName());
        setProperty("MCR.Metadata.Store.BaseDir", storeBaseDir.toString(), true);
        svnBaseDir = Files.createTempDirectory(MCRStoreTestCase.class.getSimpleName() + "_svn");
        setProperty("MCR.Metadata.Store.SVNBase", svnBaseDir.toUri().toString(), true);
        store = MCRXMLMetadataManager.instance();
        store.reload();
    }

    @AfterClass
    public static void destroy() throws Exception {
        if (storeBaseDir != null) {
            FileUtils.deleteDirectory(storeBaseDir.toFile());
        }
        if (svnBaseDir != null) {
            FileUtils.deleteDirectory(svnBaseDir.toFile());
        }
    }

    public static Path getStoreBaseDir() {
        return storeBaseDir;
    }

    public static Path getSvnBaseDir() {
        return svnBaseDir;
    }

    public static MCRXMLMetadataManager getStore() {
        return store;
    }

}
