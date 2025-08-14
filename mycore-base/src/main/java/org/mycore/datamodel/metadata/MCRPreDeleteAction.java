package org.mycore.datamodel.metadata;

public interface MCRPreDeleteAction {

    default void execute(MCRObjectID id) {
        // do nothing
    }
}
