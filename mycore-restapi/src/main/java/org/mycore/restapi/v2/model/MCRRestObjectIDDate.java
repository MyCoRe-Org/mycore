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

package org.mycore.restapi.v2.model;

import java.time.Instant;

import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.restapi.converter.MCRInstantXMLAdapter;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * wraps an MCRObjectIDDate to return it via REST API
 * and uses Instant instead of Date
 * 
 * @author Robert Stephan
 *
 */
@XmlRootElement(name = "mycoreobject")
@XmlType(propOrder = { "id", "lastModified" })
public class MCRRestObjectIDDate {
    protected Instant lastModified;

    protected String id;

    protected MCRRestObjectIDDate() {
        //required for JAXB serialization
        super();
    }

    public MCRRestObjectIDDate(MCRObjectIDDate idDate) {
        super();
        id = idDate.getId();
        lastModified = idDate.getLastModified().toInstant();
    }

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(value = MCRInstantXMLAdapter.class)
    public Instant getLastModified() {
        return lastModified;
    }

    @XmlAttribute(required = true)
    public String getId() {
        return id;
    }

    protected void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    protected void setId(String id) {
        this.id = id;
    }
}
