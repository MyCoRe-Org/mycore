package org.mycore.datamodel.ifs2;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class MCRIFS2VersioningTestCase extends MCRIFS2TestCase {
    @Rule
    public TemporaryFolder versionBaseDir = new TemporaryFolder();

    private MCRVersioningMetadataStore versStore;

    @Override
    protected void createStore() throws Exception {
        setVersStore(MCRStoreManager.createStore(STORE_ID, MCRVersioningMetadataStore.class));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println("teardown: " + getVersStore().repURL);
        MCRStoreManager.removeStore(STORE_ID);
        VFS.getManager().resolveFile(getVersStore().repURL.getPath()).delete(Selectors.SELECT_ALL);
    }

    public void setVersStore(MCRVersioningMetadataStore versStore) {
        this.versStore = versStore;
    }

    public MCRVersioningMetadataStore getVersStore() {
        return versStore;
    }

    @Override
    public MCRStore getGenericStore() {
        return getVersStore();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        try {
            String url = versionBaseDir.getRoot().toURI().toURL().toString();
            testProperties.put("MCR.IFS2.Store.TEST.SVNRepositoryURL", url);
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }
        return testProperties;
    }
}
