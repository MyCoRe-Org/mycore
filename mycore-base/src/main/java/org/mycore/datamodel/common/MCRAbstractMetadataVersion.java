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

package org.mycore.datamodel.common;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;

import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Provides information about a stored version of metadata and allows to
 * retrieve that version from SVN
 * 
 * @author Frank Lützenkirchen
 */
@XmlRootElement(name = "revision")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class MCRAbstractMetadataVersion<T> {

    protected enum Type {
        created(MCRAbstractMetadataVersion.CREATED),
        modified(MCRAbstractMetadataVersion.UPDATED),
        deleted(MCRAbstractMetadataVersion.DELETED);

        private final char charValue;

        Type(char a) {
            this.charValue = a;
        }

        public static Type fromValue(char a) {
            return Stream.of(values()).filter(t -> t.charValue == a)
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
        }
    }
    
    /**
     * The metadata document this version belongs to
     */
    @XmlTransient
    protected T vm;

    /**
     * The revision number of this version
     */
    @XmlAttribute(name = "r")
    protected String revision;

    /**
     * The user that created this version
     */
    @XmlAttribute
    protected String user;

    /**
     * The date this version was created
     */
    @XmlAttribute
    protected Date date;

    /**
     * Was this version result of a create, update or delete?
     */
    @XmlAttribute()
    protected Type type;

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

    //required for JAXB serialization
    @SuppressWarnings("unused")
    private MCRAbstractMetadataVersion() {
    }

    /**
     * Creates a new metadata version info object
     * 
     * @param vm
     *            the metadata document this version belongs to
     * @param revision
     *            the revision of this object, serialised as a string
     * @param user
     *            the user that created this revision
     * @param date
     *            the date on which this revision was created
     * @param type
     *            the type of commit
     */
    public MCRAbstractMetadataVersion(T vm, String revision, String user, Date date, char type) {
        this.vm = vm;
        this.revision = revision;
        this.user = user;
        this.date = date;
        this.type = Type.fromValue(type);
    }

    /**
     * Returns the metadata object this version belongs to
     * 
     * @return the metadata object this version belongs to
     */
    public T getMetadataObject() {
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
        return type.charValue;
    }

    /**
     * Returns the SVN revision number of this version
     * 
     * @return the SVN revision number of this version
     */
    public String getRevision() {
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
    abstract public MCRContent retrieve() throws IOException;

    /**
     * Replaces the current version of the metadata object with this version,
     * which means that a new version is created that is identical to this old
     * version. The stored metadata document is updated to this old version of
     * the metadata.
     */
    abstract public void restore() throws IOException, JDOMException;
}
