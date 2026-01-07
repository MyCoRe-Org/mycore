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

package org.mycore.migration.objectversion;

import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.migration.strategy.MCRChildrenOrderMigrationStrategy;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * Migrates a versioning store to a new format.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRVersioningMetadataStoreMigrator {

    private static final Logger LOGGER = LogManager.getLogger();
    private final MCRVersioningMetadataStore versioningStore;
    private final MCRChildrenOrderMigrationStrategy strategy;

    public MCRVersioningMetadataStoreMigrator(MCRVersioningMetadataStore versioningStore) {
        this.versioningStore = versioningStore;
        this.strategy = MCRObjectMigratorHelper.getChildrenOrderMigrationStrategy();
    }

    /**
     * Migrate the versioning store to the new format.
     *
     * @throws SVNException if an SVN error occurs during migration
     * @throws IOException if an I/O error occurs during migration
     * @throws JDOMException if a JDOM error occurs during XML processing
     */
    public void migrate() throws SVNException, IOException, JDOMException {
        Map<String, String> subPropertiesMap = MCRConfiguration2.getSubPropertiesMap(
            "MCR.IFS2.Store." + versioningStore.getID() + ".");
        LOGGER.info(subPropertiesMap);
        String svnUrl = subPropertiesMap.get("SVNRepositoryURL");
        MCRMigrationStore newStore =
            MCRVersioningMetadataStoreMigratorHelper.getMigrationStore(versioningStore.getID(), subPropertiesMap);
        if (newStore == null) {
            return;
        }
        SVNURL repURL;
        URI repositoryURI = URI.create(svnUrl);
        repURL = SVNURL.create(repositoryURI.getScheme(), repositoryURI.getUserInfo(), repositoryURI.getHost(),
            repositoryURI.getPort(), repositoryURI.getPath(), true);
        LOGGER.info("repURL: {}", repURL);
        SVNRepository repository = SVNRepositoryFactory.create(repURL);
        SVNRepository fileRepository = SVNRepositoryFactory.create(repURL);
        SVNRepository newRepository = MCRVersioningMetadataStoreMigratorHelper.initializeSVNRepository(
            MCRVersioningMetadataStoreMigratorHelper.toMigrationPath(svnUrl));

        long latestRevision = repository.getLatestRevision();
        int highestStoredID = versioningStore.getHighestStoredID();
        PathDateModifiedUpdate[] pathUpdates = new PathDateModifiedUpdate[highestStoredID + 1];
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            repository.log(new String[] { "" }, 0, latestRevision, true, false, logEntry -> {
                processRevision(logEntry, latestRevision, newStore, pathUpdates, fileRepository, newRepository,
                    executorService);
            });
        } catch (MCRException ignoredForKnownCause) {
            switch (ignoredForKnownCause.getCause()) {
                case SVNException se -> throw se;
                case IOException ioe -> throw ioe;
                case JDOMException je -> throw je;
                default -> throw ignoredForKnownCause;
            }
        }

        MCRVersioningMetadataStoreMigratorHelper.setLastModifiedDates(pathUpdates);
        //migration done, now switch store paths
        replaceStoreDirs(newStore, repository, newRepository);
        repository.closeSession();
        fileRepository.closeSession();
        newRepository.closeSession();
    }

    /**
     * Process a single revision of the SVN repository.
     *
     * @param logEntry the SVN log entry representing the revision
     * @param latestRevision the latest revision number in the repository
     * @param newStore the new migration store to update
     * @param pathUpdates an array to track path updates and their modification dates
     * @param fileRepository the SVN repository for file access
     * @param newRepository the new SVN repository for committing changes
     * @param executorService the executor service for concurrent tasks
     */
    private void processRevision(SVNLogEntry logEntry, long latestRevision, MCRMigrationStore newStore,
        PathDateModifiedUpdate[] pathUpdates, SVNRepository fileRepository, SVNRepository newRepository,
        ExecutorService executorService) throws SVNException {
        long revision = logEntry.getRevision();
        LOGGER.info("Processing revision: {}/{}", revision, latestRevision);
        SVNProperties revisionProperties = logEntry.getRevisionProperties();
        if (logEntry.getRevision() == 0) {
            SVNPropertyValue date = logEntry.getRevisionProperties()
                .getSVNPropertyValue(SVNRevisionProperty.DATE);
            MCRVersioningMetadataStoreMigratorHelper.updateRevisionDate(newRepository, 0, date);
        }
        for (Map.Entry<String, SVNLogEntryPath> entry : logEntry.getChangedPaths().entrySet()) {
            String path = entry.getKey();
            SVNLogEntryPath logEntryPath = entry.getValue();
            LOGGER.debug(logEntryPath);
            if (!path.endsWith(".xml") || !logEntryPath.getKind().equals(SVNNodeKind.FILE)) {
                LOGGER.debug("Not an xml file: {}", path);
                continue;
            }
            try {
                if (logEntryPath.getType() == 'D') {
                    try {
                        MCRVersioningMetadataStoreMigratorHelper.deleteFile(newStore, pathUpdates, path);
                        MCRVersioningMetadataStoreMigratorHelper.deleteFile(newRepository, path, revisionProperties,
                            executorService);
                    } catch (NoSuchFileException e) {
                        LOGGER.warn("File to delete not found in new store, ignoring: {}, id: {}", () -> path,
                            () -> MCRVersioningMetadataStoreMigratorHelper.getId(path));
                    }
                    continue;
                }
                try (MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream()) {
                    fileRepository.getFile(path, revision, null, baos);
                    MCRContent content =
                        new MCRByteContent(baos.getBuffer(), 0, baos.size(), logEntry.getDate().getTime());
                    MCRContent migratedContent = migrateXML(content);
                    MCRByteContent migratedContentBytes = new MCRByteContent(migratedContent.asByteArray());
                    int id = MCRVersioningMetadataStoreMigratorHelper.getId(path);
                    MCRStoredMetadata storedMetadata = newStore.retrieve(id);
                    if (storedMetadata != null) {
                        MCRContent existingContent = storedMetadata.getMetadata();
                        if (MCRVersioningMetadataStoreMigratorHelper.objectsAreSemanticalEqual(existingContent.asXML(),
                            migratedContent.asXML())) {
                            LOGGER.info("File content unchanged, not updating, id: {}", id);
                            continue;
                        }
                        storedMetadata.update(migratedContentBytes);
                    } else {
                        newStore.create(migratedContentBytes, id);
                    }
                    LOGGER.debug("File {} content size changed from {} to {} bytes, id: {}", () -> path, baos::size,
                        migratedContentBytes::length, () -> id);
                    Path localPath = newStore.getSlot(id);
                    pathUpdates[id] = new PathDateModifiedUpdate(localPath, logEntry.getDate());
                    String[] paths =
                        MCRVersioningMetadataStoreMigratorHelper.getPaths(newStore.getBaseDirectory(), localPath);
                    MCRVersioningMetadataStoreMigratorHelper.commit(newRepository, paths, migratedContentBytes,
                        revisionProperties, executorService);
                }
            } catch (IOException | JDOMException e) {
                throw new MCRException(e);
            }
        }
    }

    /**
     * Replace the old store directories with the new store directories after migration.
     *
     * @param newStore the new migration store
     * @param repository the original SVN repository
     * @param newRepository the new SVN repository
     * @throws IOException if an I/O error occurs during directory replacement
     */
    private void replaceStoreDirs(MCRMigrationStore newStore, SVNRepository repository, SVNRepository newRepository)
        throws IOException {
        Path oldBaseDir = versioningStore.getBaseDirectory();
        Path newBaseDir = newStore.getBaseDirectory();
        Path backUpDir = oldBaseDir.getParent().resolve(oldBaseDir.getFileName() + ".backup");
        Path oldSvnDir = Path.of(URI.create(repository.getLocation().toString()));
        Path newSvnDir = Path.of(URI.create(newRepository.getLocation().toString()));
        Path backUpSvnDir = oldSvnDir.getParent().resolve(oldSvnDir.getFileName() + ".backup");
        MCRVersioningMetadataStoreMigratorHelper.replaceDirectories(oldBaseDir, backUpDir, newBaseDir);
        MCRVersioningMetadataStoreMigratorHelper.replaceDirectories(oldSvnDir, backUpSvnDir, newSvnDir);
        LOGGER.info("Migration of store {} done", versioningStore::getID);
        LOGGER.info("Old store data moved to {}", backUpDir);
        LOGGER.info("Old store SVN data moved to {}", backUpSvnDir);
    }

    /**
     * Migrate the XML content of an old versioned object to the new format.
     *
     * @param oldVersionedXML the old versioned XML content
     * @return the migrated XML content
     * @throws IOException if an I/O error occurs during migration
     * @throws JDOMException if a JDOM error occurs during XML processing
     */
    private MCRContent migrateXML(MCRContent oldVersionedXML) throws IOException, JDOMException {
        Document oldDocument = oldVersionedXML.asXML();
        MCRObjectID objectID = MCRObjectID.getInstance(oldDocument.getRootElement().getAttributeValue("ID"));
        Document newDocument = MCRObjectMigratorHelper.migrateObject(objectID, oldDocument, strategy, false, false);
        return new MCRJDOMContent(newDocument);
    }

}
