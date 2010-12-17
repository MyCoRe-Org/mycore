package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.*;

import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoreCenter;
import org.mycore.datamodel.ifs2.StoreAlreadyExistsException;

public class MCRStoreHeapTest {
    @Before
    public void init() {
        MCRStoreCenter.instance().clear();
    }

    @Test
    public void heapOp() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();

        String storeID = "fake";
        System.setProperty(getPropName(storeID, "BaseDir"), "fake");
        System.setProperty(getPropName(storeID, "SlotLayout"), "4-4-2");

        storeHeap.addStore(new FakeStore(storeID));
        FakeStore fakeStore = storeHeap.getStore(storeID, FakeStore.class);

        assertNotNull("The store should be not null.", fakeStore);
        assertEquals("Fake Store", fakeStore.getMsg());

        assertTrue("Could not remove store with ID: " + storeID, storeHeap.removeStore(storeID));
        assertNull("There should be no store with ID: " + storeID, storeHeap.getStore(storeID, FakeStore.class));
    }

    @Test(expected = StoreAlreadyExistsException.class)
    public void addStoreTwice() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();

        String storeID = "fake";
        System.setProperty(getPropName(storeID, "BaseDir"), "fake");
        System.setProperty(getPropName(storeID, "SlotLayout"), "4-4-2");

        FakeStore fakeStore = new FakeStore(storeID);
        storeHeap.addStore(fakeStore);
        storeHeap.addStore(fakeStore);
    }

    private String getPropName(String storeID, String propType) {
        return MessageFormat.format("MCR.IFS2.Store.{0}.{1}", storeID, propType);
    }

    public static class FakeStore extends MCRStore {
        private String msg = "Fake Store";

        public FakeStore(String storeID) {
            id = storeID;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }
}
