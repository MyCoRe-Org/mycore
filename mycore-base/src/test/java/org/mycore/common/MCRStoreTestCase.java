package org.mycore.common;

import java.nio.file.Path;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

public abstract class MCRStoreTestCase extends MCRJPATestCase {

    private static MCRXMLMetadataManager store;

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder svnBaseDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        super.setUp();
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

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Store.BaseDir", storeBaseDir.getRoot().getAbsolutePath());
        testProperties.put("MCR.Metadata.Store.SVNBase", svnBaseDir.getRoot().toURI().toString());
        return testProperties;
    }

}
