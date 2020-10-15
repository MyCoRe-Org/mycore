package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectEvent extends MCREvent {

    MCRObjectEvent(MCRObjectID mcrObjectID){
        this.mcrObjectID = mcrObjectID;
    }

    protected MCRObjectID mcrObjectID;

    public MCRObjectID getMcrObjectID() {
        return mcrObjectID;
    }

}
