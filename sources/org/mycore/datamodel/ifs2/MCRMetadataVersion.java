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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Provides information about a stored version of metadata and allows to
 * retrieve that version from SVN
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataVersion {
    /**
     * The metadata document this version belongs to
     */
    private MCRVersionedMetadata vm;

    /**
     * The revision number of this version
     */
    private long revision;

    /**
     * The user that created this version
     */
    private String user;

    /**
     * The date this version was created
     */
    private Date date;

    /**
     * Creates a new metadata version info object
     * 
     * @param id
     *            the ID of the metadata document this version belongs to
     */
    MCRMetadataVersion(MCRVersionedMetadata vm, SVNLogEntry logEntry) {
        this.vm = vm;
        this.revision = logEntry.getRevision();
        this.user = logEntry.getAuthor();
        this.date = logEntry.getDate();
    }

    /**
     * Returns the metadata object this version belongs to
     * 
     * @return the metadata object this version belongs to
     */
    public MCRVersionedMetadata getMetadataObject() {
        return vm;
    }

    /**
     * Returns the SVN revision number of this version
     * 
     * @return the SVN revision number of this version
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Returns the user that created this version
     * 
     * @return the user that created this version
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the date and time this version was created
     * 
     * @return the date and time this version was created
     */
    public Date getDate() {
        return date;
    }

    /**
     * Retrieves this version of the metadata
     * 
     * @return the metadata document as it was in this version
     */
    public Document retrieve() throws Exception {
        SVNRepository repository = vm.getStore().getRepository();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SVNProperties properties = new SVNProperties();
        repository.getFile(vm.getStore().getSlotPath(vm.getID()), revision, properties, baos);
        baos.close();
        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        Document xml = new SAXBuilder().build(in);
        in.close();
        return xml;
        // TODO: Check keyword substitution, check revision number
    }

    /**
     * Replaces the current version of the metadata object with this version,
     * which means that a new version is created that is identical to this old
     * version. The stored metadata document is updated to this old version of
     * the metadata.
     */
    public void restore() throws Exception {
        vm.update(retrieve());
    }
}
