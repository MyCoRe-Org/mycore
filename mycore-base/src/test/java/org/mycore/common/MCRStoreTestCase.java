package org.mycore.common;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

public abstract class MCRStoreTestCase extends MCRHibTestCase {

    private static MCRXMLMetadataManager store;

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder svnBaseDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.Store.BaseDir", storeBaseDir.getRoot().getAbsolutePath(), true);
        Logger.getLogger(getClass()).info("SVN URI:" + svnBaseDir.getRoot().toURI().toString());
        setProperty("MCR.Metadata.Store.SVNBase", svnBaseDir.getRoot().toURI().toString(), true);
        store = MCRXMLMetadataManager.instance();
        store.reload();
    }

    public Path getStoreBaseDir() {
        return storeBaseDir.getRoot().toPath();
    }

    public Path getSvnBaseDir() {
        return svnBaseDir.getRoot().toPath();
    }

    public static MCRXMLMetadataManager getStore() {
        return store;
    }

}
