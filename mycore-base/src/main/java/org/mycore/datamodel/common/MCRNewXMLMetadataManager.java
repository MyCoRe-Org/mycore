/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.datamodel.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRNewMetadataStore;
import org.mycore.datamodel.ifs2.MCROCFLMetadataStore;
import org.mycore.datamodel.ifs2.MCRObjectIDFileSystemDate;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoreCenter;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.history.MCRMetadataHistoryManager;

/**
 * Manages persistence of MCRObject and MCRDerivate xml metadata. Provides
 * methods to create, retrieve, update and delete object metadata using IFS2
 * MCRMetadataStore instances.
 *
 * For configuration, at least the following properties must be set:
 *
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 *
 * Both directories will be created if they do not exist yet. For each project
 * and type, a subdirectory will be created, for example
 * %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 *
 * The default IFS2 store is MCRVersioningMetadataStore, which versions metadata
 * using SVN in local repositories below SVNBase. If you do not want versioning
 * and would like to have better performance, change the following property to
 *
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRMetadataStore
 *
 * It is also possible to change individual properties per project and object
 * type and overwrite the defaults, for example
 *
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 *
 * See documentation of MCRStore and MCRMetadataStore for details.
 *
 * @author Christoph Neidahl (OPNA2608)
 */
public class MCRNewXMLMetadataManager implements MCRXMLMetadataManagerAdapter {

	private static final Logger LOGGER = LogManager.getLogger();

	/** The singleton */
	private static MCRNewXMLMetadataManager SINGLETON;

	private HashSet<String> createdStores;

	/**
	 * The default IFS2 Metadata store class to use, set by
	 * MCR.Metadata.Store.DefaultClass
	 */
	private Class<? extends MCRNewMetadataStore> defaultClass;

	/**
	 * The default subdirectory slot layout for IFS2 metadata store, is 4-2-2 for
	 * 8-digit IDs, that means DocPortal_document_0000001 will be stored in the file
	 * DocPortal/document/0000/00/DocPortal_document_00000001.xml
	 */
	private String defaultLayout;

	/**
	 * The base directory for all IFS2 metadata stores used, set by
	 * MCR.Metadata.Store.BaseDir
	 */
	private Path basePath;

	private Path baseWorkPath;

	protected MCRNewXMLMetadataManager() {
		this.createdStores = new HashSet<>();
		reload();
	}

	/** Returns the singleton */
	public static synchronized MCRNewXMLMetadataManager instance() {
		if (SINGLETON == null) {
			SINGLETON = new MCRNewXMLMetadataManager();
		}
		return SINGLETON;
	}

	public synchronized void reload() {
		String pattern = MCRConfiguration2.getString("MCR.Metadata.ObjectID.NumberPattern").orElse("0000000000");
		defaultLayout = pattern.length() - 4 + "-2-2";

		String base = MCRConfiguration2.getStringOrThrow("MCR.Metadata.Store.BaseDir");
		basePath = Paths.get(base);
		checkPath(basePath, "base");

		defaultClass = MCRConfiguration2.<MCRNewMetadataStore>getClass("MCR.Metadata.Store.DefaultClass")
				.orElse(MCROCFLMetadataStore.class);

		closeCreatedStores();
	}

	private synchronized void closeCreatedStores() {
		for (String storeId : createdStores) {
			MCRStoreCenter.instance().removeStore(storeId);
		}
		createdStores.clear();
	}

	/**
	 * Checks the directory configured exists and is readable and writable, or
	 * creates it if it does not exist yet.
	 *
	 * @param path the path to check
	 * @param type metadata store type
	 */
	private void checkPath(Path path, String type) {
		if (!Files.exists(path)) {
			try {
				if (!Files.exists(Files.createDirectories(path))) {
					throw new MCRConfigurationException(
							"The metadata store " + type + " directory " + path.toAbsolutePath() + " does not exist.");
				}
			} catch (Exception ex) {
				String msg = "Exception while creating metadata store " + type + " directory " + path.toAbsolutePath();
				throw new MCRConfigurationException(msg, ex);
			}
		} else {
			if (!Files.isDirectory(path)) {
				throw new MCRConfigurationException(
						"Metadata store " + type + " " + path.toAbsolutePath() + " is a file, not a directory");
			}
			if (!Files.isReadable(path)) {
				throw new MCRConfigurationException(
						"Metadata store " + type + " directory " + path.toAbsolutePath() + " is not readable");
			}
			if (!Files.isWritable(path)) {
				throw new MCRConfigurationException(
						"Metadata store " + type + " directory " + path.toAbsolutePath() + " is not writeable");
			}
		}
	}

