package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRContentStoreManager;


public class MCRStoreManagerTest {
    @Test
    public void createMCRFileStore() throws Exception {
        MCRFileStore fileStore = MCRContentStoreManager.create("Test", MCRFileStore.class);
        
//        assertNotNull("MCRStoreManager could not create Filestore.", fileStore);
    }

}
