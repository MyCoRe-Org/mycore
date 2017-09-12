package org.mycore.common;

import org.junit.Ignore;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.Map;

@Ignore
public class MCRDerivateTest extends MCRStoreTestCase {

    MCRObject root;

    MCRDerivate derivate;

    @Test
    public void create() throws Exception {
        MCREventManager.instance().clear().addEventHandler("MCRObject", new MCRXMLMetadataEventHandler());
        root = new MCRObject();
        root.setId(MCRObjectID.getInstance("mycore_object_00000001"));
        root.setSchema("noSchema");
        derivate = createDerivate();
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties
                .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Access.Strategy.Class", AlwaysTrueStrategy.class.getName());
        testProperties.put("MCR.Metadata.Type.object", "true");
        return testProperties;
    }

    private MCRDerivate createDerivate() {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRObjectID.getNextFreeId("mycore_derivate"));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID();
        mcrMetaLinkID.setReference("mycore_object_00000001", null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(root);
        super.tearDown();
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }

}
