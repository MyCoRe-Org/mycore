package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.*;

import java.text.MessageFormat;

import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;


public class MCRStoreManagerTest {
    @Test
    public void createMCRFileStore() throws Exception {
        String storeID = "Test";
        System.setProperty(getPropName(storeID, "BaseDir"), "fake");
        System.setProperty(getPropName(storeID, "SlotLayout"), "4-4-2");
        MCRFileStore fileStore = MCRStoreManager.createStore(storeID, MCRFileStore.class);
        
        assertNotNull("MCRStoreManager could not create Filestore.", fileStore);
    }
    
    private String getPropName(String storeID, String propType) {
        return MessageFormat.format("MCR.IFS2.Store.{0}.{1}", storeID, propType);
    }
}
