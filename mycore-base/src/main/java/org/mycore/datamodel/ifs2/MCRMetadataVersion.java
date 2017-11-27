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
import java.util.Date;

import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
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
     * Was this version result of a create, update or delete?
     */
    private char type;

    /**
     * A version that was created in store
     */
    public static final char CREATED = 'A';

    /**
     * A version that was updated in store
     */
    public static final char UPDATED = 'M';

    /**
     * A version that was deleted in store
     */
    public static final char DELETED = 'D';

    /**
     * Creates a new metadata version info object
     * 
     * @param vm
     *            the metadata document this version belongs to
     * @param logEntry
     *            the log entry from SVN holding data on this version
     * @param type
     *            the type of commit
     */
    MCRMetadataVersion(MCRVersionedMetadata vm, SVNLogEntry logEntry, char type) {
        this.vm = vm;
        revision = logEntry.getRevision();
        user = logEntry.getAuthor();
        date = logEntry.getDate();
        this.type = type;
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
     * Returns the type of operation this version comes from
     * 
     * @see #CREATED
     * @see #UPDATED
     * @see #DELETED
     */
    public char getType() {
        return type;
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
     * @throws MCRUsageException
     *             if this is a deleted version, which can not be retrieved
     */
    public MCRContent retrieve() throws IOException {
        if (type == DELETED) {
            String msg = "You can not retrieve a deleted version, retrieve a previous version instead";
            throw new MCRUsageException(msg);
        }
        try {
            SVNRepository repository = vm.getStore().getRepository();
            MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream();
            repository.getFile(vm.getStore().getSlotPath(vm.getID()), revision, null, baos);
            baos.close();
            return new MCRByteContent(baos.getBuffer(), 0, baos.size(), getDate().getTime());
        } catch (SVNException e) {
            throw new IOException(e);
        }
    }

    /**
     * Replaces the current version of the metadata object with this version,
     * which means that a new version is created that is identical to this old
     * version. The stored metadata document is updated to this old version of
     * the metadata.
     */
    public void restore() throws IOException, JDOMException {
        vm.update(retrieve());
    }
}
