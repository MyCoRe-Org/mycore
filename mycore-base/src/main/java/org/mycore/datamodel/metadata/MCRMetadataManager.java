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

package org.mycore.datamodel.metadata;

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import static org.mycore.datamodel.metadata.MCRObjectService.DATE_TYPE_CREATEDATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPermissionException;
import org.mycore.access.MCRMissingPrivilegeException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRExpandedObjectManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.common.MCRMarkManager.Operation;
import org.mycore.datamodel.common.MCRObjectIDGenerator;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.normalization.MCRObjectNormalizer;
import org.mycore.datamodel.metadata.validator.MCRObjectValidator;
import org.mycore.datamodel.metadata.validator.MCRValidationResult;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;

/**
 * Delivers persistence operations for {@link MCRObject} and {@link MCRDerivate} .
 *
 * @author Thomas Scheffler (yagee)
 * @since 2.0.92
 */
public final class MCRMetadataManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRObjectIDGenerator MCROBJECTID_GENERATOR = MCRConfiguration2.getSingleInstanceOfOrThrow(
        MCRObjectIDGenerator.class, "MCR.Metadata.ObjectID.Generator.Class");

    public static final String VALIDATORS_PROPERTY_PREFIX = "MCR.Metadata.Validator.";

    public static final String NORMALIZERS_PROPERTY_PREFIX = "MCR.Metadata.Normalizer.";

    public static final String DEFAULT_IMPLEMENTATION_KEY = "Default";

    private MCRMetadataManager() {
    }

    public static MCRObjectIDGenerator getMCRObjectIDGenerator() {
        return MCROBJECTID_GENERATOR;
    }

    /**
     * Stores the derivate.
     *
     * @param mcrDerivate
     *            derivate instance to store
     * @throws MCRPersistenceException
     *            if a persistence problem is occurred
     * @throws MCRAccessException
     *            if write permission to object is missing or create permission to derivate is missing
     */
    public static void create(final MCRDerivate mcrDerivate) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID derivateId = mcrDerivate.getId();
        checkCreatePrivilege(derivateId);
        throwPersistenceExceptionIfExists(derivateId, MCRDerivate.OBJECT_TYPE);

        derivateId = assignNewIdIfNecessary(mcrDerivate);

        validateDerivate(mcrDerivate, derivateId);

        final MCRObjectID objectId = getObjectIDFromDerivate(mcrDerivate);
        checkWritePermission(objectId, derivateId);

        byte[] objectBackup = retrieveObjectBackup(objectId);

        setDerivateMetadata(mcrDerivate);

        fireEvent(mcrDerivate, null, MCREvent.EventType.CREATE);

        createDataInIFS(mcrDerivate, derivateId, objectId, objectBackup);

    }

    private static MCRObjectID assignNewIdIfNecessary(MCRDerivate mcrDerivate) {
        MCRObjectID derivateId = mcrDerivate.getId();
        if (derivateId.getNumberAsInteger() == 0) {
            derivateId = getMCRObjectIDGenerator().getNextFreeId(derivateId.getBase());
            mcrDerivate.setId(derivateId);
            LOGGER.info("Assigned new derivate id {}", derivateId);
        }
        return derivateId;
    }

    private static void validateDerivate(MCRDerivate mcrDerivate, MCRObjectID derivateId)
        throws MCRPersistenceException {
        try {
            mcrDerivate.validate();
        } catch (MCRException exc) {
            throw new MCRPersistenceException("The derivate " + derivateId + " is not valid.", exc);
        }
    }

    private static MCRObjectID getObjectIDFromDerivate(MCRDerivate mcrDerivate) {
        return mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
    }

    private static void checkWritePermission(MCRObjectID objectId, MCRObjectID derivateId) throws MCRAccessException {
        if (!MCRAccessManager.checkPermission(objectId, PERMISSION_WRITE)) {
            throw new MCRMissingPermissionException("Add derivate " + derivateId + " to object.",
                objectId.toString(), PERMISSION_WRITE);
        }
    }

    private static byte[] retrieveObjectBackup(MCRObjectID objectId) throws MCRPersistenceException {
        byte[] objectBackup;
        try {
            objectBackup = MCRXMLMetadataManager.getInstance().retrieveBLOB(objectId);
        } catch (IOException ioExc) {
            throw new MCRPersistenceException("Unable to retrieve xml blob of " + objectId, ioExc);
        }
        if (objectBackup == null) {
            throw new MCRPersistenceException("Cannot find " + objectId + " to attach derivate.");
        }
        return objectBackup;
    }

    private static void setDerivateMetadata(MCRDerivate mcrDerivate) {
        boolean importMode = mcrDerivate.isImportMode();
        if (mcrDerivate.getService().getDate(DATE_TYPE_CREATEDATE) == null || !importMode) {
            mcrDerivate.getService().setDate(DATE_TYPE_CREATEDATE);
        }
        if (mcrDerivate.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE) == null || !importMode) {
            mcrDerivate.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        }
    }

    private static void createDataInIFS(MCRDerivate mcrDerivate, MCRObjectID derivateId, MCRObjectID objectId,
        byte[] objectBackup) throws MCRPersistenceException {
        try {
            processDerivate(mcrDerivate, derivateId, objectBackup);
        } catch (Exception e) {
            restore(mcrDerivate, objectId, objectBackup);
            throw new MCRPersistenceException("Error during data creation in IFS.", e);
        }
    }

    private static void processDerivate(MCRDerivate mcrDerivate, MCRObjectID objectId,
        byte[] objectBackup) {
        MCRObjectID derivateId = mcrDerivate.getId();
        if (mcrDerivate.getDerivate().getInternals() != null) {
            MCRPath rootPath = MCRPath.getPath(derivateId.toString(), "/");
            if (mcrDerivate.getDerivate().getInternals().getSourcePath() == null) {
                try {
                    rootPath.getFileSystem().createRoot(rootPath.getOwner());
                } catch (IOException ioExc) {
                    throw new MCRPersistenceException(
                        "Cannot create root of '" + rootPath.getOwner() + "'.", ioExc);
                }
            } else {
                final String sourcepath = mcrDerivate.getDerivate().getInternals().getSourcePath();
                final Path f = Paths.get(sourcepath);
                if (Files.exists(f)) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Starting File-Import");
                        }
                        importDerivate(derivateId.toString(), f);
                    } catch (final Exception e) {
                        if (Files.exists(rootPath)) {
                            deleteDerivate(derivateId.toString());
                        }
                        restore(mcrDerivate, objectId, objectBackup);
                        throw new MCRPersistenceException("Can't add derivate to the IFS", e);
                    }
                } else {
                    LOGGER.warn("Empty derivate, the File or Directory -->{}<--  was not found.", sourcepath);
                }
            }
        }
    }

    private static void throwPersistenceExceptionIfExists(MCRObjectID id, String typeName) {
        if (exists(id)) {
            throw new MCRPersistenceException("The" + typeName + " " + id + " already exists, nothing done.");
        }
    }

    private static void deleteDerivate(String derivateID) throws MCRPersistenceException {
        try {
            MCRPath rootPath = MCRPath.getPath(derivateID, "/");
            if (!Files.exists(rootPath)) {
                LOGGER.info("Derivate does not exist: {}", derivateID);
                return;
            }
            Files.walkFileTree(rootPath, new MCRRecursiveDeleter());
            rootPath.getFileSystem().removeRoot(derivateID);
        } catch (Exception exc) {
            throw new MCRPersistenceException("Unable to delete derivate " + derivateID, exc);
        }
    }

    private static void importDerivate(String derivateID, Path sourceDir) throws MCRPersistenceException {
        try {
            MCRPath rootPath = MCRPath.getPath(derivateID, "/");
            if (Files.exists(rootPath)) {
                LOGGER.info("Derivate does already exist: {}", derivateID);
            }
            rootPath.getFileSystem().createRoot(derivateID);
            Files.walkFileTree(sourceDir, new MCRTreeCopier(sourceDir, rootPath));
        } catch (Exception exc) {
            throw new MCRPersistenceException(
                "Unable to import derivate " + derivateID + " from source " + sourceDir.toAbsolutePath(),
                exc);
        }
    }

    /**
     * Stores the object.
     *
     * @param mcrObject
     *            object instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRAccessException if "create-{objectType}" privilege is missing
     */
    public static void create(final MCRObject mcrObject) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID objectId = Objects.requireNonNull(mcrObject.getId(), "ObjectID must not be null");

        checkCreatePrivilege(objectId);
        // exist the object?
        if (exists(objectId)) {
            throw new MCRPersistenceException("The object " + objectId + " already exists, nothing done.");
        }

        normalizeObject(mcrObject);

        validateObject(mcrObject);

        // handle events
        fireEvent(mcrObject, null, MCREvent.EventType.CREATE);
    }

    public static void checkCreatePrivilege(MCRObjectID objectId) throws MCRAccessException {
        String createBasePrivilege = "create-" + objectId.getBase();
        String createTypePrivilege = "create-" + objectId.getTypeId();
        if (!MCRAccessManager.checkPermission(createBasePrivilege)
            && !MCRAccessManager.checkPermission(createTypePrivilege)) {
            throw new MCRMissingPrivilegeException("Create base with id " + objectId, createBasePrivilege,
                createTypePrivilege);
        }
    }

    /**
     * Deletes MCRDerivate.
     *
     * @param mcrDerivate
     *            to be deleted
     * @throws MCRPersistenceException
     *            if persistence problem occurs
     * @throws MCRAccessException
     *            if delete permission is missing
     */
    public static void delete(final MCRDerivate mcrDerivate) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID id = mcrDerivate.getId();
        if (!MCRAccessManager.checkDerivateContentPermission(id, PERMISSION_DELETE)) {
            throw new MCRMissingPermissionException("Delete derivate", id.toString(), PERMISSION_DELETE);
        }
        // mark for deletion
        MCRMarkManager.getInstance().mark(id, Operation.DELETE);

        // delete data from IFS
        if (mcrDerivate.getDerivate().getInternals() != null) {
            try {
                deleteDerivate(id.toString());
                LOGGER.info("IFS entries for MCRDerivate {} are deleted.", id);
            } catch (final Exception e) {
                throw new MCRPersistenceException("Error while delete MCRDerivate " + id + " in IFS", e);
            }
        }

        // handle events
        fireEvent(mcrDerivate, null, MCREvent.EventType.DELETE);

        // remove mark
        MCRMarkManager.getInstance().remove(id);
    }

    /**
     * Deletes the <code>mcrObject</code>.
     *
     * @param mcrObject
     *            the object to be deleted
     * @throws MCRPersistenceException
     *            if persistence problem occurs
     * @throws MCRActiveLinkException
     *            object couldn't  be deleted because its linked somewhere
     * @throws MCRAccessException
     *            if delete permission is missing
     */
    public static void delete(final MCRObject mcrObject)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID id = mcrObject.getId();
        if (id == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        checkDeletePermission(id);

        checkForActiveLinks(mcrObject, id);

        markForDeletion(id);

        removeAllChildren(mcrObject);

        removeAllDerivates(mcrObject);

        fireEvent(mcrObject, null, MCREvent.EventType.DELETE);

        removeMark(id);
    }

    private static void removeAllChildren(MCRObject mcrObject)
        throws MCRPersistenceException, MCRAccessException, MCRActiveLinkException {
        for (MCRObjectID childId : getChildren(mcrObject.getId())) {
            if (!exists(childId)) {
                LOGGER.warn("Unable to remove not existing object {} of parent {}", () -> childId, mcrObject::getId);
                continue;
            }
            deleteMCRObject(childId);
        }
    }

    private static void removeAllDerivates(MCRObject mcrObject) throws MCRPersistenceException, MCRAccessException {
        for (MCRObjectID derivateID : getDerivateIds(mcrObject.getId())) {
            if (exists(derivateID)) {
                deleteMCRDerivate(derivateID);
            } else {
                LOGGER.warn("Derivate {} does not exist. (present in database)", derivateID);
            }
        }

    }

    private static void checkDeletePermission(MCRObjectID id) throws MCRAccessException {
        if (!MCRAccessManager.checkPermission(id, PERMISSION_DELETE)) {
            throw new MCRMissingPermissionException("Delete object", id.toString(), PERMISSION_DELETE);
        }
    }

    private static void checkForActiveLinks(MCRObject mcrObject, MCRObjectID id) throws MCRActiveLinkException {
        final Collection<String> sources = MCRLinkTableManager.getInstance().getSourceOf(mcrObject.mcrId,
            MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sources size:{}", sources.size());
        }
        if (!sources.isEmpty()) {
            final MCRActiveLinkException activeLinks = new MCRActiveLinkException("Error while deleting object " + id
                + ". This object is still referenced by other objects and "
                + "can not be removed until all links are released.");
            for (final String curSource : sources) {
                activeLinks.addLink(curSource, id.toString());
            }
            throw activeLinks;
        }
    }

    private static void markForDeletion(MCRObjectID id) {
        MCRMarkManager.getInstance().mark(id, Operation.DELETE);
    }

    private static void removeMark(MCRObjectID id) {
        MCRMarkManager.getInstance().remove(id);
    }

    /**
     * Delete the derivate. The order of delete steps is:<br>
     * <ul>
     * <li>remove link in object metadata</li>
     * <li>remove all files from IFS</li>
     * <li>remove derivate</li>
     * </ul>
     *
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRAccessException if delete permission is missing
     */
    public static void deleteMCRDerivate(final MCRObjectID id) throws MCRPersistenceException, MCRAccessException {
        final MCRDerivate derivate = retrieveMCRDerivate(id);
        delete(derivate);
    }


    /**
     * Deletes the mcr object with the given <code>id</code>.
     *
     * @param id
     *            the object to be deleted
     * @throws MCRPersistenceException
     *            if persistence problem occurs
     * @throws MCRActiveLinkException
     *            object couldn't  be deleted because its linked somewhere
     * @throws MCRAccessException if delete permission is missing
     */
    public static void deleteMCRObject(final MCRObjectID id)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        final MCRObject object = retrieveMCRObject(id);
        delete(object);
    }

    /**
     * Tells if the object or derivate with <code>id</code> exists.
     *
     * @param id
     *            the object ID
     * @throws MCRPersistenceException
     *            the xml couldn't be read
     */
    public static boolean exists(final MCRObjectID id) throws MCRPersistenceException {
        return MCRXMLMetadataManager.getInstance().exists(id);
    }

    /**
     * Fires {@link MCREvent.EventType#REPAIR} for given derivate.
     *
     */
    public static void fireRepairEvent(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // handle events
        fireEvent(mcrDerivate, null, MCREvent.EventType.REPAIR);
    }

    /**
     * Fires {@link MCREvent.EventType#REPAIR} for given object.
     *
     */
    public static void fireRepairEvent(final MCRBase mcrBaseObj) throws MCRPersistenceException {
        if (mcrBaseObj instanceof MCRDerivate derivate) {
            fireRepairEvent(derivate);
        } else if (mcrBaseObj instanceof MCRObject object) {
            fireRepairEvent(object);
        }
    }

    /**
     * Fires {@link MCREvent.EventType#REPAIR} for given object.
     *
     */
    public static void fireRepairEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        /* TODO: replace with link variant
        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            if (!exists(derivate.getXLinkHrefID())) {
                LOGGER.error("Can't find MCRDerivate {}", derivate::getXLinkHrefID);
            }
        }
        */

        // handle events
        fireEvent(mcrObject, null, MCREvent.EventType.REPAIR);
    }

    /**
     * Fires {@link MCREvent.EventType#UPDATE} for given object. If {@link MCRObject#isImportMode()} modifydate
     * will not be updated.
     *
     * @param mcrObject
     *            mycore object which is updated
     */
    public static void fireUpdateEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        if (!mcrObject.isImportMode() ||
            mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE) == null) {
            mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        }
        // remove ACL if it is set from data source
        mcrObject.getService().getRules().clear();
        // handle events
        fireEvent(mcrObject, retrieveMCRObject(mcrObject.getId()), MCREvent.EventType.UPDATE);
    }

    /**
     * Retrieves instance of {@link MCRDerivate} with the given {@link MCRObjectID}
     *
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        try {
            Document xml = MCRXMLMetadataManager.getInstance().retrieveXML(id);
            if (xml == null) {
                throw new MCRPersistenceException("Could not retrieve xml of derivate: " + id);
            }
            return new MCRDerivate(xml);
        } catch (IOException | JDOMException e) {
            throw new MCRPersistenceException("Could not retrieve xml of derivate: " + id, e);
        }
    }

    /**
     * Retrieves an instance of {@link MCRObject} with the given {@link MCRObjectID}
     *
     * @param id
     *                the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRObject retrieveMCRObject(final MCRObjectID id) throws MCRPersistenceException {
        try {
            Document xml = MCRXMLMetadataManager.getInstance().retrieveXML(id);
            if (xml == null) {
                throw new MCRPersistenceException("Could not retrieve xml of object: " + id);
            }
            return new MCRObject(xml);
        } catch (IOException | JDOMException e) {
            throw new MCRPersistenceException("Could not retrieve xml of object: " + id, e);
        }
    }

    /**
     * Retrieves an instance of {@link MCRExpandedObject} with the given {@link MCRObjectID}
     * @param id the object ID
     * @return the expanded object
     * @throws MCRPersistenceException if a persistence problem is occurred
     */
    public static MCRExpandedObject retrieveMCRExpandedObject(final MCRObjectID id) throws MCRPersistenceException {
        return MCRExpandedObjectManager.getInstance().getExpandedObject(retrieveMCRObject(id));
    }

    /**
     * Retrieves an instance of {@link MCRObject} or {@link MCRDerivate} depending on {@link MCRObjectID#getTypeId()}
     *
     * @param id
     *                derivate or object id
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRBase retrieve(final MCRObjectID id) throws MCRPersistenceException {
        if (id.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
            return retrieveMCRDerivate(id);
        }
        return retrieveMCRObject(id);
    }

    /**
     * Updates the <code>MCRObject</code> or <code>MCRDerivate</code>.
     *
     * @param base the object to update
     *
     * @throws MCRPersistenceException
     *              if a persistence problem is occurred
     * @throws MCRAccessException
     *              if write permission is missing or see {@link #create(MCRObject)}
     */
    public static void update(final MCRBase base)
        throws MCRPersistenceException, MCRAccessException {
        if (base instanceof MCRObject object) {
            update(object);
        } else if (base instanceof MCRDerivate derivate) {
            update(derivate);
        } else {
            throw new IllegalArgumentException("Type is unsupported " + base.getId());
        }
    }

    /**
     * Updates the derivate or creates it if it does not exist yet.
     *
     * @throws MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRAccessException
     *                if write permission to object or derivate is missing
     */
    public static void update(final MCRDerivate mcrDerivate) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID derivateId = mcrDerivate.getId();

        if (isMarkedForDeletion(derivateId)) {
            return;
        }

        if (!exists(derivateId)) {
            create(mcrDerivate);
            return;
        }

        checkUpdatePermission(derivateId);

        Path fileSourceDirectory = handleFileSourceDirectory(mcrDerivate);

        MCRDerivate old = retrieveMCRDerivate(derivateId);

        updateDerivate(mcrDerivate, old);

        updateIFS(fileSourceDirectory, derivateId);

    }

    private static Path handleFileSourceDirectory(MCRDerivate mcrDerivate) {
        MCRMetaIFS internals = mcrDerivate.getDerivate().getInternals();
        if (internals != null && internals.getSourcePath() != null) {
            Path fileSourceDirectory = Paths.get(internals.getSourcePath());
            internals.setSourcePath(null); // MCR-2645
            if (!Files.exists(fileSourceDirectory)) {
                LOGGER.warn("{}: the directory {} was not found.", mcrDerivate::getId, () -> fileSourceDirectory);
                return null;
            }
            return fileSourceDirectory;
        }
        return null;
    }


    private static void updateDerivate(MCRDerivate mcrDerivate, MCRDerivate old) throws MCRPersistenceException {
        Date oldDate = old.getService().getDate(DATE_TYPE_CREATEDATE);
        mcrDerivate.getService().setDate(DATE_TYPE_CREATEDATE, oldDate);
        if (!mcrDerivate.getService().isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
            for (String flagCreatedBy : old.getService().getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
                mcrDerivate.getService().addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, flagCreatedBy);
            }
        }
        updateMCRDerivateXML(mcrDerivate);
    }

    private static void updateIFS(Path fileSourceDirectory, MCRObjectID derivateId) throws MCRPersistenceException {
        if (fileSourceDirectory != null) {
            MCRPath targetPath = MCRPath.getPath(derivateId.toString(), "/");
            try {
                Files.walkFileTree(fileSourceDirectory, new MCRTreeCopier(fileSourceDirectory, targetPath));
            } catch (Exception exc) {
                throw new MCRPersistenceException(
                    "Unable to update IFS. Copy failed from " + fileSourceDirectory.toAbsolutePath()
                        + " to target " + targetPath.toAbsolutePath(),
                    exc);
            }
        }
    }


    /**
     * Updates the object or creates it if it does not exist yet.
     *
     * @throws MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRAccessException
     *                if write permission is missing or see {@link #create(MCRObject)}
     * @return the normalized version of the object or the object itself if no normalization is applied or null if the
     *        object is marked for deletion
     */
    public static MCRObject update(final MCRObject mcrObject) throws MCRPersistenceException, MCRAccessException {
        MCRObject objectCopy = copyObject(mcrObject);

        MCRObjectID id = objectCopy.getId();

        if (isMarkedForDeletion(id)) {
            return null;
        }

        if (!exists(id)) {
            create(mcrObject);
        }

        checkUpdatePermission(id);
        normalizeObject(objectCopy);
        validateObject(objectCopy);

        MCRObject old = retrieveMCRObject(id);

        checkModificationDates(objectCopy, old);
        retainCreatedDateAndFlags(objectCopy, old);

        fireUpdateEvent(objectCopy);
        return objectCopy;
    }

    /**
     * Creates a copy of the given MCRObject.
     * @param mcrObject the object to copy
     * @return a copy of the MCRObject
     */
    private static MCRObject copyObject(MCRObject mcrObject) {
        Document document = mcrObject.createXML();
        Document documentCopy = document.clone();
        MCRObject objectCopy = new MCRObject();
        objectCopy.setFromJDOM(documentCopy);
        return objectCopy;
    }

    /**
     * Normalizes the object. The normalization is done by the normalizers defined in
     * {@link #NORMALIZERS_PROPERTY_PREFIX}
     * @param mcrObject the object to normalize
     */
    public static void normalizeObject(MCRObject mcrObject) {
        List<MCRObjectNormalizer> normalizers =
            instanciateConfigurableList(MCRObjectNormalizer.class, mcrObject, NORMALIZERS_PROPERTY_PREFIX);

        for (MCRObjectNormalizer normalizer : normalizers) {
            normalizer.normalize(mcrObject);
        }
    }

    /**
     *  Validates the object. The validation is done by the validators defined in {@link #VALIDATORS_PROPERTY_PREFIX}
     * @param mcrObject the object to validate
     * @throws MCRPersistenceException if the object is not valid
     */
    public static void validateObject(MCRObject mcrObject) throws MCRPersistenceException {
        List<MCRObjectValidator> validatorList =
            instanciateConfigurableList(MCRObjectValidator.class, mcrObject, VALIDATORS_PROPERTY_PREFIX);

        for (MCRObjectValidator validator : validatorList) {
            MCRValidationResult validate = validator.validate(mcrObject);
            if (!validate.isValid()) {
                throw new MCRPersistenceException(
                    "The object " + mcrObject.getId() + " is not valid: " + validate.getMessage());
            }
        }
    }

    private static <T> List<T> instanciateConfigurableList(Class<T> type, MCRObject mcrObject, String propPrefix) {
        String objectType = mcrObject.getId().getTypeId();
        Map<String, Callable<T>> nameConstructorMap =
            MCRConfiguration2.getInstances(type, propPrefix + DEFAULT_IMPLEMENTATION_KEY + ".");
        nameConstructorMap.putAll(MCRConfiguration2.getInstances(type, propPrefix + objectType + "."));
        return nameConstructorMap.entrySet().stream()
            .map(entry -> {
                String name = entry.getKey();
                Callable<T> constructor = entry.getValue();
                try {
                    return constructor.call();
                } catch (Exception e) {
                    throw new MCRException("Could not initialize " + name, e);
                }
            }).toList();
    }

    private static boolean isMarkedForDeletion(MCRObjectID id) {
        return MCRMarkManager.getInstance().isMarkedForDeletion(id);
    }

    private static void checkUpdatePermission(MCRObjectID id) throws MCRAccessException {
        if (!MCRAccessManager.checkPermission(id, PERMISSION_WRITE)) {
            throw new MCRMissingPermissionException("Update object.", id.toString(), PERMISSION_WRITE);
        }
    }

    private static void checkModificationDates(MCRObject mcrObject, MCRObject old) throws MCRPersistenceException {
        Date diskModifyDate = old.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        Date updateModifyDate = mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        if (!mcrObject.isImportMode() && diskModifyDate != null && updateModifyDate != null
            && updateModifyDate.before(diskModifyDate)) {
            throw new MCRPersistenceException("The object " + mcrObject.getId() + " was modified (" + diskModifyDate
                + ") during the time it was opened in the editor.");
        }
    }

    private static void retainCreatedDateAndFlags(MCRObject mcrObject, MCRObject old) throws MCRPersistenceException {
        boolean importMode = mcrObject.isImportMode();
        if (!importMode || mcrObject.getService().getDate(DATE_TYPE_CREATEDATE) == null) {
            mcrObject.getService().setDate(DATE_TYPE_CREATEDATE, old.getService().getDate(DATE_TYPE_CREATEDATE));
        }
        if (!importMode && !mcrObject.getService().isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
            for (String flagCreatedBy : old.getService().getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
                mcrObject.getService().addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, flagCreatedBy);
            }
        }
    }


    /**
     * Updates only the XML part of the derivate.
     *
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    private static void updateMCRDerivateXML(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        boolean importMode = mcrDerivate.isImportMode();
        if (!importMode || mcrDerivate.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE) == null) {
            mcrDerivate.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        }
        fireEvent(mcrDerivate, retrieveMCRDerivate(mcrDerivate.getId()), MCREvent.EventType.UPDATE);
    }

    private static void restore(final MCRDerivate mcrDerivate, final MCRObjectID mcrObjectId, final byte[] backup) {
        try {
            final MCRObject obj = new MCRObject(backup, false);
            // If an event handler exception occurred, its crucial to restore the object first
            // before updating it again. Otherwise the exception could be thrown again and
            // the object will be in an invalid state (the not existing derivate will be
            // linked with the object).
            MCRXMLMetadataManager.getInstance().update(mcrObjectId, obj.createXML(), new Date());

            // update and call event handlers
            update(obj);
        } catch (final Exception e1) {
            LOGGER.warn("Error while restoring {}", mcrObjectId, e1);
        } finally {
            // remove derivate
            fireEvent(mcrDerivate, null, MCREvent.EventType.DELETE);
        }
    }

    private static void fireEvent(MCRBase base, MCRBase oldBase, MCREvent.EventType eventType) {
        boolean objectEvent = base instanceof MCRObject;
        MCREvent.ObjectType type = objectEvent ? MCREvent.ObjectType.OBJECT : MCREvent.ObjectType.DERIVATE;
        final MCREvent evt = new MCREvent(type, eventType);
        if (objectEvent) {
            evt.put(MCREvent.OBJECT_KEY, base);
        } else {
            evt.put(MCREvent.DERIVATE_KEY, base);
        }
        Optional.ofNullable(oldBase)
            .ifPresent(b -> evt.put(objectEvent ? MCREvent.OBJECT_OLD_KEY : MCREvent.DERIVATE_OLD_KEY, b));
        if (MCREvent.EventType.DELETE == eventType) {
            MCREventManager.getInstance().handleEvent(evt, MCREventManager.BACKWARD);
        } else {
            MCREventManager.getInstance().handleEvent(evt);
        }
    }

    public static List<MCRObjectID> getChildren(MCRObjectID id) {
        return MCRLinkTableManager.getInstance()
            .getSourceOf(id, MCRLinkType.PARENT)
            .stream()
            .map(MCRObjectID::getInstance)
            .toList();
    }

    public static List<MCRObjectID> getDerivateIds(MCRObjectID id) {
        return MCRLinkTableManager.getInstance().getSourceOf(id, MCRLinkType.DERIVATE)
            .stream()
            .map(MCRObjectID::getInstance)
            .toList();
    }

    public static MCRObjectID getObjectId(MCRObjectID derivateID) {
        return MCRLinkTableManager.getInstance()
            .getDestinationOf(derivateID, MCRLinkType.DERIVATE)
            .stream()
            .map(MCRObjectID::getInstance)
            .findFirst()
            .orElseGet(() -> {
                //one expensive process
                if (exists(derivateID)) {
                    LOGGER.warn("inconsistency: Derivate {} has no owner in Database", derivateID);
                    return retrieveMCRDerivate(derivateID).getOwnerID();
                }
                return null;
            });
    }
}
