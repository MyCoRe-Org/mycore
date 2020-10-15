package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRDeleteObjectEvent extends MCRObjectEvent {

    MCRDeleteObjectEvent(MCRObjectID mcrObjectID) {
        super(mcrObjectID);
    }
}
