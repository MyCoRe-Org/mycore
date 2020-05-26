package org.mycore.restapi.v2.model;

import java.time.Instant;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.restapi.converter.MCRInstantXMLAdapter;

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
