package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.exception.OverwriteException;
import edu.wisc.library.ocfl.api.model.ObjectDetails;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.OcflObjectVersionFile;
import edu.wisc.library.ocfl.api.model.VersionDetails;
import edu.wisc.library.ocfl.api.model.VersionInfo;
import edu.wisc.library.ocfl.api.model.VersionNum;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;

public class MCROCFLMetadataStore extends MCRNewMetadataStore {

	protected boolean hasBaseDir = true;

	protected boolean hasWorkDir = true;

	protected boolean hasSlotLayout = false;

	// TODO not yet supported
	protected boolean canVerify = false;

	private final OcflRepository repo;

	protected MCROCFLMetadataStore(MCRStoreConfig storeConfig) {
		super(storeConfig);
		repo = new OcflRepositoryBuilder().defaultLayoutConfig(new HashedNTupleIdEncapsulationLayoutConfig())
				.fileSystemStorage(storage -> storage.repositoryRoot(baseDir)).workDir(workDir).build();
	}

	@Override
	public boolean exists(MCRNewMetadata metadata) throws MCRPersistenceException {
		return repo.containsObject(metadata.getFullID().toString());
	}

	@Override
	protected void createContent(MCRNewMetadata metadata, MCRContent content)
			throws MCRPersistenceException, MCRUsageException {
		String objName = metadata.getFullID().toString();
		try (InputStream objectAsStream = forceContent(content).getInputStream()) {
			repo.updateObject(ObjectVersionId.head(objName), new VersionInfo().setMessage("Created"), init -> {
				init.writeFile(objectAsStream, objName + this.suffix);
			});
		} catch (IOException | OverwriteException e) {
			throw new MCRPersistenceException("Failed to create object '" + objName + "'", e);
		}

	}

	@Override
	protected MCRContent readContent(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException {
		String objName = metadata.getFullID().toString();
		OcflObjectVersionFile storeObject;
		try {
			storeObject = this.repo.getObject(ObjectVersionId.version(objName, metadata.getRevision()))
					.getFile(objName + this.suffix);
		} catch (NotFoundException e) {
			throw new MCRUsageException("Object '" + objName + "' could not be found", e);
		}
		try (InputStream storedContentStream = storeObject.getStream()) {
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
		if (!repo.describeObject(objName).getHeadVersion().containsFile(objName + this.suffix)) {
			// object exists but the metadata file's usual path doesn't
			// assume object was created & deleted
			// FIXME replace by checking the version info message instead
			throw new MCRUsageException("Cannot delete already deleted object '" + objName + "'");
		}
		repo.updateObject(ObjectVersionId.head(objName), new VersionInfo().setMessage("Deleted"), init -> {
			init.removeFile(objName + this.suffix);
		});
	}

	@Override
	public IntStream getIDs() throws MCRPersistenceException {
		return repo.listObjectIds().mapToInt((fullId) -> {
			return Integer.parseInt(fullId.substring(this.getStoreID().length() + 1));
		}).sorted();
	}

	@Override
	public MCRNewMetadataVersion getVersion(MCRNewMetadata metadata) throws MCRPersistenceException, MCRUsageException {
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
		// TODO property identify type from version message instead of assuming
		// MCRNewMetadataVersion.UPDATED
		return new MCRNewMetadataVersion(metadata, metadata.getRevision(), "system",
				Date.from(storedVersion.getCreated().toInstant()), MCRNewMetadataVersion.UPDATED);
	}

	@Override
	public Stream<MCRNewMetadataVersion> getVersions(MCRNewMetadata metadata)
			throws MCRPersistenceException, MCRUsageException {
		repo.describeObject(metadata.getFullID().toString()).getVersionMap();
		// FIXME
		return null;
	}

	@Override
	protected void doVerification() throws MCRPersistenceException {
		throw new MCRPersistenceException("Not implemented yet");
	}

	@Override
	public Date getModified(MCRNewMetadata metadata) throws MCRPersistenceException {
		// FIXME deduplicate this shit
		String objName = metadata.getFullID().toString();
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
		return Date.from(storedVersion.getCreated().toInstant());
	}

	@Override
	public void setModified(MCRNewMetadata metadata, Date date) throws MCRPersistenceException, MCRUsageException {
		throw new MCRUsageException("Cannot be implemented(?)");
	}

}
