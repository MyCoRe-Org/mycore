/**
 * 
 */
package org.mycore.services.handle.hibernate.tables;

import org.mycore.services.handle.MCRIHandleProvider;

/**
 * Class wraps a handle. 
 * 
 * @author shermann
 * 
 * @see http://www.handle.net/rfc/rfc3651.html
 */
public class MCRHandle {

    /**
     * Hibernate primary key
     */
    private long id;

    /**
     * The schema part of the handle
     */
    private String schema;

    /**
     * The naming authority part of the handle
     */
    private String namingAuthority;

    /**
     * The naming authority segment part of the handle
     */
    private String namingAuthoritySegment;

    /**
     * The local part of the handle
     */
    private String localName;

    /**
     * mycore related, the path of the file addressed by this handle
     */
    private String path;

    /**
     * mycore related, the mcrid implicitly addressed by this handle (a derivate or a mycore object) 
     * */
    private String mcrid;

    /**
     * The checksum of this handle, must be calulated by a {@link MCRIHandleProvider}.
     * */
    private int checksum;

    /**
     * 
     */
    private String objectSignature;

    /**
     * 
     */
    private String messageSignature;

    /**
     * The default constructor. Leaves all fields unset.
     */
    public MCRHandle() {
        checksum = -1;
    }

    /**
     * @param namingAuthority
     * @param namingAuthoritySegment
     * @param localName
     */
    public MCRHandle(String namingAuthority, String namingAuthoritySegment, String localName) {
        this(null, namingAuthority, namingAuthoritySegment, localName);
    }

    /**
     * @param schema
     * @param namingAuthority
     * @param namingAuthoritySegment
     * @param localName
     */
    public MCRHandle(String protocol, String namingAuthority, String namingAuthoritySegment, String localName) {
        this();
        this.schema = protocol;
        this.namingAuthority = namingAuthority;
        this.namingAuthoritySegment = namingAuthoritySegment;
        this.localName = localName;
    }

    /**
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return the namingAuthority
     */
    public String getNamingAuthority() {
        return namingAuthority;
    }

    /**
     * @param namingAuthority the namingAuthority to set
     */
    public void setNamingAuthority(String namingAuthority) {
        this.namingAuthority = namingAuthority;
    }

    /**
     * @return the namingAuthoritySegment
     */
    public String getNamingAuthoritySegment() {
        return namingAuthoritySegment;
    }

    /**
     * @param namingAuthoritySegment the namingAuthoritySegment to set
     */
    public void setNamingAuthoritySegment(String namingAuthoritySegment) {
        this.namingAuthoritySegment = namingAuthoritySegment;
    }

    /**
     * @return the localName
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * @param localName the localName to set
     */
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the id of a mycore derivate or a mycore object.
     * 
     * @return the mcrid
     */
    public String getMcrid() {
        return mcrid;
    }

    /**
     * Sets the id to a mycore derivate or a mycore object.
     * 
     * @param mcrid the mcrid to set
     */
    public void setMcrid(String mcrid) {
        this.mcrid = mcrid;
    }

    /**
     * @return the checksum
     */
    public int getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the objectSignature
     */
    public String getObjectSignature() {
        return objectSignature;
    }

    /**
     * @param objectSignature the objectSignature to set
     */
    public void setObjectSignature(String objectSignature) {
        this.objectSignature = objectSignature;
    }

    /**
     * @return the messageSignature
     */
    public String getMessageSignature() {
        return messageSignature;
    }

    /**
     * @param messageSignature the messageSignature to set
     */
    public void setMessageSignature(String messageSignature) {
        this.messageSignature = messageSignature;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        if (localName == null) {
            if (objectSignature != null) {
                return objectSignature;
            }
            return null;
        }

        StringBuilder b = new StringBuilder();
        if (schema != null && schema.length() > 0) {
            b.append(schema + ":");
        }
        b.append(namingAuthority);
        if (namingAuthoritySegment != null && namingAuthoritySegment.length() > 0) {
            b.append("." + namingAuthoritySegment);
        }

        b.append("/" + localName);

        return b.toString();
    }
}
