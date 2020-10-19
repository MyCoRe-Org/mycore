package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObject;

public class MCRDeleteObjectEvent extends MCRObjectEvent {
    public MCRDeleteObjectEvent(MCRObject mcrObject) {
        super(mcrObject);
    }
}
