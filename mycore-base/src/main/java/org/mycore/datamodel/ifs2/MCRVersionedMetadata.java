/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

/**
 * Represents an XML metadata document that is stored in a local filesystem
 * store and in parallel in a Subversion repository to track and restore
 * changes.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersionedMetadata extends MCRStoredMetadata {

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

        public long getLastRevision() {
            return lastRevision;
        }
    }

    /**
     * The logger
     */
    protected final static Logger LOGGER = LogManager.getLogger();

    /**
     * The revision number of the metadata version that is currently in the
     * local filesystem store.
     */
    protected Optional<Long> revision;

    /**
     * Creates a new metadata object both in the local store and in the SVN
     * repository
     * 
     * @param store
     *            the store this object is stored in
     * @param fo
     *            the file storing the data in the local filesystem
     * @param id
     *            the id of the metadata object
     */
    MCRVersionedMetadata(MCRMetadataStore store, FileObject fo, int id, String docType) {
        super(store, fo, id, docType);
        try {
            // 1. current revision, 2. deleted revision, empty()
            revision = Optional.ofNullable(Optional.ofNullable(getStore().getRepository().info(getFilePath(), -1))
                .map(SVNDirEntry::getRevision).orElseGet(this::getLastRevision));
        } catch (SVNException e) {
            LOGGER.error("Could not get last revision of " + getStore().getID() + "_" + id, e);
            revision = Optional.empty();
        }
    }

    @Override
    public MCRVersioningMetadataStore getStore() {
        return (MCRVersioningMetadataStore) store;
    }

    /**
     * Stores a new metadata object first in the SVN repository, then
     * additionally in the local store.
     * 
     * @param xml
     *            the metadata document to store
     * @throws JDOMException thrown by {@link MCRStoredMetadata#create(MCRContent)}
     */
    @Override
    void create(MCRContent xml) throws IOException, JDOMException {
        super.create(xml);
        commit("create");
    }

    /**
     * Updates this metadata object, first in the SVN repository and then in the
     * local store
     * 
     * @param xml
     *            the new version of the document metadata
     * @throws JDOMException thrown by {@link MCRStoredMetadata#create(MCRContent)}
     */
    @Override
    public void update(MCRContent xml) throws IOException, JDOMException {
        if (isDeleted()) {
            create(xml);
        } else {
            super.update(xml);
            commit("update");
        }
    }

    void commit(String mode) throws IOException {
        // Commit to SVN
        SVNCommitInfo info;
        try {
            SVNRepository repository = getStore().getRepository();

            // Check which paths already exist in SVN
            String[] paths = store.getSlotPaths(id);
            int existing = paths.length - 1;
            for (; existing >= 0; existing--) {
                if (!repository.checkPath(paths[existing], -1).equals(SVNNodeKind.NONE)) {
                    break;
                }
            }

            existing += 1;

            // Start commit editor
            String commitMsg = mode + "d metadata object " + store.getID() + "_" + id + " in store";
            ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
            editor.openRoot(-1);

            // Create directories in SVN that do not exist yet
            for (int i = existing; i < paths.length - 1; i++) {
                LOGGER.debug("SVN create directory " + paths[i]);
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

            InputStream in = fo.getContent().getInputStream();
            String checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
            in.close();

            if (store.shouldForceXML()) {
                editor.changeFileProperty(filePath, SVNProperty.MIME_TYPE, SVNPropertyValue.create("text/xml"));
            }

            editor.closeFile(filePath, checksum);
            editor.closeDir(); // root

            info = editor.closeEdit();
        } catch (SVNException e) {
            throw new IOException(e);
        }
        revision = Optional.of(info.getNewRevision());
        LOGGER.info("SVN commit of " + mode + " finished, new revision " + getRevision());

        if (MCRVersioningMetadataStore.shouldSyncLastModifiedOnSVNCommit()) {
            setLastModified(info.getDate());
        }
    }

    /**
     * Deletes this metadata object in the SVN repository, and in the local
     * store.
     */
    @Override
    public void delete() throws IOException {
        if (isDeleted()) {
            String msg = "You can not delete already deleted data: " + id;
            throw new MCRUsageException(msg);
        }
        if (!store.exists(id)) {
            throw new IOException("Does not exist: " + store.getID() + "_" + id);
        }
        String commitMsg = "Deleted metadata object " + store.getID() + "_" + id + " in store";

        // Commit to SVN
        SVNCommitInfo info;
        try {
            SVNRepository repository = getStore().getRepository();
            ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
            editor.openRoot(-1);
            editor.deleteEntry(getFilePath(), -1);
            editor.closeDir();

            info = editor.closeEdit();
            revision = Optional.of(info.getNewRevision());
            LOGGER.info("SVN commit of delete finished, new revision " + getRevision());
        } catch (SVNException e) {
            LOGGER.error("Error while deleting " + id + " in SVN ", e);
        } finally {
            super.delete();
        }
    }

    public boolean isDeleted() throws IOException {
        return super.isDeleted() || isDeletedInRepository();
    }

    private boolean isDeletedInRepository() throws IOException {
        return getRevision() >= 0 && getRevision(getRevision()).getType() == MCRMetadataVersion.DELETED;
    }

    /**
     * Updates the version stored in the local filesystem to the latest version
     * from Subversion repository HEAD.
     */
    public void update() throws Exception {
        SVNRepository repository = getStore().getRepository();
        MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream();
        revision = Optional.of(repository.getFile(getFilePath(), -1, null, baos));
        baos.close();
        new MCRByteContent(baos.getBuffer(), 0, baos.size(), this.getLastModified().getTime()).sendTo(fo);
    }

    /**
     * Lists all versions of this metadata object available in the subversion
     * repository
     * 
     * @return all stored versions of this metadata object
     */
    @SuppressWarnings("unchecked")
    public List<MCRMetadataVersion> listVersions() throws IOException {
        try {
            List<MCRMetadataVersion> versions = new ArrayList<MCRMetadataVersion>();
            SVNRepository repository = getStore().getRepository();
            String path = getFilePath();
            String dir = getDirectory();

            Collection<SVNLogEntry> entries = null;
            try {
                entries = repository.log(new String[] { dir }, null, 0, repository.getLatestRevision(), true, true);
            } catch (Exception ioex) {
                LOGGER.error("Could not get versions", ioex);
                return versions;
            }

            for (SVNLogEntry entry : entries) {
                SVNLogEntryPath svnLogEntryPath = entry.getChangedPaths().get(path);
                if (svnLogEntryPath != null) {
                    char type = svnLogEntryPath.getType();
                    versions.add(new MCRMetadataVersion(this, entry, type));
                }
            }
            return versions;
        } catch (SVNException svnExc) {
            throw new IOException(svnExc);
        }
    }

    private String getFilePath() {
        return "/" + store.getSlotPath(id);
    }

    private String getDirectory() {
        String path = getFilePath();
        return path.substring(0, path.lastIndexOf('/'));
    }

    public MCRMetadataVersion getRevision(long revision) throws IOException {
        try {
            if (revision < 0) {
                revision = getLastPresentRevision();
                if (revision < 0) {
                    LOGGER.warn(
                        MessageFormat.format("Metadata object {0} in store {1} has no last revision!", getID(),
                            getStore().getID()));
                    return null;
                }
            }
            SVNRepository repository = getStore().getRepository();
            String path = getFilePath();
            String dir = getDirectory();
            @SuppressWarnings("unchecked")
            Collection<SVNLogEntry> log = repository.log(new String[] { dir }, null, revision, revision, true, true);
            for (SVNLogEntry logEntry : log) {
                SVNLogEntryPath svnLogEntryPath = logEntry.getChangedPaths().get(path);
                if (svnLogEntryPath != null) {
                    char type = svnLogEntryPath.getType();
                    return new MCRMetadataVersion(this, logEntry, type);
                }
            }
            LOGGER.warn(MessageFormat.format("Metadata object {0} in store {1} has no revision ''{2}''!", getID(),
                getStore().getID(),
                getRevision()));
            return null;
        } catch (SVNException svnExc) {
            throw new IOException(svnExc);
        }
    }

    public long getLastPresentRevision() throws SVNException {
        return getLastRevision(false);
    }

    private long getLastRevision(boolean deleted) throws SVNException {
        SVNRepository repository = getStore().getRepository();
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
        } catch (LastRevisionFoundException e) {
        }
        return lastRevisionLogHandler.getLastRevision();
    }

    private Long getLastRevision() {
        try {
            long lastRevision = getLastRevision(true);
            return lastRevision < 0 ? null : lastRevision;
        } catch (SVNException e) {
            LOGGER.warn("Could not get last revision of: " + getStore() + "_" + id, e);
            return null;
        }
    }

    /**
     * Returns the revision number of the version currently stored in the local
     * filesystem store.
     * 
     * @return the revision number of the local version
     */
    public long getRevision() {
        return revision.orElse(-1l);
    }

    /**
     * Checks if the version in the local store is up to date with the latest
     * version in SVN repository
     * 
     * @return true, if the local version in store is the latest version
     */
    public boolean isUpToDate() throws IOException {
        SVNDirEntry entry;
        try {
            SVNRepository repository = getStore().getRepository();
            entry = repository.info(getFilePath(), -1);
        } catch (SVNException e) {
            throw new IOException(e);
        }
        return entry.getRevision() <= getRevision();
    }
}
