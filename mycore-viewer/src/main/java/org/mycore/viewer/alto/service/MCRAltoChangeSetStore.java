package org.mycore.viewer.alto.service;

import java.util.List;

import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;

public interface MCRAltoChangeSetStore {

    MCRStoredChangeSet get(String pid);
    MCRStoredChangeSet storeChangeSet(MCRAltoChangeSet changeSet);
    MCRStoredChangeSet updateChangeSet(String pid, MCRAltoChangeSet changeSet);

    List<MCRStoredChangeSet> list();
    List<MCRStoredChangeSet> listBySessionID(String sessionID);
    List<MCRStoredChangeSet> listByDerivate(String derivateID);
    List<MCRStoredChangeSet> list(long start, long count);
    List<MCRStoredChangeSet> listBySessionID(long start, long count, String sessionID);
    List<MCRStoredChangeSet> listByDerivate(long start, long count, String derivateID);

    long count();
    void delete(String pid);
}
