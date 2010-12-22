package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.*;

import java.text.MessageFormat;

import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;


public class MCRStoreManagerTest {
    @Test
    public void createMCRFileStore() throws Exception {
        MCRFileStore fileStore = MCRStoreManager.createStore(new StoreConfig(), MCRFileStore.class);
        
        assertNotNull("MCRStoreManager could not create Filestore.", fileStore);
    }
    
    class StoreConfig implements MCRStoreConfig{

        @Override
        public String getID() {
            return "Test";
        }

        @Override
        public String getBaseDir() {
            return "ram:///fake";
        }

        @Override
        public String getSlotLayout() {
            return "4-4-2";
        }
        
    }
}
