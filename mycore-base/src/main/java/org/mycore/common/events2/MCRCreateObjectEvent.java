package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObject;

public class MCRCreateObjectEvent extends MCRObjectEvent {
    public MCRCreateObjectEvent(MCRObject mcrObject) {
        super(mcrObject);
    }
}
