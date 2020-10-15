package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRCreateObjectEvent extends MCRObjectEvent {

    MCRCreateObjectEvent(MCRObjectID mcrObjectID) {
        super(mcrObjectID);
    }

}
