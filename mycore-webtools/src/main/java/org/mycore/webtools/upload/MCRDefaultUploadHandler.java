/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.webtools.upload;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.fileupload.MCRUploadHelper;
import org.mycore.webtools.upload.exception.MCRInvalidFileException;
import org.mycore.webtools.upload.exception.MCRInvalidUploadParameterException;
import org.mycore.webtools.upload.exception.MCRMissingParameterException;
import org.mycore.webtools.upload.exception.MCRUploadForbiddenException;
import org.mycore.webtools.upload.exception.MCRUploadServerException;

/**
 * Default implementation of the {@link MCRUploadHandler} interface.
 * This implementation uploads files to a derivate and assigns a mainfile if none is set.
 *
 * <dl>
 *     <dt>{@link #OBJ_OR_DERIVATE_ID_PARAMETER_NAME}</dt>
 *     <dd>The derivate id where the files should be uploaded to. It can also be an object id, in this case a new
 *     derivate will be created.</dd>
 *     <dt>{@link #CLASSIFICATIONS_PARAMETER_NAME}</dt>
 *     <dd>A comma separated list of classifications that should be added to the new derivate.</dd>
 * </dl>
 *
 * The following configuration properties are used:
 * <dl>
 *     <dt>MCR.Upload.NotPreferredFiletypeForMainfile</dt>
 *     <dd>A comma separated list of file extensions that should not be used as mainfile. </dd>
 * </dl>
 */
public class MCRDefaultUploadHandler implements MCRUploadHandler {

    public static final String OBJ_OR_DERIVATE_ID_PARAMETER_NAME = "object";

    public static final String CLASSIFICATIONS_PARAMETER_NAME = "classifications";

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String UNIQUE_OBJECT_TRANSLATION_KEY = "component.webtools.upload.invalid.parameter.unique";
    public static final String INVALID_OBJECT_TRANSLATION_KEY = "component.webtools.upload.invalid.parameter.object";
    public static final String OBJECT_DOES_NOT_EXIST_TRANSLATION_KEY =
        "component.webtools.upload.invalid.parameter.object.not.exist";
    public static final String INVALID_FILE_NAME_TRANSLATION_KEY = "component.webtools.upload.invalid.fileName";
    public static final String INVALID_FILE_SIZE_TRANSLATION_KEY = "component.webtools.upload.invalid.fileSize";

