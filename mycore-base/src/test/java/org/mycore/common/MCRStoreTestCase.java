package org.mycore.common;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

public abstract class MCRStoreTestCase extends MCRHibTestCase {

    private static Path storeBaseDir;
    private static Path svnBaseDir;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        storeBaseDir = Files.createTempDirectory(getClass().getSimpleName());
        setProperty("MCR.Metadata.Store.BaseDir", storeBaseDir.toString(), true);
        svnBaseDir = Files.createTempDirectory(getClass().getSimpleName() + "_svn");
        setProperty("MCR.Metadata.Store.SVNBase", svnBaseDir.toString(), true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if(storeBaseDir != null) {
            FileUtils.deleteDirectory(storeBaseDir.toFile());
        }
        if(svnBaseDir != null) {
            FileUtils.deleteDirectory(svnBaseDir.toFile());
        }
    }

    public static Path getStoreBaseDir() {
        return storeBaseDir;
    }

    public static Path getSvnBaseDir() {
        return svnBaseDir;
    }

}
