package org.mycore.datamodel.ifs2;

import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.MCRTestCase;

public class MCRIFS2TestCase extends MCRTestCase {

    protected static final String STORE_ID = "TEST";

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    private MCRFileStore store;

    protected void createStore() throws Exception {
        System.out.println(getClass() + " store id: " + STORE_ID);
        setStore(MCRStoreManager.createStore(STORE_ID, MCRFileStore.class));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createStore();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        MCRStoreManager.removeStore(STORE_ID);
        assertNull("Could not remove store " + STORE_ID, MCRStoreManager.getStore(STORE_ID));
        store = null;
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

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.IFS2.Store.TEST.BaseDir", storeBaseDir.getRoot().getAbsolutePath());
        testProperties.put("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2");
        return testProperties;
    }
}
