package org.mycore.viewer.alto.service.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRDBStoredChangeSet;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;
import org.mycore.viewer.alto.service.MCRDerivateTitleResolver;

public class MCRJPAAltoChangeSetStore implements MCRAltoChangeSetStore {

    private MCRDerivateTitleResolver titleResolver = MCRInjectorConfig.injector()
        .getInstance(MCRDerivateTitleResolver.class);

    @Override
    public MCRStoredChangeSet get(String pid) {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.ByPID", MCRDBStoredChangeSet.class)
            .setParameter("pid", pid)
            .getSingleResult();
    }

    @Override
    public MCRStoredChangeSet storeChangeSet(MCRAltoChangeSet changeSet) {
        String objectTitle = titleResolver.resolveTitle(changeSet.getDerivateID());
        String userID = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();

        MCRDBStoredChangeSet storedChangeSet = new MCRDBStoredChangeSet(MCRSessionMgr.getCurrentSessionID(),
            changeSet.getDerivateID(), objectTitle, new Date(), null,
            userID, changeSet);
        MCRHIBConnection.instance().getSession().save(storedChangeSet);

        return storedChangeSet;
    }

    @Override
    public MCRStoredChangeSet updateChangeSet(String pid, MCRAltoChangeSet changeSet) {
        MCRStoredChangeSet storedChangeSet = get(pid);
        storedChangeSet.setChangeSet(changeSet);
        return storedChangeSet;
    }

    @Override
    public List<MCRStoredChangeSet> list() {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied", MCRDBStoredChangeSet.class)
            .getResultList());
    }

    @Override
    public List<MCRStoredChangeSet> listBySessionID(String sessionID) {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied.bySID", MCRDBStoredChangeSet.class)
            .setParameter("sid", sessionID)
            .getResultList());
    }

    @Override
    public List<MCRStoredChangeSet> listByDerivate(String derivateID) {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied.byDerivate", MCRDBStoredChangeSet.class)
            .setParameter("derivateID", derivateID)
            .getResultList());
    }

    @Override
    public List<MCRStoredChangeSet> list(long start, long count) {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied", MCRDBStoredChangeSet.class)
            .setFirstResult(Math.toIntExact(start))
            .setMaxResults(Math.toIntExact(count))
            .getResultList());
    }

    @Override
    public List<MCRStoredChangeSet> listBySessionID(long start, long count, String sessionID) {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied.bySID", MCRDBStoredChangeSet.class)
            .setParameter("sid", sessionID)
            .setFirstResult(Math.toIntExact(start))
            .setMaxResults(Math.toIntExact(count))
            .getResultList());
    }

    @Override
    public List<MCRStoredChangeSet> listByDerivate(long start, long count, String derivateID) {
        return castList(MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Get.ALTOCS.Unapplied.byDerivate", MCRDBStoredChangeSet.class)
            .setParameter("derivateID", derivateID)
            .setFirstResult(Math.toIntExact(start))
            .setMaxResults(Math.toIntExact(count))
            .getResultList());
    }

    @Override
    public long count() {
        return MCREntityManagerProvider
            .getCurrentEntityManager()
            .createNamedQuery("Count.ALTOCS.Unapplied", Number.class)
            .getSingleResult()
            .longValue();
    }

    @Override
    public void delete(String pid) {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("Delete.ALTOCS.byPID")
            .setParameter("pid", pid)
            .executeUpdate();
    }

    private List<MCRStoredChangeSet> castList(List<MCRDBStoredChangeSet> storedChangeSet) {
        return storedChangeSet.stream().map(MCRStoredChangeSet.class::cast).collect(Collectors.toList());
    }
}
