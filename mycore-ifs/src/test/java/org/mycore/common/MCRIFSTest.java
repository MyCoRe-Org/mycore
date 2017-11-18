package org.mycore.common;

import org.junit.Before;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.Map;

public abstract class MCRIFSTest extends MCRStoreTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCREventManager.instance().clear().addEventHandler("MCRObject", new MCRXMLMetadataEventHandler());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.datadir", "%MCR.basedir%/data");
        testProperties
            .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Access.Strategy.Class", AlwaysTrueStrategy.class.getName());
        testProperties.put("MCR.Metadata.Type.object", "true");
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        return testProperties;
    }

    public static MCRObject createObject() {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getNextFreeId("mycore_object"));
        object.setSchema("noSchema");
        return object;
    }

    public static MCRDerivate createDerivate(MCRObjectID objectHrefId) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRObjectID.getNextFreeId("mycore_derivate"));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID();
        mcrMetaLinkID.setReference(objectHrefId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }

}
