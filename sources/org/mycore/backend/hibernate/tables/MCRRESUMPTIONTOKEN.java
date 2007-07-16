package org.mycore.backend.hibernate.tables;

import java.io.Serializable;
import java.util.Date;
import java.sql.Blob;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/** @author Hibernate CodeGenerator */
public class MCRRESUMPTIONTOKEN implements Serializable {

    private static final long serialVersionUID = 5750307015900672047L;

    /** identifier field */
    private String resumptionTokenID;

    /** nullable persistent field */
    private String prefix;

    /** nullable persistent field */
    private long completeSize;

    /** nullable persistent field */
    private Date created;

    /** nullable persistent field */
    private String instance;

    /** nullable persistent field */
    private Blob hitBlob;

    /** full constructor */
    public MCRRESUMPTIONTOKEN(String resumptionTokenID, String prefix, long completeSize, Date created, String instance, Blob hitBlob) {
        this.resumptionTokenID = resumptionTokenID;
        this.prefix = prefix;
        this.completeSize = completeSize;
        this.created = created;
        this.instance = instance;
        this.hitBlob = hitBlob;
    }

    /** default constructor */
    public MCRRESUMPTIONTOKEN() {
    }

    /** minimal constructor */
    public MCRRESUMPTIONTOKEN(String resumptionTokenID, Blob hitBlob) {
        this.resumptionTokenID = resumptionTokenID;
        this.hitBlob = hitBlob;
    }

    public String getResumptionTokenID() {
        return this.resumptionTokenID;
    }

    public void setResumptionTokenID(String resumptionTokenID) {
        this.resumptionTokenID = resumptionTokenID;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getCompleteSize() {
        return this.completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getInstance() {
        return this.instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Blob getHitBlob() {
        return this.hitBlob;
    }

    public void setHitBlob(Blob hitBlob) {
        this.hitBlob = hitBlob;
    }
    public byte[] getHitByteArray(){
       return MCRBlob.getBytes(this.hitBlob);
    }

    public void setHitByteArray(byte[] hitByte) {
       this.hitBlob = new MCRBlob(hitByte);
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("resumptionTokenID", getResumptionTokenID())
            .toString();
    }

    public boolean equals(Object other) {
        if ( !(other instanceof MCRRESUMPTIONTOKEN) ) return false;
        MCRRESUMPTIONTOKEN castOther = (MCRRESUMPTIONTOKEN) other;
        return new EqualsBuilder()
            .append(this.getResumptionTokenID(), castOther.getResumptionTokenID())
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(getResumptionTokenID())
            .toHashCode();
    }

}