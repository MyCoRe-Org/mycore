package org.mycore.pi.backend;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.mycore.common.MCRCoreVersion;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "identifier", "type" }) })
public class MCRPI implements org.mycore.pi.MCRPIRegistrationInfo {

    private static final long serialVersionUID = 234168232792525611L;

    // unique constraint für identifier type
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String mycoreID;

    @Column(length = 4096)
    private String additional;

    @Column()
    private String service;

    @Column(nullable = false)
    private Date created;

    @Column()
    private Date registered;

    @Column(nullable = false)
    private String mcrVersion;

    @Column(nullable = false)
    private int mcrRevision;

    private MCRPI() {
    }

    public MCRPI(String identifier, String type, String mycoreID, String additional) {
        this(identifier, type, mycoreID.toString(), additional, null, null);
    }

    public MCRPI(String identifier, String type, String mycoreID, String additional, String service, Date registered) {
        this();
        this.identifier = identifier;
        this.type = type;
        this.mycoreID = mycoreID;
        this.additional = additional;
        this.service = service;
        this.registered = registered;
        this.mcrRevision = MCRCoreVersion.getRevision();
        this.mcrVersion = MCRCoreVersion.getVersion();
        this.created = new Date();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getMycoreID() {
        return mycoreID;
    }

    public void setMycoreID(String mycoreID) {
        this.mycoreID = mycoreID;
    }

    @Override
    public String getAdditional() {
        return additional;
    }

    public void setAdditional(String additional) {
        this.additional = additional;
    }

    @Override
    public String getMcrVersion() {
        return mcrVersion;
    }

    public void setMcrVersion(String mcrVersion) {
        this.mcrVersion = mcrVersion;
    }

    @Override
    public int getMcrRevision() {
        return mcrRevision;
    }

    public void setMcrRevision(int mcrRevision) {
        this.mcrRevision = mcrRevision;
    }

    @Override
    public Date getRegistered() {
        return registered;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