	/**
	 * Returns IFS2 MCRMetadataStore for the given MCRObjectID base, which is
	 * {project}_{type}
	 *
	 * @param base the MCRObjectID base, e.g. DocPortal_document
	 */
	private MCRMetadataStore getStore(String base) {
		String[] split = base.split("_");
		return getStore(split[0], split[1], false);
	}

	/**
	 * Returns IFS2 MCRMetadataStore for the given MCRObjectID base, which is
	 * {project}_{type}
	 *
	 * @param base     the MCRObjectID base, e.g. DocPortal_document
	 * @param readOnly If readOnly, the store will not be created if it does not
	 *                 exist yet. Instead an exception is thrown.
	 * @return the metadata store
	 */
	private MCRMetadataStore getStore(String base, boolean readOnly) {
		String[] split = base.split("_");
		return getStore(split[0], split[1], readOnly);
	}

	/**
	 * Returns IFS2 MCRMetadataStore used to store metadata of the given MCRObjectID
	 *
	 * @param mcrid    the mycore object identifier
	 * @param readOnly If readOnly, the store will not be created if it does not
	 *                 exist yet. Instead an exception is thrown.
	 * @return the metadata store
	 */
	private MCRMetadataStore getStore(MCRObjectID mcrid, boolean readOnly) {
		return getStore(mcrid.getProjectId(), mcrid.getTypeId(), readOnly);
	}

	/**
	 * Returns IFS2 MCRMetadataStore for the given project and object type
	 *
	 * @param project  the project, e.g. DocPortal
	 * @param type     the object type, e.g. document
	 * @param readOnly if readOnly, this method will throw an exception if the store
	 *                 does not exist's yet
	 * @return the metadata store
	 */
	private MCRMetadataStore getStore(String project, String type, boolean readOnly) {
		String projectType = getStoryKey(project, type);
		String prefix = "MCR.IFS2.Store." + projectType + ".";
		String forceXML = MCRConfiguration2.getString(prefix + "ForceXML").orElse(null);
		if (forceXML == null) {
			synchronized (this) {
				forceXML = MCRConfiguration2.getString(prefix + "ForceXML").orElse(null);
				if (forceXML == null) {
					try {
						setupStore(project, type, prefix, readOnly);
					} catch (ReflectiveOperationException e) {
						throw new MCRPersistenceException(
								new MessageFormat("Could not instantiate store for project {0} and object type {1}.",
										Locale.ROOT).format(new Object[] { project, type }),
								e);
					}
				}
			}
		}
		MCRMetadataStore store = MCRStoreManager.getStore(projectType);
		if (store == null) {
			throw new MCRPersistenceException(
					new MessageFormat("Metadata store for project {0} and object type {1} is unconfigured.",
							Locale.ROOT).format(new Object[] { project, type }));
		}
		return store;
	}

	public void verifyStore(String base) {
		MCRMetadataStore store = getStore(base);
		if (store instanceof MCRVersioningMetadataStore) {
			LOGGER.info("Verifying SVN history of {}.", base);
			((MCRVersioningMetadataStore) (getStore(base))).verify();
		} else {
			LOGGER.warn("Cannot verify unversioned store {}!", base);
		}
	}

	@SuppressWarnings("unchecked")
	private void setupStore(String project, String objectType, String configPrefix, boolean readOnly)
			throws ReflectiveOperationException {
		String baseID = getStoryKey(project, objectType);
		Class<? extends MCRNewMetadataStore> clazz = MCRConfiguration2.<MCRNewMetadataStore>getClass(configPrefix + "Class").orElseGet(() -> {
			MCRConfiguration2.set(configPrefix + "Class", defaultClass.getName());
			return defaultClass;
		});

		Path typePath = basePath.resolve(project).resolve(objectType);
		checkAndCreateDirectory(typePath, project, objectType, configPrefix, readOnly);

		String slotLayout = MCRConfiguration2.getString(configPrefix + "SlotLayout").orElse(null);
		if (slotLayout == null) {
			MCRConfiguration2.set(configPrefix + "SlotLayout", defaultLayout);
		}
		MCRConfiguration2.set(configPrefix + "BaseDir", typePath.toAbsolutePath().toString());
		MCRConfiguration2.set(configPrefix + "ForceXML", String.valueOf(true));
		String value = "derivate".equals(objectType) ? "mycorederivate" : "mycoreobject";
		MCRConfiguration2.set(configPrefix + "ForceDocType", value);
		createdStores.add(baseID);
		MCRStoreManager.createStore(baseID, clazz);
	}

