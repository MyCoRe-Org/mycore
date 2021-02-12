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

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.datamodel.ifs2.MCRMetadataVersion.MCRMetadataVersionState;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.xml.sax.SAXException;

/**
 * A generic interface to {@link MCRMetadataStore}s.
 * 
 * An MCRMetadata instance represents an object that is (to be) stored in an
 * MCRMetadataStore. It has an immutable set of information about its associated store & ID,
 * everything else is queried from the store.
 * 
 * @author Christoph Neidahl (OPNA2608)
 *
 */
public class MCRMetadata {

    protected static final Logger LOGGER = LogManager.getLogger();

    /**
     * The {@link MCRMetadataStore} object this MCRMetadata object belongs to.
     */
    private final MCRMetadataStore store;

    /**
     * The object's ID.
     */
    private final int id;

    /**
     * Syncing status. Set to <code>false</code> if this object's information
     * couldn't or hasn't been synced with the linked store yet.
     * 
     * Possible reasons for a negative syncing status with the store:
     * <ul>
     *   <li>
     *     The object has just been instantiated and no CRUD operations have been called yet.
     *   </li>
     *   <li>
     *     An exception has occurred while communicating with the store backend.
     *   </li>
     * </ul>
     */
    private boolean synced = false;

    /**
     * The revision of the MyCoRe metadata object this instance references.
     * 
     * May be <code>null</code> if
     * <ul>
     *   <li>
     *     No revision has been specified on object instantiation.
     *   </li>
     *   <li>
     *     {@link #delete()} has been called and the store implementation is incapable of tracking
     *     {@link MCRMetadataVersionState.DELETED} revisions.
     *   </li>
     * </ul>
     */
    protected Long revision;

    /**
     * The current revision's file contents.
     * 
     * May be <code>null</code> if
     * <ul>
     *   <li>
     *     The object has just been instantiated and no CRU operations have been called yet.
     *   </li>
     *   <li>
     *     {@link #delete()} has been called successfully or a {@link MCRVersioningMetadtaState.DELETED}
     *     revision is used
     *   </li>
     * </ul>
     */
    protected MCRContent content;

    /**
     * TODO
     */
    protected String user;

    /**
     * The current revision's commit date.
     * 
     * May be <code>null</code> if
     * <ul>
     *   <li>
     *     The object has just been instantiated and no CRU operations have been called yet.
     *   </li>
     *   <li>
     *     {@link #delete()} has been called successfully and the store implementation is incapable of tracking
     *     {@link MCRMetadataVersionState.DELETED} revisions.
     *   </li>
     * </ul>
     */
    protected Date date;

    /**
     * TODO
     * @param store
     * @param id
     */
    public MCRMetadata(MCRMetadataStore store, int id) {
        this.store = store;
        this.id = id;
    }

    /**
     * TODO
     * @param store
     * @param id
     * @param revision
     */
    public MCRMetadata(MCRMetadataStore store, int id, long revision) {
        this(store, id);
        this.revision = revision;
    }

    /***** START COMPATIBILITY *****/

    /**
     * TODO needs to be moved into the SVN store eventually
     * @param type
     * @param content
     * @throws MCRPersistenceException
     */
    private void commit(MCRMetadataVersionState type, MCRContent content) throws MCRPersistenceException {
        String commitMsg = type.toString().toLowerCase(Locale.ROOT) + " metadata object " + getFullID()
            + " in store";
        SVNCommitInfo info;
        try {
            MCRVersioningMetadataStore svnStore = (MCRVersioningMetadataStore) (store);
            SVNRepository repository = svnStore.getRepository();

            if (type != MCRMetadataVersionState.DELETED) {
                String[] paths = store.getSlotPaths(getID());
                int existing = paths.length - 1;
                for (; existing >= 0; existing--) {
                    if (!repository.checkPath(paths[existing], -1).equals(SVNNodeKind.NONE)) {
                        break;
                    }
                }

                existing += 1;

                ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
                editor.openRoot(-1);

                // Create directories in SVN that do not exist yet
                for (int i = existing; i < paths.length - 1; i++) {
                    LOGGER.debug("SVN create directory {}", paths[i]);
                    editor.addDir(paths[i], null, -1);
                    editor.closeDir();
                }

                // Commit file changes
                String filePath = paths[paths.length - 1];
                if (existing < paths.length) {
                    editor.addFile(filePath, null, -1);
                } else {
                    editor.openFile(filePath, -1);
                }

                editor.applyTextDelta(filePath, null);
                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

                String checksum;
                try (InputStream in = content.getContentInputStream()) {
                    checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
                }

                if (store.shouldForceXML()) {
                    editor.changeFileProperty(filePath, SVNProperty.MIME_TYPE, SVNPropertyValue.create("text/xml"));
                }

                editor.closeFile(filePath, checksum);

                editor.closeDir(); // root
                info = editor.closeEdit();
                if (MCRVersioningMetadataStore.shouldSyncLastModifiedOnSVNCommit()) {
                    setLastModified(info.getDate());
                }
            } else {
                ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
                editor.openRoot(-1);

                editor.deleteEntry(store.getSlotPath(getID()), -1);

                editor.closeDir();
                info = editor.closeEdit();
            }
            LOGGER.info("SVN commit of {} finished, new revision {}", type.toString(), info.getNewRevision());
        } catch (SVNException | IOException e) {
            throw new MCRPersistenceException("Failed to commit changes to SVN repository!", e);
        }
    }

