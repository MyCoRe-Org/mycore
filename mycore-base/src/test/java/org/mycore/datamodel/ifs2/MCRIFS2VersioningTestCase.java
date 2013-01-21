package org.mycore.datamodel.ifs2;

import java.io.File;

import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.junit.After;

public class MCRIFS2VersioningTestCase extends MCRIFS2TestCase {
    private MCRVersioningMetadataStore versStore;

    protected void createStore() throws Exception {
        setProperties();
        File temp = File.createTempFile("base", "");
        String path = "file:///" + temp.getAbsolutePath().replace('\\', '/');
        temp.delete();
        setProperty("MCR.IFS2.Store.TEST.SVNRepositoryURL", path, true);
        
        setVersStore(MCRStoreManager.createStore(STORE_ID, MCRVersioningMetadataStore.class));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println("teardown: " + getVersStore().repURL);
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
}
