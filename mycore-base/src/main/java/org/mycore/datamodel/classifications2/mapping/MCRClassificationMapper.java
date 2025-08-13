package org.mycore.datamodel.classifications2.mapping;

import org.mycore.datamodel.metadata.MCRObject;

public interface MCRClassificationMapper {

    /**
     * Creates classifications which are derived from the given object and adds them to the object.
     * @param object the object to create the mappings for
     */
    void createMappings(MCRObject object);

    /**
     * Removes all mappings from the given object.
     * @param object the object to remove the mappings from
     */
    void clearMappings(MCRObject object);

}