    private static final Map<String, MCRMetadataVersionState> TYPE_STATE_MAPPING = Stream
        .of(new AbstractMap.SimpleImmutableEntry<>("A", MCRMetadataVersionState.CREATED),
            new AbstractMap.SimpleImmutableEntry<>("M", MCRMetadataVersionState.UPDATED),
            new AbstractMap.SimpleImmutableEntry<>("R", MCRMetadataVersionState.UPDATED), /* should not occur */
            new AbstractMap.SimpleImmutableEntry<>("D", MCRMetadataVersionState.DELETED))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private String getFilePath() {
        return "/" + store.getSlotPath(getID());
    }

    private String getDirectory() {
        String path = getFilePath();
        return path.substring(0, path.lastIndexOf('/'));
    }

    /**
     * TODO copied from MCRVersionedMetadata, needs to be moved into stores
     * @param revision
     * @return
     * @throws IOException
     */
    public MCRMetadataVersion getRevision(long revision) throws IOException {
        try {
            if (revision < 0) {
                revision = getLastPresentRevision();
                if (revision < 0) {
                    LOGGER.warn("Metadata object {} in store {} has no last revision!", getID(), store.getID());
                    return null;
                }
            }
            MCRVersioningMetadataStore svnStore = (MCRVersioningMetadataStore) (store);
            SVNRepository repository = svnStore.getRepository();
            String path = getFilePath();
            String dir = getDirectory();
            @SuppressWarnings("unchecked")
            Collection<SVNLogEntry> log = repository.log(new String[] { dir }, null, revision, revision, true, true);
            for (SVNLogEntry logEntry : log) {
                SVNLogEntryPath svnLogEntryPath = logEntry.getChangedPaths().get(path);
                if (svnLogEntryPath != null) {
                    char type = svnLogEntryPath.getType();
                    return new MCRMetadataVersion(this, logEntry.getRevision(), logEntry.getAuthor(),
                        logEntry.getDate(),
                        TYPE_STATE_MAPPING.get(String.valueOf(type)));
                }
            }
            LOGGER.warn("Metadata object {} in store {} has no revision ''{}''!", getID(), getStore().getID(),
                getRevision());
            return null;
        } catch (SVNException svnExc) {
            throw new IOException(svnExc);
        }
    }

    public long getLastPresentRevision() throws SVNException {
        return getLastRevision(false);
    }

    private long getLastRevision(boolean deleted) throws SVNException {
        SVNRepository repository = ((MCRVersioningMetadataStore) (store)).getRepository();
        if (repository.getLatestRevision() == 0) {
            //new repository cannot hold a revision yet (MCR-1196)
            return -1;
        }
        final String path = getFilePath();
        String dir = getDirectory();
        LastRevisionLogHandler lastRevisionLogHandler = new LastRevisionLogHandler(path, deleted);
        int limit = 0; //we stop through LastRevisionFoundException
        try {
            repository.log(new String[] { dir }, repository.getLatestRevision(), 0, true, true, limit, false, null,
                lastRevisionLogHandler);
        } catch (LastRevisionFoundException ignored) {
        }
        return lastRevisionLogHandler.getLastRevision();
    }

    private Long getLastRevision() {
        try {
            long lastRevision = getLastRevision(true);
            return lastRevision < 0 ? null : lastRevision;
        } catch (SVNException e) {
            LOGGER.warn("Could not get last revision of: {}_{}", getStore(), id, e);
            return null;
        }
    }

