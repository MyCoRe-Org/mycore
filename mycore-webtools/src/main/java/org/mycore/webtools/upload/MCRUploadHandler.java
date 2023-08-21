package org.mycore.webtools.upload;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Handles uploaded {@link MCRFileUploadBucket}s
 */
public interface MCRUploadHandler {

    /**
     * Traverses the given {@link MCRFileUploadBucket} and creates or updates the corresponding
     * {@link org.mycore.datamodel.metadata.MCRObject}
     * 
     * @param objectID the {@link MCRObjectID} of the {@link org.mycore.datamodel.metadata.MCRObject} or
     * {@link org.mycore.datamodel.metadata.MCRDerivate} to create or update
     * @param bucket the {@link MCRFileUploadBucket} to traverse
     * @param classifications the {@link MCRMetaClassification}s to add to the Derivate
     */
    URI commit(MCRObjectID objectID, MCRFileUploadBucket bucket, List<MCRMetaClassification> classifications);

    /**
     * Validates the given {@link MCRObjectID} and throws an exception if the object is not valid
     *
     * @param oid the {@link MCRObjectID} to validate
     * @throws ForbiddenException if the user is not allowed to access the object
     * @throws BadRequestException if the object is not valid
     */
    void validateObject(MCRObjectID oid) throws ForbiddenException, BadRequestException;
}
