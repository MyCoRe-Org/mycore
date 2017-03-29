package org.mycore.pi;

import mockit.Mock;
import mockit.MockUp;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by chi on 29.03.17.
 * @author Huu Chi Vu
 */
public class MockMetadataManager extends MockUp<MCRMetadataManager> {
    HashMap<MCRObjectID, MCRBase> objMap;

    public MockMetadataManager() {
        objMap = new HashMap<>();
    }

    @Mock
    public void update(final MCRDerivate mcrDerivate) throws MCRPersistenceException, IOException,
            MCRAccessException {
        System.out.println("Update: " + mcrDerivate.getId().toString());
    }

    @Mock
    public MCRBase retrieve(final MCRObjectID id) throws MCRPersistenceException {
        return objMap.get(id);
    }

    @Mock
    public MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        return (MCRDerivate) objMap.get(id);
    }

    public void put(MCRObjectID id, MCRBase obj) {
        objMap.put(id, obj);
    }
}