    /**
     * TODO wrapper until implemented cleanly in stores
     * @param metadata
     * @param content
     * @throws MCRPersistenceException
     */
    private void createTransitionHelper(MCRContent content) throws MCRPersistenceException {
        // TODO replace with calls into store
        try {
            if (store.shouldForceXML()) {
                content = content.ensureXML();
            }
            Path xmlPath = store.getSlot(getID());
            if (!Files.exists(xmlPath.getParent())) {
                Files.createDirectories(xmlPath.getParent());
            }
            content.sendTo(xmlPath);
            if (store instanceof MCRVersioningMetadataStore) {
                commit(MCRMetadataVersionState.CREATED, content);
            }
        } catch (SAXException | IOException | JDOMException e) {
            throw new MCRPersistenceException("Failed to create " + getFullID() + "!", e);
        }
    }

    /**
     * TODO wrapper until implemented cleanly in stores
     * @param metadata
     * @param content
     * @throws MCRPersistenceException
     */
    private MCRContent readTransitionHelper() throws MCRPersistenceException {
        //TODO replace with calls into store
        // shared by both stores
        try {
            if (!MCRVersioningMetadataStore.class.isInstance(store) || revision == null) {
                Path xmlPath = store.getSlot(getID());
                if (Files.exists(xmlPath)) {
                    MCRPathContent pathContent = new MCRPathContent(xmlPath);
                    pathContent.setDocType(store.forceDocType);
                    return pathContent;
                    // if XML store, exit here with no content
                } else if (!MCRVersioningMetadataStore.class.isInstance(store)) {
                    return null;
                }
            }
            // if SVN store, check the SVN repo
            // if no revision specified, default to latest one
            if (revision == null) {
                revision = getLastRevision();
            }
            SVNRepository svnRepo = ((MCRVersioningMetadataStore) (store)).getRepository();
            MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream();
            svnRepo.getFile(store.getSlotPath(getID()), revision, null, baos);
            baos.close();
            return new MCRByteContent(baos.getBuffer(), 0, baos.size(), getDate().getTime());
        } catch (SVNException | IOException e) {
            throw new MCRPersistenceException("Failed to read " + getFullID() + "!", e);
        }
    }

    /**
     * TODO wrapper until implemented cleanly in stores
     * @param metadata
     * @param content
     * @throws MCRPersistenceException
     */
    private void updateTransitionHelper(MCRContent content) throws MCRPersistenceException {

    }

    /**
     * TODO wrapper until implemented cleanly in stores
     * @throws MCRPersistenceException
     */
    private void deleteTransitionHelper() throws MCRPersistenceException {

    }

    private MCRMetadataVersion getMetadataVersionTransitionHelper(int id, long revision)
        throws MCRPersistenceException {
        return new MCRMetadataVersion(this, MCRMetadataVersionState.CREATED);
    }

    /**
     * TODO implement XML + SVN wrapper
     * @param metadata
     * @return
     * @throws MCRPersistenceException
     */
    private static MCRMetadataVersion getMetadataVersionLastTransitionHelper(MCRMetadata metadata)
        throws MCRPersistenceException {
        return null;
    }

    /**
     * TODO implement XML + SVN wrapper
     * @param metadata
     * @return
     * @throws MCRPersistenceException
     */
    private static Date getLastModifiedTransitionHelper(MCRMetadata metadata) throws MCRPersistenceException {
        return null;
    }

    private static void setLastModifiedTransitionHelper(MCRMetadata metadata, Date date) throws MCRUsageException {

    }

    /***** END COMPATIBILITY *****/

    public void create(MCRContent content) throws MCRPersistenceException {
        try {
            // store.createContent(this, content);
            createTransitionHelper(content);
            this.content = content;
            // revision = store.getMetadataVersionLast(id).getRevision();
            revision = getMetadataVersionLastTransitionHelper(this).getRevision();
            // date = store.getLastModified(this);
            date = getLastModifiedTransitionHelper(this);
            synced = true;
        } catch (MCRPersistenceException e) {
            synced = false;
            throw new MCRPersistenceException(
                "Failed to create " + getFullID() + ", revision " + revision + " in store!", e);
        }
    }

