package org.mycore.datamodel.metadata;

public class MCRMetaParentID extends MCRMetaLinkID {


    public MCRMetaParentID() {
        super();
    }

    public MCRMetaParentID(MCRObjectID parent) {
        super("parent", 0);
        setReference(parent, null, null);
    }


}
