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

import org.apache.commons.vfs.FileObject;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
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
        xml = new MCRContent(fo);

        String commitMsg = "Created metadata object " + store.getID() + "_" + id + " in store";
        SVNRepository repository = getStore().getRepository();

        // Create slot subdirectories in SVN, if not existing yet
        String[] parts = store.getSlotPathParts(id);
        List<String> dirsToCreate = new ArrayList<String>();
        StringBuffer path = new StringBuffer();
        for (int i = 0; i < parts.length - 1; i++) {
            path.append(parts[i]);
            SVNNodeKind nodeKind = repository.checkPath(path.toString(), -1);
            if (nodeKind.equals(SVNNodeKind.NONE))
                dirsToCreate.add(path.toString());
            if (path.length() > 0)
                path.append('/');
        }

        ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
        editor.openRoot(-1);

        for (String dir : dirsToCreate) {
            LOGGER.info("Create in SVN: directory " + dir);
            editor.addDir(dir, null, -1);
            editor.closeDir();
        }

        path.append(parts[parts.length - 1]);
        String filePath = path.toString();
        LOGGER.info("Create in SVN: file " + filePath);
        editor.addFile(filePath, null, -1);
        editor.applyTextDelta(filePath, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

        InputStream in = xml.getInputStream();
        String checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
        in.close();
        editor.closeFile(filePath, checksum);
        editor.closeDir();

        // Commit to SVN
        SVNCommitInfo info = editor.closeEdit();
        this.revision = info.getNewRevision();
        LOGGER.info("SVN commit of create finished, new revision " + revision);

        setLastModified( info.getDate() );
    }

    /**
     * Deletes this metadata object in the SVN repository, and in the local
     * store.
     */
    public void delete() throws Exception {
        String commitMsg = "Deleted metadata object " + store.getID() + "_" + id + " in store";
        String filePath = store.getSlotPath(id);
        LOGGER.info("Delete in SVN: file " + filePath);

        SVNRepository repository = getStore().getRepository();
        ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
        editor.openRoot(-1);
        editor.deleteEntry(filePath, -1);
        editor.closeDir();

        // Commit to SVN
        SVNCommitInfo info = editor.closeEdit();
        this.revision = info.getNewRevision();
        LOGGER.info("SVN commit of delete finished, new revision " + revision);

        store.delete(fo);
    }

    /**
     * Updates this metadata object, first in the SVN repository and then in the
     * local store
     * 
     * @param xml
     *            the new version of the document metadata
     */
    public void update(MCRContent xml) throws Exception {
        super.update(xml);
        xml = new MCRContent(fo);

        String commitMsg = "Updated metadata object " + store.getID() + "_" + id + " in store";
        String filePath = store.getSlotPath(id);
        LOGGER.info("Update in SVN: file " + filePath);

        SVNRepository repository = getStore().getRepository();

        ISVNEditor editor = repository.getCommitEditor(commitMsg, null);
        editor.openRoot(-1);
        editor.openFile(filePath, -1);
        editor.applyTextDelta(filePath, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
        InputStream in = xml.getInputStream();
        String checksum = deltaGenerator.sendDelta(filePath, in, editor, true);
        in.close();
        editor.closeFile(filePath, checksum);
        editor.closeDir();

        // Commit to SVN
        SVNCommitInfo info = editor.closeEdit();
        this.revision = info.getNewRevision();
        LOGGER.info("SVN commit of update finished, new revision " + revision);

        setLastModified( info.getDate() );
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
        Collection<SVNLogEntry> entries = (Collection<SVNLogEntry>)(repository.log(new String[] { path }, null, 0, -1, true, true));
        for (SVNLogEntry entry : entries)
            versions.add(new MCRMetadataVersion(this, entry));
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
