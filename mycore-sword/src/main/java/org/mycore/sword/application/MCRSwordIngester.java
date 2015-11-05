package org.mycore.sword.application;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.sword.MCRSwordUtil;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 * Let the application decide how to add metadata and resources.
 * Useful functions e.g. extract ZIP files can be found in {@link MCRSwordUtil}.
 */
public interface MCRSwordIngester extends MCRSwordLifecycle {
    /**
     * Will be called when the client tries to deposit an object with metadata.
     * @param entry the entry with metadata which should be added
     * @return object id for the new created object
     * @throws SwordError
     * @throws SwordServerException
     */
    MCRObjectID ingestMetadata(Deposit entry) throws SwordError, SwordServerException;

    /**
     * Will be called when the client tries to deposit an object with metadata and resources.
     * @param entry the entry with metadata and resources which should be added
     * @return  object id for the new created object
     * @throws SwordError
     * @throws SwordServerException
     */
    MCRObjectID ingestMetadataResources(Deposit entry) throws SwordError, SwordServerException;

    /**
     * Will be called when the client tries to add resources to an existing object.
     * @param object where the resources should be added
     * @param entry which contains the resources
     * @throws SwordError
     */
    void ingestResource(MCRObject object, Deposit entry) throws SwordError, SwordServerException;

    /**
     * Will be called when the client tries to update the metadata or replace existing metadata
     * @param object where metadata should be added or replaced
     * @param entry which contains metadata
     * @param replace indicates whether metadata should be added or replaced
     * @throws SwordError
     */
    void updateMetadata(MCRObject object, Deposit entry, boolean replace) throws SwordError, SwordServerException;

    /**
     * Will be called when the client tries to update the metadata and resources.
     * @param object where metadata and resources should be replaced
     * @param entry which contains metadata and reources
     * @throws SwordError
     */
    void updateMetadataResources(MCRObject object, Deposit entry) throws SwordError, SwordServerException;
}