    public MCRContent read() throws MCRPersistenceException {
        try {
            if (revision == null) {
                LOGGER.info("Reading " + getFullID() + " without revision requested, querying for latest revision.");
                // revision = store.getMetadataVersionLast(id).getRevision();
                revision = getMetadataVersionLastTransitionHelper(this).getRevision();
            }
            // content = store.readContent(this);
            content = readTransitionHelper();
            // date = store.getLastModified(this);
            date = getLastModifiedTransitionHelper(this);
            synced = true;
            return content;
        } catch (MCRPersistenceException e) {
            synced = false;
            throw new MCRPersistenceException(
                "Failed to read " + getFullID() + ", revision " + revision + " from store!", e);
        }
    }

    public void update(MCRContent content) throws MCRPersistenceException {
        try {
            // store.updateContent(this, content);
            updateTransitionHelper(content);
            // revision = getVersionLast().getRevision();
            revision = getMetadataVersionLastTransitionHelper(this).getRevision();
            // date = store.getLastModified(this);
            date = getLastModifiedTransitionHelper(this);
            this.content = content;
            synced = true;
        } catch (MCRPersistenceException e) {
            synced = false;
            throw new MCRPersistenceException(
                "Failed to update " + getFullID() + ", revision " + revision + " in store!", e);
        }
    }

    public void delete() throws MCRPersistenceException {
        try {
            // store.deleteContent(this);
            deleteTransitionHelper();
            // MCRMetadataVersion newVersion = getVersionLast();
            MCRMetadataVersion newVersion = getMetadataVersionLastTransitionHelper(this);
            // store may return null if deletion removes object history
            if (newVersion != null) {
                revision = newVersion.getRevision();
                date = newVersion.getDate();
            } else {
                revision = null;
                date = null;
            }
            this.content = null;
            synced = true;
        } catch (MCRPersistenceException e) {
            synced = false;
            throw new MCRPersistenceException(
                "Failed to delete " + getFullID() + ", revision " + revision + " from store!", e);
        }
    }

    public boolean isSynced() {
        return synced;
    }

    public String getBase() {
        return store.getID();
    }

    public int getID() {
        return id;
    }

    public String getFullID() {
        return store.getID() + "_" + store.createIDWithLeadingZeros(id);
    }

    public Long getRevision() {
        return revision;
    }

    public String getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }

    public MCRMetadataStore getStore() {
        return store;
    }

    public MCRMetadataVersion getMetadataVersion() {
        return getMetadataVersionTransitionHelper(getID(), revision);
    }

    public boolean isRevisionDeleted() {
        MCRMetadataVersion thisVersion = getMetadataVersion();
        // TODO replace with calls into store
        if (store instanceof MCRVersioningMetadataStore) {
            return thisVersion.getState() == MCRMetadataVersionState.DELETED;
        } else {
            return thisVersion != null;
        }
    }

    public boolean isLatestDeleted() {
        MCRMetadataVersion latestVersion = getMetadataVersionLastTransitionHelper(this);
        // TODO replace with calls into store
        if (store instanceof MCRVersioningMetadataStore) {
            return latestVersion.getState() == MCRMetadataVersionState.DELETED;
        } else {
            return latestVersion != null;
        }
    }

    public void restore() throws MCRPersistenceException {
        getMetadataVersionLastTransitionHelper(this).getMetadataObject().update(read());
    }

    public void setLastModified(Date date) throws MCRUsageException {
        // store.setLastModified(this, date);
        setLastModifiedTransitionHelper(this, date);
    }
    
    /***** START COMPATIBILITY INNER CLASSES *****/
    
    

    private static final class LastRevisionFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class LastRevisionLogHandler implements ISVNLogEntryHandler {
        private final String path;

        long lastRevision = -1;

        private boolean deleted;

        private LastRevisionLogHandler(String path, boolean deleted) {
            this.path = path;
            this.deleted = deleted;
        }

        @Override
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            SVNLogEntryPath svnLogEntryPath = logEntry.getChangedPaths().get(path);
            if (svnLogEntryPath != null) {
                char type = svnLogEntryPath.getType();
                if (deleted || type != SVNLogEntryPath.TYPE_DELETED) {
                    lastRevision = logEntry.getRevision();
                    //no other way to stop svnkit from logging
                    throw new LastRevisionFoundException();
                }
            }
        }

        long getLastRevision() {
            return lastRevision;
        }
    }
    
    /***** END COMPATIBILITY INNER CLASSES *****/
}
