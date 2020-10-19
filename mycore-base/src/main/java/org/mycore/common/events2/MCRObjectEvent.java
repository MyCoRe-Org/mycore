package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObject;

public class MCRObjectEvent extends MCREvent {

    MCRObjectEvent(MCRObject mcrObject){
        this.mcrObject = mcrObject;
    }

    protected MCRObject mcrObject;

    public MCRObject getMcrObject() {
        return mcrObject;
    }

}
