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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Provides information about the revision of a {@link MCRVersionedMetadata} object
 * at the time of object instantiation. This includes the revision number, date,
 * state ({@link MCRMetadataVersionState}), the committer and a reference to the metadata object itself.
 * 
 * @author Frank Lützenkirchen
 * @author Christoph Neidahl (OPNA2608)
 */
@XmlRootElement(name = "revision")
@XmlAccessorType(XmlAccessType.FIELD)
public class MCRMetadataVersion {

    protected static final Logger LOGGER = LogManager.getLogger();

    /**
     * The state of the metadata object as described by this version.
     * 
     * <ul>
     *   <li><p>CREATED - Metadata has been created</p>
     *       <p>(default if the parent {@link MCRMetadataStore} does not implement a version control system)</p></li>
     *   <li>MODIFIED - Metadata has been revised</li>
     *   <li>DELETED - Metadata was deleted</li>
     * </ul>
     * - 
     * @author Christoph Neidahl (OPNA2608)
     *
     */
    public enum MCRMetadataVersionState {
        CREATED,
        UPDATED,
        DELETED
    }

    /**
     * The metadata object this version belongs to.
     */
    @XmlTransient
    private final MCRVersionedMetadata vm;

    /**
     * The revision number of this version.
     */
    @XmlAttribute(name = "r")
    private final long revision;

    /**
     * The user that created this version.
     */
    @XmlAttribute
    private final String user;

    /**
     * The date this version was created on.
     */
    @XmlAttribute
    private final Date date;

    /**
     * Was this version the result of a create, update or delete operation?
     */
    @XmlAttribute()
    private final MCRMetadataVersionState state;

    /**
     * Creates a new metadata version info object.
     * 
     * @param vm
     *            the metadata document this version belongs to
     * @param revision
     *            the version of the metadata document
     * @param user
     *            who committed this version
     * @param date
     *            when was this version created
     * @param state
     *            what object state does it represent ({@link MCRMetadataVersionState})
     */
    public MCRMetadataVersion(MCRVersionedMetadata vm, long revision, String user, Date date,
        MCRMetadataVersionState state) {
        LOGGER.debug("Instantiating version information for {}_{} in revision {}.", vm.getStore().id,
            vm.getStore().createIDWithLeadingZeros(vm.getID()), revision);
        this.vm = vm;
        this.revision = revision;
        this.user = user;
        this.date = date;
        this.state = state;
    }

    /**
     * Returns the metadata object this version belongs to.
     * 
     * @return the metadata object this version belongs to
     */
    public MCRVersionedMetadata getMetadataObject() {
        return vm;
    }

    /**
     * Returns the type of operation this version comes from.
     * 
     * This method is deprecated, please use the new enum-based method {@link getState} instead.
     * 
     * @see #CREATED
     * @see #UPDATED
     * @see #DELETED
     * @return {@link #state} shortened to the first character
     */
    @Deprecated
    public char getType() {
        return state.toString().charAt(0);
    }

    /**
     * Returns the state represented by this version.
     * 
     * @see MCRMetadataVersionState
     * @return the state represented by this version
     */
    public MCRMetadataVersionState getState() {
        return state;
    }

    /**
     * Returns the revision number of this version.
     * 
     * @return the revision number of this version
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Returns the user that created this version.
     * 
     * @return the user that created this version
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the date and time this version was created.
     * 
     * @return the date and time this version was created
     */
    public Date getDate() {
        return date;
    }

    /**
     * Retrieves this version of the metadata.
     * 
     * @return the metadata document as it was in this version
     * @throws MCRUsageException
     *             if this is a deleted version, which can not be retrieved
     */
    public MCRContent retrieve() throws IOException {
        if (state == MCRMetadataVersionState.DELETED) {
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
