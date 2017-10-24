package org.mycore.viewer.alto.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class MCRStoredAltoChangeSetMetadata {

    public MCRStoredAltoChangeSetMetadata() {
        this.pid = UUID.randomUUID().toString();
    }

    public MCRStoredAltoChangeSetMetadata(String sessionID, String derivateID, String objectTitle, Date created,
        Date applied, String user) {
        this();
        this.sessionID = sessionID;
        this.derivateID = derivateID;
        this.objectTitle = objectTitle;
        this.created = created;
        this.applied = applied;
        this.user = user;
    }

    @Column(nullable = false)
    private String sessionID;

    @Column(nullable = false)
    private String derivateID;

    @Column(nullable = false)
    private String objectTitle;

    @Column(nullable = false)
    private Date created;

    private Date applied;

    @Id
    private String pid;

    @Column(nullable = false, name = "username")
    private String user;

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDerivateID() {
        return derivateID;
    }

    public void setDerivateID(String derivateID) {
        this.derivateID = derivateID;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getObjectTitle() {
        return objectTitle;
    }

    public void setObjectTitle(String objectTitle) {
        this.objectTitle = objectTitle;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getApplied() {
        return applied;
    }

    public void setApplied(Date applied) {
        this.applied = applied;
    }
}
