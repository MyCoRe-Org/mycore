package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRUpdateObjectEvent extends MCRObjectEvent {
    MCRUpdateObjectEvent(MCRObjectID mcrObjectID) {
        super(mcrObjectID);
    }
}