	private void checkAndCreateDirectory(Path path, String project, String objectType, String configPrefix,
			boolean readOnly) {
		if (Files.exists(path)) {
			return;
		}
		if (readOnly) {
			throw new MCRPersistenceException(String.format(Locale.ENGLISH,
					"Path does not exists ''%s'' to set up store for project ''%s'' and objectType ''%s'' "
							+ "and config prefix ''%s''. We are not willing to create it for an read only operation.",
					path.toAbsolutePath(), project, objectType, configPrefix));
		}
		try {
			if (!Files.exists(Files.createDirectories(path))) {
				throw new FileNotFoundException(path.toAbsolutePath() + " does not exists.");
			}
		} catch (Exception e) {
			throw new MCRPersistenceException(
					String.format(Locale.ENGLISH,
							"Couldn'e create directory ''%s'' to set up store for project ''%s'' and objectType ''%s'' "
									+ "and config prefix ''%s''",
							path.toAbsolutePath(), project, objectType, configPrefix));
		}
	}

	private String getStoryKey(String project, String objectType) {
		return project + "_" + objectType;
	}

	public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
		try {
			MCRStoredMetadata sm = getStore(mcrid, false).create(xml, mcrid.getNumberAsInteger());
			sm.setLastModified(lastModified);
			MCRConfigurationBase.systemModified();
		} catch (Exception exc) {
			throw new MCRPersistenceException("Error while storing object: " + mcrid, exc);
		}
	}

	public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
		try {
			getStore(mcrid, true).delete(mcrid.getNumberAsInteger());
			MCRConfigurationBase.systemModified();
		} catch (Exception exc) {
			throw new MCRPersistenceException("Error while deleting object: " + mcrid, exc);
		}
	}

	public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
		if (!exists(mcrid)) {
			throw new MCRPersistenceException("Object to update does not exist: " + mcrid);
		}
		try {
			MCRStoredMetadata sm = getStore(mcrid, false).retrieve(mcrid.getNumberAsInteger());
			sm.update(xml);
			sm.setLastModified(lastModified);
			MCRConfigurationBase.systemModified();
		} catch (Exception exc) {
			throw new MCRPersistenceException("Unable to update object " + mcrid, exc);
		}
	}

	public MCRContent retrieveContent(MCRObjectID mcrid) throws IOException {
		MCRContent metadata;
		MCRStoredMetadata storedMetadata = retrieveStoredMetadata(mcrid);
		if (storedMetadata == null || storedMetadata.isDeleted()) {
			return null;
		}
		metadata = storedMetadata.getMetadata();
		return metadata;
	}

	public MCRContent retrieveContent(MCRObjectID mcrid, long revision) throws IOException {
		LOGGER.info("Getting object {} in revision {}", mcrid, revision);
		MCRMetadataVersion version = getMetadataVersion(mcrid, revision);
		if (version != null) {
			return version.retrieve();
		}
		return null;
	}

	/**
	 * Returns the {@link MCRMetadataVersion} of the given id and revision.
	 *
	 * @param mcrId the id of the object to be retrieved
	 * @param rev   the revision to be returned, specify -1 if you want to retrieve
	 *              the latest revision (includes deleted objects also)
	 * @return a {@link MCRMetadataVersion} representing the {@link MCRObject} of
	 *         the given revision or <code>null</code> if there is no such object
	 *         with the given revision
	 * @throws IOException version metadata couldn't be retrieved due an i/o error
	 */
	private MCRMetadataVersion getMetadataVersion(MCRObjectID mcrId, long rev) throws IOException {
		MCRVersionedMetadata versionedMetaData = getVersionedMetaData(mcrId);
		if (versionedMetaData == null) {
			return null;
		}
		return versionedMetaData.getRevision(rev);
	}

	public List<MCRMetadataVersion> listRevisions(MCRObjectID id) throws IOException {
		MCRVersionedMetadata vm = getVersionedMetaData(id);
		if (vm == null) {
			return null;
		}
		return vm.listVersions();
	}

	private MCRVersionedMetadata getVersionedMetaData(MCRObjectID id) throws IOException {
		if (id == null) {
			return null;
		}
		MCRMetadataStore metadataStore = getStore(id, true);
		if (!(metadataStore instanceof MCRVersioningMetadataStore)) {
			return null;
		}
		MCRVersioningMetadataStore verStore = (MCRVersioningMetadataStore) metadataStore;
		return verStore.retrieve(id.getNumberAsInteger());
	}

	/**
	 * Retrieves stored metadata xml as IFS2 metadata object.
	 *
	 * @param mcrid the MCRObjectID
	 */
	private MCRStoredMetadata retrieveStoredMetadata(MCRObjectID mcrid) throws IOException {
		return getStore(mcrid, true).retrieve(mcrid.getNumberAsInteger());
	}

	public int getHighestStoredID(String project, String type) {
		MCRMetadataStore store;
		try {
			store = getStore(project, type, true);
		} catch (MCRPersistenceException persistenceException) {
			// store does not exists -> return 0
			return 0;
		}
		int highestStoredID = store.getHighestStoredID();
		// fixes MCR-1534 (IDs once deleted should never be used again)
		return Math.max(highestStoredID, MCRMetadataHistoryManager.getHighestStoredID(project, type)
				.map(MCRObjectID::getNumberAsInteger).orElse(0));
	}

	public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
		try {
			if (mcrid == null) {
				return false;
			}
			MCRMetadataStore store;
			try {
				store = getStore(mcrid, true);
			} catch (MCRPersistenceException persistenceException) {
				// the store couldn't be retrieved, the object does not exists
				return false;
			}
			return store.exists(mcrid.getNumberAsInteger());
		} catch (Exception exc) {
			throw new MCRPersistenceException("Unable to check if object exists " + mcrid, exc);
		}
	}

	public List<String> listIDsForBase(String base) {
		MCRMetadataStore store;
		try {
			store = getStore(base, true);
		} catch (MCRPersistenceException e) {
			LOGGER.warn("Store for '{}' does not exist.", base);
			return Collections.emptyList();
		}

		List<String> list = new ArrayList<>();
		Iterator<Integer> it = store.listIDs(MCRStore.ASCENDING);
		String[] idParts = MCRObjectID.getIDParts(base);
		while (it.hasNext()) {
			list.add(MCRObjectID.formatID(idParts[0], idParts[1], it.next()));
		}
		return list;
	}

	public List<String> listIDsOfType(String type) {
		try (Stream<Path> streamBasePath = list(basePath)) {
			return streamBasePath.flatMap(projectPath -> {
				final String project = projectPath.getFileName().toString();
				return list(projectPath).flatMap(typePath -> {
					if (type.equals(typePath.getFileName().toString())) {
						final String base = getStoryKey(project, type);
						return listIDsForBase(base).stream();
					}
					return Stream.empty();
				});
			}).collect(Collectors.toList());
		}
	}

	public List<String> listIDs() {
		try (Stream<Path> streamBasePath = list(basePath)) {
			return streamBasePath.flatMap(projectPath -> {
				final String project = projectPath.getFileName().toString();
				return list(projectPath).flatMap(typePath -> {
					final String type = typePath.getFileName().toString();
					final String base = getStoryKey(project, type);
					return listIDsForBase(base).stream();
				});
			}).collect(Collectors.toList());
		}
	}

	public Collection<String> getObjectTypes() {
		try (Stream<Path> streamBasePath = list(basePath)) {
			return streamBasePath.flatMap(this::list).map(Path::getFileName).map(Path::toString)
					.filter(MCRObjectID::isValidType).distinct().collect(Collectors.toSet());
		}
	}

	public Collection<String> getObjectBaseIds() {
		try (Stream<Path> streamBasePath = list(basePath)) {
			return streamBasePath.flatMap(this::list).filter(p -> MCRObjectID.isValidType(p.getFileName().toString()))
					.map(p -> p.getParent().getFileName() + "_" + p.getFileName()).collect(Collectors.toSet());
		}
	}

	/**
	 * Returns the entries of the given path. Throws a MCRException if an
	 * I/O-Exceptions occur.
	 *
	 * @return stream of project directories
	 */
	private Stream<Path> list(Path path) {
		try {
			return Files.list(path);
		} catch (IOException ioException) {
			throw new MCRPersistenceException(
					"unable to list files of IFS2 metadata directory " + path.toAbsolutePath(), ioException);
		}
	}

	public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException {
		List<MCRObjectIDDate> objidlist = new ArrayList<>(ids.size());
		for (String id : ids) {
			MCRStoredMetadata sm = this.retrieveStoredMetadata(MCRObjectID.getInstance(id));
			objidlist.add(new MCRObjectIDFileSystemDate(sm, id));
		}
		return objidlist;
	}

	public long getLastModified(MCRObjectID id) throws IOException {
		MCRMetadataStore store = getStore(id, true);
		MCRStoredMetadata metadata = store.retrieve(id.getNumberAsInteger());
		if (metadata != null) {
			return metadata.getLastModified().getTime();
		}
		return -1;
	}

	public MCRCache.ModifiedHandle getLastModifiedHandle(final MCRObjectID id, final long expire, TimeUnit unit) {
		return new StoreModifiedHandle(this, id, expire, unit);
	}

	private static final class StoreModifiedHandle implements MCRCache.ModifiedHandle {
		private final MCRNewXMLMetadataManager mm;

		private final long expire;

		private final MCRObjectID id;

		private StoreModifiedHandle(MCRNewXMLMetadataManager mm, MCRObjectID id, long time, TimeUnit unit) {
			this.mm = mm;
			this.expire = unit.toMillis(time);
			this.id = id;
		}

		@Override
		public long getCheckPeriod() {
			return expire;
		}

		@Override
		public long getLastModified() throws IOException {
			return mm.getLastModified(id);
		}
	}
}
