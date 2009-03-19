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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileObject;
import org.apache.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
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

    /**
     * The logger
     */
    protected final static Logger LOGGER = Logger.getLogger(MCRVersionedMetadata.class);

    /**
     * The revision number of the metadata version that is currently in the
     * local filesystem store.
     */
    protected long revision;

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
    MCRVersionedMetadata(MCRMetadataStore store, FileObject fo, int id) {
        super(store, fo, id);
        // TODO: set revision of existing data at retrieve()
    }

    public MCRVersioningMetadataStore getStore() {
        return (MCRVersioningMetadataStore) store;
    }

    /**
     * Stores a new metadata object first in the SVN repository, then
     * additionally in the local store.
     * 
     * @param xml
     *            the metadata document to store
     */
    void create(MCRContent xml) throws Exception {
        super.create(xml);
        commit("create");
    }

    /**
     * Updates this metadata object, first in the SVN repository and then in the
     * local store
     * 
     * @param xml
     *            the new version of the document metadata
     */
    public void update(MCRContent xml) throws Exception {
        if (isDeleted())
            create(xml);
        else {
            super.update(xml);
            commit("update");
        }
    }

    void commit(String mode) throws Exception {
        SVNRepository repository = getStore().getRepository();

        // Check which paths already exist in SVN
        String[] paths = store.getSlotPaths(id);
        int existing = paths.length - 1;
        for (; existing >= 0; existing--)
            if (!repository.checkPath(paths[existing], -1).equals(SVNNodeKind.NONE))
                break;

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
        if (existing < paths.length)
            editor.addFile(filePath, null, -1);
        else
            editor.openFile(filePath, -1);

        editor.applyTextDelta(filePath, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

        InputStream in = fo.getContent().getInputStream();
        String checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
        in.close();

        if (store.shouldForceXML())
            editor.changeFileProperty(filePath, SVNProperty.MIME_TYPE, SVNPropertyValue.create("text/xml"));

        editor.closeFile(filePath, checksum);
        editor.closeDir(); // root

        // Commit to SVN
        SVNCommitInfo info = editor.closeEdit();
        this.revision = info.getNewRevision();
        LOGGER.info("SVN commit of " + mode + " finished, new revision " + revision);

        setLastModified(info.getDate());
    }

    /**
     * Deletes this metadata object in the SVN repository, and in the local
     * store.
     */
    public void delete() throws Exception {
        if (isDeleted()) {
            String msg = "You can not delete already deleted data: " + id;
            throw new MCRUsageException(msg);
        }
        String commitMsg = "Deleted metadata object " + store.getID() + "_" + id + " in store";

        SVNRepository repository = getStore().getRepository();
        ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
        editor.openRoot(-1);
        editor.deleteEntry(store.getSlotPath(id), -1);
        editor.closeDir();

        // Commit to SVN
        SVNCommitInfo info = editor.closeEdit();
        this.revision = info.getNewRevision();
        LOGGER.info("SVN commit of delete finished, new revision " + revision);

        store.delete(fo);
    }

    /**
     * Updates the version stored in the local filesystem to the latest version
     * from Subversion repository HEAD.
     */
    public void update() throws Exception {
        SVNRepository repository = getStore().getRepository();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        revision = repository.getFile(store.getSlotPath(id), -1, null, baos);
        baos.close();
        new MCRContent(baos.toByteArray()).sendTo(fo);
    }

    /**
     * Lists all versions of this metadata object available in the subversion
     * repository
     * 
     * @return all stored versions of this metadata object
     */
    public List<MCRMetadataVersion> listVersions() throws Exception {
        List<MCRMetadataVersion> versions = new ArrayList<MCRMetadataVersion>();
        SVNRepository repository = getStore().getRepository();
        String path = store.getSlotPath(id);

        String dir = (path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "");
        Collection<SVNLogEntry> entries = (Collection<SVNLogEntry>) (repository.log(new String[] { dir }, null, 0, -1, true, true));

        path = "/" + path;
        for (SVNLogEntry entry : entries) {
            Map<String, SVNLogEntryPath> paths = entry.getChangedPaths();
            if (paths.containsKey(path)) {
                char type = paths.get(path).getType();
                versions.add(new MCRMetadataVersion(this, entry, type));
            }
        }
        return versions;
    }

    /**
     * Returns the revision number of the version currently stored in the local
     * filesystem store.
     * 
     * @return the revision number of the local version
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Checks if the version in the local store is up to date with the latest
     * version in SVN repository
     * 
     * @return true, if the local version in store is the latest version
     */
    public boolean isUpToDate() throws Exception {
        SVNRepository repository = getStore().getRepository();
        SVNDirEntry entry = repository.info(store.getSlotPath(id), -1);
        return (entry.getRevision() <= revision);
    }
}
