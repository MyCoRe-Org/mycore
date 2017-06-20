package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;
import org.mycore.datamodel.ifs2.MCRStoreAlreadyExistsException;
import org.mycore.datamodel.ifs2.MCRStoreCenter;

public class MCRStoreCenterTest {
    @Before
    public void init() {
        MCRStoreCenter.instance().clear();
    }

    @Test
    public void heapOp() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();

        FakeStoreConfig config = new FakeStoreConfig();
        storeHeap.addStore(config.getID(), new FakeStore(config));
        String storeID = config.getID();
        FakeStore fakeStore = storeHeap.getStore(storeID, FakeStore.class);

        assertNotNull("The store should be not null.", fakeStore);
        assertEquals("Fake Store", fakeStore.getMsg());

        assertTrue("Could not remove store with ID: " + storeID, storeHeap.removeStore(storeID));
        assertNull("There should be no store with ID: " + storeID, storeHeap.getStore(storeID, FakeStore.class));
    }

    @Test(expected = MCRStoreAlreadyExistsException.class)
    public void addStoreTwice() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();
        FakeStoreConfig config = new FakeStoreConfig();
        FakeStore fakeStore = new FakeStore(config);
        storeHeap.addStore(config.getID(), fakeStore);
        storeHeap.addStore(config.getID(), fakeStore);
    }

    class FakeStoreConfig implements MCRStoreConfig {

        @Override
        public String getID() {
            return "fake";
        }

        @Override
        public String getBaseDir() {
            return "ram://fake";
        }

        @Override
        public String getSlotLayout() {
            return "4-4-2";
        }

    }

    private String getPropName(String storeID, String propType) {
        return MessageFormat.format("MCR.IFS2.Store.{0}.{1}", storeID, propType);
    }

    public static class FakeStore extends MCRStore {
        private String msg = "Fake Store";

        public FakeStore(MCRStoreConfig config) {
            init(config);
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }
}
