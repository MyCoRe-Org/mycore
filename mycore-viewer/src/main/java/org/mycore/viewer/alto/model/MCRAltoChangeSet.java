package org.mycore.viewer.alto.model;

import java.util.ArrayList;
import java.util.List;

public class MCRAltoChangeSet {

    private List<MCRAltoWordChange> wordChanges;

    private String derivateID;

    public MCRAltoChangeSet(String derivateID, List<MCRAltoWordChange> wordChanges) {
        this.derivateID = derivateID;
        this.wordChanges = wordChanges;
    }

    public MCRAltoChangeSet() {
        this.wordChanges = new ArrayList<>();
    }

    public List<MCRAltoWordChange> getWordChanges() {
        return wordChanges;
    }

    public String getDerivateID() {
        return derivateID;
    }

    public void setDerivateID(String derivateID) {
        this.derivateID = derivateID;
    }
}
