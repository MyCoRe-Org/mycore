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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class MCRVersioningMetadataStoreMigratorHelper {
    private static final Pattern LAST_PATH_SEGMENT = Pattern.compile("([^\\\\/]+)([\\\\/]?)$");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern INT_ID_PATTERN = Pattern.compile("(\\d+)(?=\\.xml$)");
    private static final LoadingCache<String, ISVNAuthenticationManager> AUTHENTICATION_MANAGER_CACHE =
        CacheBuilder.<String, ISVNAuthenticationManager>newBuilder()
            .build(new CacheLoader<>() {

                @Override
                public ISVNAuthenticationManager load(String user) throws Exception {
                    SVNAuthentication[] auth = { SVNUserNameAuthentication.newInstance(user, false, null, false) };
                    return new BasicAuthenticationManager(auth);
                }
            });
    private static final String MIGRATION_STORE_SUFFIX = ".new";

    /**
     * Update the last modified dates of the given paths to the given dates.
     * Errors are logged but do not stop the process.
     *
     * @param pathUpdates array of path and date pairs to update
     */
    static void setLastModifiedDates(PathDateModifiedUpdate[] pathUpdates) {
        LOGGER.info("Updating last modified dates in store");
        Stream.of(pathUpdates)
            .filter(Objects::nonNull)
            .forEach(update -> {
                try {
                    update.update();
                } catch (IOException e) {
                    //not critical, just log
                    LOGGER.warn(() -> "Error updating last modified date of " + update.path() + " to "
                        + update.dateModified(), e);
                }
            });
    }

    /**
     * Replace the old base directory with the new base directory, backing up the old base directory.
     *
     * @param oldBaseDir the old base directory
     * @param backUpDir the backup directory
     * @param newBaseDir the new base directory
     * @throws IOException if an I/O error occurs
     */
    static void replaceDirectories(Path oldBaseDir, Path backUpDir, Path newBaseDir) throws IOException {
        Files.move(oldBaseDir, backUpDir);
        Files.move(newBaseDir, oldBaseDir);
    }

    /**
     * Initialize a new SVN repository at the given URL.
     *
     * @param svnUrl the URL of the SVN repository
     * @return the initialized SVN repository
     * @throws SVNException if an error occurs while initializing the repository
     */
    static SVNRepository initializeSVNRepository(String svnUrl) throws SVNException {
        URI repositoryURI = URI.create(svnUrl);
        FSRepositoryFactory.setup();
        File repoDir = new File(repositoryURI);
        SVNAdminClient admin =
            new SVNAdminClient((ISVNAuthenticationManager) null, SVNWCUtil.createDefaultOptions(true));
        boolean enableRevisionProperties = true; //required to set svn:date and svn:author
        SVNURL repURL =
            admin.doCreateRepository(repoDir, null, enableRevisionProperties, false,
                false,
                false,
                false,
                false,
                true,
                false); //subversion 1.10+ with lz4 compression and performance improvements
        LOGGER.info("Creating new SVN repository at: {}", repURL);
        return SVNRepositoryFactory.create(repURL);
    }

    /**
     * Commit the given content to the SVN repository at the given paths with the given revision properties.
     * Creates intermediate directories as needed.
     *
     * @param repository the SVN repository
     * @param paths the paths to commit to, with intermediate directories
     * @param input the content to commit
     * @param revProps the revision properties
     * @param executorService executor service to run post-commit tasks
     * @throws IOException if an I/O error occurs
     */
    static void commit(SVNRepository repository, String[] paths, MCRContent input,
        SVNProperties revProps, ExecutorService executorService) throws IOException, SVNException {

        SVNCommitInfo info = commitFile(repository, paths, input, revProps);
        updateMissingRevisionUpdates(repository, revProps, executorService, info);
    }

    private static void updateMissingRevisionUpdates(SVNRepository repository, SVNProperties revProps,
        ExecutorService executorService,
        SVNCommitInfo info) {
        long revision = info.getNewRevision();
        SVNPropertyValue date = revProps.getSVNPropertyValue(SVNRevisionProperty.DATE);
        //run in executor to not block main thread
        executorService.execute(() -> updateRevisionDate(repository, revision, date));
    }

    static void updateRevisionDate(SVNRepository repository, long revision, SVNPropertyValue date) {
        LOGGER.debug("Change commit date {}", date::getString);
        try {
            repository.setRevisionPropertyValue(revision, SVNRevisionProperty.DATE, date);
        } catch (SVNException e) {
            throw new MCRException(e);
        }
    }

    static SVNCommitInfo deleteFile(SVNRepository repository, String path, SVNProperties revProps,
        ExecutorService executorService)
        throws SVNException {
        String user = revProps.getStringValue(SVNRevisionProperty.AUTHOR);
        // Start commit editor
        repository.setAuthenticationManager(AUTHENTICATION_MANAGER_CACHE.getUnchecked(user));
        ISVNEditor editor = repository.getCommitEditor(revProps.getStringValue(SVNRevisionProperty.LOG), null);
        editor.openRoot(-1);
        editor.deleteEntry(path, -1);
        editor.closeDir(); // root
        SVNCommitInfo svnCommitInfo = editor.closeEdit();
        updateMissingRevisionUpdates(repository, revProps, executorService, svnCommitInfo);
        return svnCommitInfo;
    }

    private static SVNCommitInfo commitFile(SVNRepository repository, String[] paths, MCRContent input,
        SVNProperties revProps) throws SVNException, IOException {
        String filePath = paths[paths.length - 1];
        // Check which paths already exist in SVN
        int existing = paths.length - 1;
        for (; existing >= 0; existing--) {
            if (!repository.checkPath(paths[existing], -1).equals(SVNNodeKind.NONE)) {
                break;
            }
        }

        String user = revProps.getStringValue(SVNRevisionProperty.AUTHOR);
        // Start commit editor
        repository.setAuthenticationManager(AUTHENTICATION_MANAGER_CACHE.getUnchecked(user));
        ISVNEditor editor = repository.getCommitEditor(revProps.getStringValue(SVNRevisionProperty.LOG), null);
        editor.openRoot(-1);

        existing += 1;
        // Create directories in SVN that do not exist yet
        for (int i = existing; i < paths.length - 1; i++) {
            LOGGER.debug("SVN create directory {}", paths[i]);
            editor.addDir(paths[i], null, -1);
            editor.closeDir();
        }

        // Commit file changes
        LOGGER.debug("Commit file {}", filePath);
        if (existing < paths.length) {
            editor.addFile(filePath, null, -1);
        } else {
            editor.openFile(filePath, -1);
        }

        editor.applyTextDelta(filePath, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

        String checksum;
        try (InputStream in = input.getInputStream()) {
            checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
        }

        editor.changeFileProperty(filePath, SVNProperty.MIME_TYPE, SVNPropertyValue.create("text/xml"));

        editor.closeFile(filePath, checksum);
        editor.closeDir(); // root

        return editor.closeEdit();
    }

    /**
     * Extract the integer ID to use with a {@link org.mycore.datamodel.ifs2.MCRStore} from the given path.
     *
     * @param path the path to extract the ID from
     * @return the extracted ID, or -1 if no ID could be extracted
     */
    static int getId(String path) {
        //pattern to match last integer before file extension
        Matcher m = INT_ID_PATTERN.matcher(path);
        if (!m.find()) {
            return -1;
        }
        return Integer.parseInt(m.group(1));
    }

    /**
     * Create a new migration store based on the given original store ID and properties.
     * The new store ID and properties are derived from the original ones by appending ".new" to paths.
     *
     * @param origStoreId the original store ID
     * @param subPropertiesMap the original store properties
     * @return the created migration store
     */
    static MCRMigrationStore getMigrationStore(String origStoreId, Map<String, String> subPropertiesMap) {
        Map<String, String> storeProperties = new TreeMap<>(subPropertiesMap);
        storeProperties.replaceAll((k, v) -> switch (k) {
            case "BaseDir", "SVNRepositoryURL" -> {
                String newV = toMigrationPath(v);
                LOGGER.info("New {}: {}", k, newV);
                yield newV;
            }
            case "Class" -> MCRMigrationStore.class.getName();
            default -> v;
        });
        storeProperties.putIfAbsent("Prefix", origStoreId + "_");
        String newId = toMigrationPath(origStoreId);
        storeProperties.forEach((k, v) -> {
            LOGGER.info("Copy property MCR.IFS2.Store.{}.{} = {}", newId, k, v);
            MCRConfiguration2.set("MCR.IFS2.Store." + newId + "." + k, v);
        });
        return MCRStoreManager.computeStoreIfAbsent(newId, () -> {
            try {
                return MCRStoreManager.buildStore(newId, MCRMigrationStore.class);
            } catch (ReflectiveOperationException e) {
                throw new MCRException(e);
            }
        });
    }

    /**
     * Convert the given path to a migration path by appending {@link #MIGRATION_STORE_SUFFIX}.
     *
     * @param path the original path either with or without trailing path separator
     * @return the migration path
     */
    static String toMigrationPath(String path) {
        return LAST_PATH_SEGMENT.matcher(path).replaceAll("$1" + MIGRATION_STORE_SUFFIX + "$2");
    }

    static String[] getPaths(Path baseDirectory, Path path) {
        Path relativized = baseDirectory.relativize(path);
        int nameCount = relativized.getNameCount();
        String[] paths = new String[nameCount];
        StringBuilder component = new StringBuilder();

        for (int i = 0; i < nameCount; i++) {
            if (i > 0) {
                component.append('/');
            }
            component.append(relativized.getName(i));
            paths[i] = component.toString();
        }
        return paths;
    }

    /**
     * Check if two MyCoRe objects are semantically equal, ignoring revision data such as modification dates and
     * modifiedBy flags.
     *
     * @param oldDocument the old MyCoRe object
     * @param newDocument the new MyCoRe object
     * @return true if the objects are semantically equal, false otherwise
     */
    static boolean objectsAreSemanticalEqual(Document oldDocument, Document newDocument) {
        Document oldCopy = oldDocument.clone();
        Document newCopy = newDocument.clone();
        removeRevisionData(oldCopy);
        removeRevisionData(newCopy);
        return MCRXMLHelper.deepEqual(oldCopy, newCopy);
    }

    /**
     * Remove revision data such as modification dates and modifiedBy flags from the given MyCoRe object document.
     *
     * @param mycoreObject the MyCoRe object document to remove revision data from
     */
    private static void removeRevisionData(Document mycoreObject) {
        Element service = mycoreObject.getRootElement().getChild(MCRObjectService.XML_NAME);
        List<Element> toRemove = new ArrayList<>();
        for (Element e : service.getDescendants(new MCRObjectModificationElementFilter())) {
            toRemove.add(e);
        }
        toRemove.forEach(e -> {
            LOGGER.debug("Removing {} of type {}", e::getName, () -> e.getAttributeValue("type"));
            e.detach();
        });
    }

    /**
     * Delete a file from the new store and mark its path update as null.
     *
     * @param store the new migration store
     * @param pathUpdates an array to track path updates and their modification dates
     * @param path the path of the file to delete
     * @throws IOException if an I/O error occurs during deletion
     */
    static void deleteFile(MCRMigrationStore store, PathDateModifiedUpdate[] pathUpdates, String path)
        throws IOException {
        int id = getId(path);
        LOGGER.debug("File deleted: {}, id: {}", path, id);
        pathUpdates[id] = null;
        store.delete(id);
    }
}
