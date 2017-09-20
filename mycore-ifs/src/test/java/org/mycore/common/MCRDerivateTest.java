package org.mycore.common;

import org.junit.Test;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRDerivateTest extends MCRIFSTest {

    MCRObject root;

    MCRDerivate derivate;

    @Test
    public void create() throws Exception {
        root = createObject();
        derivate = createDerivate(root.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(root);
        super.tearDown();
    }

}
