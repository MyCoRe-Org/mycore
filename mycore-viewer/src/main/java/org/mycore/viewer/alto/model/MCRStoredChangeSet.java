package org.mycore.viewer.alto.model;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

@MappedSuperclass
public class MCRStoredChangeSet extends MCRStoredAltoChangeSetMetadata {

    public MCRStoredChangeSet() {
    }

    public MCRStoredChangeSet(String sessionID, String derivateID, String objectTitle, Date created,
        Date applied, String user) {
        super(sessionID, derivateID, objectTitle, created, applied, user);
    }

    @Transient
    @JsonInclude()
    private MCRAltoChangeSet changeSet;

    public MCRAltoChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(MCRAltoChangeSet changeSet) {
        this.changeSet = changeSet;
    }
}
