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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;

/**
 * Represents an XML metadata document that is stored in a local filesystem
 * store and in parallel in a Subversion repository to track and restore
 * changes.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersionedMetadata extends MCRStoredMetadata {

    /**
     * The revision number of the metadata version that is currently in the
     * local filesystem store.
     */
    protected int revision;

    /**
     * Creates a new metadata object
     * 
     * @param store
     *            the store this object is stored in
     * @param fo
     *            the file storing the data in the local filesystem
     * @param id
     *            the id of the metadat object
     */
    MCRVersionedMetadata(MCRMetadataStore store, FileObject fo, int id) {
        super(store, fo, id);
    }

    void create(Document xml) throws Exception {
        // TODO: create in SVN, use revision keyword in comment
        super.create(xml);
    }

    public void delete() throws Exception {
        // TODO: delete in SVN
        super.delete();
    }

    public void update(Document xml) throws Exception {
        // TODO: update in SVN, use revision keyword in comment
        super.update(xml);
    }

    /**
     * Updates the version stored in the local filesystem to the latest version
     * from Subversion repository HEAD.
     */
    public void update() throws Exception {
        // TODO: update to HEAD from SVN
    }

    /**
     * Lists all versions of this metadata object available in the subversion
     * repository
     * 
     * @return all stored versions of this metadata object
     */
    public List<MCRMetadataVersion> listVersions() {
        // TODO: retrieve versions from SVN
        return new ArrayList<MCRMetadataVersion>();
    }

    /**
     * Returns the revision number of the version currently stored in the local
     * filesystem store.
     * 
     * @return the revision number of the local version
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Checks if the version in the local store is up to date with the latest
     * version in SVN repository
     * 
     * @return true, if the local version in store is the latest version
     */
    public boolean isUpToDate() throws Exception {
        // TODO: check if version in store is up to date
        return true;
    }
}
