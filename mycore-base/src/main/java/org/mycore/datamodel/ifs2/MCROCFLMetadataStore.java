package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.exception.OverwriteException;
import edu.wisc.library.ocfl.api.model.ObjectDetails;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.OcflObjectVersion;
import edu.wisc.library.ocfl.api.model.VersionDetails;
import edu.wisc.library.ocfl.api.model.VersionInfo;
import edu.wisc.library.ocfl.api.model.VersionNum;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;

/**
 * OCFL store.
 * 
 * @author Christoph Neidahl (OPNA2608)
 *
 */
public class MCROCFLMetadataStore extends MCRNewMetadataStore {

    protected boolean hasBaseDir = true;

    protected boolean hasWorkDir = true;

    protected boolean hasSlotLayout = false;

    // TODO not yet supported
    protected boolean canVerify = false;

    private OcflRepository repo;

    private static final String MESSAGE_CREATED = "Created";

    private static final String MESSAGE_UPDATED = "Updated";

    private static final String MESSAGE_DELETED = "Deleted";

    private static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCRNewMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCRNewMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCRNewMetadataVersion.DELETED)));

    private static final char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    @Override
    protected void init(MCRStoreConfig storeConfig) {
        super.init(storeConfig);
        repo = new OcflRepositoryBuilder().defaultLayoutConfig(new HashedNTupleIdEncapsulationLayoutConfig())
            .fileSystemStorage(storage -> storage.repositoryRoot(baseDirectory)).workDir(workDirectory).build();
    }

    @Override
    public boolean exists(MCRNewMetadata metadata) throws MCRPersistenceException {
        return repo.containsObject(metadata.getFullID().toString());
    }

    @Override
    protected void createContent(MCRNewMetadata metadata, MCRContent content)
        throws MCRPersistenceException, MCRUsageException {
        String objName = metadata.getFullID().toString();
        VersionInfo info = new VersionInfo().setMessage(MESSAGE_CREATED);
        Date targetDate = metadata.getDate();
        if (targetDate != null) {
            info.setCreated(targetDate.toInstant().atOffset(ZoneOffset.UTC));
        }
        try (InputStream objectAsStream = forceContent(content).getInputStream()) {
            repo.updateObject(ObjectVersionId.head(objName), info, init -> {
                init.writeFile(objectAsStream, objName + this.suffix);
            });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to create object '" + objName + "'", e);
        }

    }

    @Override
    protected MCRContent readContent(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException {
        String objName = metadata.getFullID().toString();
        OcflObjectVersion storeObject;
        try {
            storeObject = this.repo.getObject(ObjectVersionId.version(objName, metadata.getRevision()));
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }
        if (convertMessageToType(
            getStoredOcflVersion(metadata).getVersionInfo().getMessage()) == MCRNewMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }
        try (InputStream storedContentStream = storeObject.getFile(objName + this.suffix).getStream()) {
            return forceContent(new MCRStreamContent(storedContentStream));
        } catch (IOException e) {
            throw new MCRPersistenceException("Object '" + objName + "' could not be read", e);
        }
    }

    @Override
    protected void updateContent(MCRNewMetadata metadata, MCRContent content)
        throws MCRPersistenceException, MCRUsageException {
        String objName = metadata.getFullID().toString();
        if (!exists(metadata)) {
            throw new MCRUsageException("Cannot update nonexistent object '" + objName + "'");
        }
        try (InputStream objectAsStream = forceContent(content).getInputStream()) {
            repo.updateObject(ObjectVersionId.head(objName), new VersionInfo().setMessage("Updated"), init -> {
                init.writeFile(objectAsStream, objName + this.suffix, OcflOption.OVERWRITE);
            });
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        }
    }

    @Override
    protected void deleteContent(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException {
        String objName = metadata.getFullID().toString();
        if (!exists(metadata)) {
            throw new MCRUsageException("Cannot delete nonexistent object '" + objName + "'");
        }
        VersionInfo headVersion = repo.describeObject(objName).getHeadVersion().getVersionInfo();
        char versionType = convertMessageToType(headVersion.getMessage());
        if (versionType == MCRNewMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot delete already deleted object '" + objName + "'");
        }
        repo.updateObject(ObjectVersionId.head(objName), new VersionInfo().setMessage("Deleted"), init -> {
            init.removeFile(objName + this.suffix);
        });
    }

    @Override
    public IntStream getStoredIDs() throws MCRPersistenceException {
        return repo.listObjectIds().mapToInt((fullId) -> {
            return Integer.parseInt(fullId.substring(this.getID().length() + 1));
        }).sorted();
    }

    private VersionDetails getStoredOcflVersion(MCRNewMetadata metadata) throws MCRUsageException {
        String objName = metadata.getFullID().toString();
        if (metadata.getRevision() == null) {
            throw new MCRUsageException(
                "Cannot get version details without specifying a revision of object '" + objName + "'");
        }
        ObjectDetails storedObject;
        try {
            storedObject = repo.describeObject(objName);
        } catch (NotFoundException e) {
            throw new MCRUsageException("Cannot get version details of nonexistent object '" + objName + "'", e);
        }
        VersionDetails storedVersion = storedObject
            .getVersion(new VersionNum(Long.parseLong(metadata.getRevision().substring(1))));
        if (storedVersion == null) {
            throw new MCRUsageException("Cannot get details of nonexistent version '" + metadata.getRevision()
                + "' for object '" + objName + "'");
        }
        return storedVersion;
    }

    @Override
    public MCRNewMetadataVersion getVersion(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException {
        VersionInfo ocflDetails = getStoredOcflVersion(metadata).getVersionInfo();
        // TODO property identify type from version message instead of assuming
        // MCRNewMetadataVersion.UPDATED
        return new MCRNewMetadataVersion(metadata, metadata.getRevision(),
            ocflDetails.getUser().getName(),
            Date.from(ocflDetails.getCreated().toInstant()), convertMessageToType(ocflDetails.getMessage()));
    }

    @Override
    public Stream<MCRNewMetadataVersion> getVersions(MCRNewMetadata metadata)
        throws MCRPersistenceException, MCRUsageException {
        repo.describeObject(metadata.getFullID().toString()).getVersionMap();
        // TODO implement
        return null;
    }

    @Override
    protected void doVerification() throws MCRPersistenceException {
        // TODO not yet supported, shouldn't get called in the first place though
        throw new MCRPersistenceException("Not implemented yet");
    }

    @Override
    public Date getModified(MCRNewMetadata metadata) throws MCRPersistenceException {
        VersionDetails ocflDetails = getStoredOcflVersion(metadata);
        return Date.from(ocflDetails.getCreated().toInstant());
    }

    @Override
    public void setModified(MCRNewMetadata metadata, Date date) throws MCRPersistenceException, MCRUsageException {
        throw new MCRUsageException("Cannot be implemented(?)");
    }

    @Override
    public boolean isEmpty() {
        return this.repo.listObjectIds().count() > 0;
    }

    @Override
    public int getHighestStoredID() throws MCRPersistenceException {
        // TODO implement
        return 0;
    }

    @Override
    public Iterator<Integer> listIDs(boolean order) throws MCRPersistenceException {
        // TODO implement
        return null;
    }

}