    public static void setDefaultMainFile(MCRDerivate derivate) {
        MCRPath path = MCRPath.getPath(derivate.getId().toString(), "/");
        try {
            MCRUploadHelper.detectMainFile(path).ifPresent(file -> {
                LOGGER.info("Setting main file to {}", file::toUri);
                derivate.getDerivate().getInternals().setMainDoc(file.getOwnerRelativePath());
                try {
                    MCRMetadataManager.update(derivate);
                } catch (MCRPersistenceException | MCRAccessException e) {
                    LOGGER.error("Could not set main file!", e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Could not set main file!", e);
        }
    }

    @Override
    public URI commit(MCRFileUploadBucket bucket) throws MCRUploadServerException {
        Map<String, List<String>> parameters = bucket.getParameters();
        final MCRObjectID objOrDerivateID = getObjectID(parameters);
        final List<MCRMetaClassification> classifications = getClassifications(parameters);
        final Path root = bucket.getRoot();
        final boolean isDerivate = MCRDerivate.OBJECT_TYPE.equals(objOrDerivateID.getTypeId());

        final MCRPath targetDerivateRoot;

        MCRObjectID derivateID = objOrDerivateID;
        if (isDerivate) {
            targetDerivateRoot = MCRPath.getPath(objOrDerivateID.toString(), "/");
        } else {
            try {
                derivateID = MCRUploadHelper.createDerivate(objOrDerivateID, classifications).getId();
                targetDerivateRoot = MCRPath.getPath(derivateID.toString(), "/");
            } catch (MCRAccessException e) {
                throw new MCRUploadServerException("mcr.upload.create.derivate.failed", e);
            }
            // check if we have write access to the newly created derivate
            if (!MCRAccessManager.checkPermission(derivateID, MCRAccessManager.PERMISSION_WRITE)) {
                throw new MCRUploadServerException("No write access to newly created derivate " + derivateID);
            }
        }

        final MCRTreeCopier copier;
        try {
            copier = new MCRTreeCopier(root, targetDerivateRoot, false, true);
        } catch (NoSuchFileException e) {
            throw new MCRException(e);
        }

        try {
            Files.walkFileTree(root, copier);
        } catch (IOException e) {
            throw new MCRUploadServerException("mcr.upload.import.failed", e);
        }

        MCRDerivate theDerivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);

        String mainDoc = theDerivate.getDerivate().getInternals().getMainDoc();
        if (mainDoc == null || mainDoc.isEmpty()) {
            setDefaultMainFile(theDerivate);
        }

        return null; // We don´t want to redirect to the derivate, so we return null
    }

    private static void checkPermissions(MCRObjectID oid) throws MCRInvalidUploadParameterException,
        MCRUploadForbiddenException {
        if (!MCRMetadataManager.exists(oid)) {
            throw new MCRInvalidUploadParameterException(OBJ_OR_DERIVATE_ID_PARAMETER_NAME, oid.toString(),
                OBJECT_DOES_NOT_EXIST_TRANSLATION_KEY, true);
        }

        if (!oid.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
            try {
                String derivateId = MCRObjectID.formatID(oid.getProjectId(), MCRDerivate.OBJECT_TYPE, 0);
                MCRObjectID mcrDerivateId = MCRObjectID.getInstance(derivateId);
                MCRMetadataManager.checkCreatePrivilege(mcrDerivateId);
            } catch (MCRAccessException e) {
                MCRUploadForbiddenException uploadForbiddenException = new MCRUploadForbiddenException(e.getMessage());
                uploadForbiddenException.initCause(e);
                throw uploadForbiddenException;
            }
        }

        if (!MCRAccessManager.checkPermission(oid, MCRAccessManager.PERMISSION_WRITE)) {
            throw new MCRUploadForbiddenException();
        }
    }

    @Override
    public void validateFileMetadata(String name, long size) throws MCRInvalidFileException {
        try {
            MCRUploadHelper.checkPathName(name);
        } catch (MCRException e) {
            MCRInvalidFileException invalidFileException =
                new MCRInvalidFileException(name, INVALID_FILE_NAME_TRANSLATION_KEY, true);
            invalidFileException.initCause(e);
            throw invalidFileException;
        }

        long maxSize = MCRConfiguration2.getOrThrow("MCR.FileUpload.MaxSize", Long::parseLong);
        if (size > maxSize) {
            throw new MCRInvalidFileException(name, INVALID_FILE_SIZE_TRANSLATION_KEY, true,
                MCRUtils.getSizeFormatted(size), MCRUtils.getSizeFormatted(maxSize));
        }
    }

    @Override
    public String begin(Map<String, List<String>> parameters)
        throws MCRUploadForbiddenException, MCRMissingParameterException, MCRInvalidUploadParameterException {
        if (!parameters.containsKey(OBJ_OR_DERIVATE_ID_PARAMETER_NAME)) {
            throw new MCRMissingParameterException(OBJ_OR_DERIVATE_ID_PARAMETER_NAME);
        }

        List<String> oidList = parameters.get(OBJ_OR_DERIVATE_ID_PARAMETER_NAME);
        if (oidList.size() != 1) {
            throw new MCRInvalidUploadParameterException(OBJ_OR_DERIVATE_ID_PARAMETER_NAME, String.join(",", oidList),
                UNIQUE_OBJECT_TRANSLATION_KEY, true);
        }

        String oidString = oidList.getFirst();
        if (!MCRObjectID.isValid(oidString)) {
            throw new MCRInvalidUploadParameterException(OBJ_OR_DERIVATE_ID_PARAMETER_NAME, oidString,
                INVALID_OBJECT_TRANSLATION_KEY, true);
        }

        MCRObjectID oid = MCRObjectID.getInstance(oidString);

        checkPermissions(oid);

        return UUID.randomUUID().toString();
    }

    /**
     * returns the object id from the parameters under the key {@link #OBJ_OR_DERIVATE_ID_PARAMETER_NAME}
     * @param parameters the parameters
     * @return the object id
     */
    public static MCRObjectID getObjectID(Map<String, List<String>> parameters) {
        return MCRObjectID.getInstance(parameters.get(OBJ_OR_DERIVATE_ID_PARAMETER_NAME).getFirst());
    }

    /**
     * Gets the classifications from the parameters under the key {@link #CLASSIFICATIONS_PARAMETER_NAME}.
     * @param parameters the parameters
     * @return the classifications
     */
    public static List<MCRMetaClassification> getClassifications(Map<String, List<String>> parameters) {
        List<String> classificationParameters = parameters.get(CLASSIFICATIONS_PARAMETER_NAME);
        return MCRUploadHelper
            .getClassifications(classificationParameters != null ? String.join(",", classificationParameters) : null);
    }
}
