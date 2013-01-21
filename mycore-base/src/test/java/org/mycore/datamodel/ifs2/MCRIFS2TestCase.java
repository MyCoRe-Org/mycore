package org.mycore.datamodel.ifs2;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Before;
import org.mycore.common.MCRTestCase;

public class MCRIFS2TestCase extends MCRTestCase {

    protected static final String STORE_ID = "TEST";

    private MCRFileStore store;

    protected void createStore() throws Exception {
        setProperties();
        System.out.println(getClass() + " store id: " + STORE_ID);
        setStore(MCRStoreManager.createStore(STORE_ID, MCRFileStore.class));
    }

    protected void setProperties() throws IOException {
        File temp = File.createTempFile("base", "");
        String path = temp.getAbsolutePath();
        temp.delete();

        setProperty("MCR.IFS2.Store.TEST.BaseDir", path, true);
        setProperty("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2", true);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (getGenericStore() == null) {
            createStore();
        } else {
            VFS.getManager().resolveFile(getGenericStore().getBaseDirURI()).createFolder();
        }

        assertTrue("Store is not Empty", getGenericStore().isEmpty());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        VFS.getManager().resolveFile(getGenericStore().getBaseDirURI()).delete(Selectors.SELECT_ALL);
        MCRStoreManager.removeStore(STORE_ID);
        assertNull("Could not remove store " + STORE_ID, MCRStoreManager.getStore(STORE_ID));
    }

    public void setStore(MCRFileStore store) {
        this.store = store;
    }

    public MCRFileStore getStore() {
        return store;
    }

    public MCRStore getGenericStore() {
        return getStore();
    }
}