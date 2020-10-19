package org.mycore.common.events2;

import org.mycore.datamodel.metadata.MCRObject;

public class MCRUpdateObjectEvent extends MCRObjectEvent {

    public MCRUpdateObjectEvent(MCRObject oldObject, MCRObject newObject) {
        super(newObject);
        this.oldObject = oldObject;
    }

    private final MCRObject oldObject;

    public MCRObject getOldObject() {
        return oldObject;
    }

}
