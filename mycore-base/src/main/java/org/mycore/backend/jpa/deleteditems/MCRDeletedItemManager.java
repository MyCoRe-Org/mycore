package org.mycore.backend.jpa.deleteditems;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRDeletedItemManager {

    /**
     * @param identifier
     *            the identifier of the MCRObject or MCRDerivate
     * @param dateDeleted
     *            the current date
     */
    public static final void addEntry(String identifier, ZonedDateTime dateDeleted) throws MCRPersistenceException {
        if (identifier == null) {
            throw new MCRPersistenceException("The identifier is null.");
        }
        if (dateDeleted == null) {
            throw new MCRPersistenceException("The deleted date parameter is null");
        }

        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRDELETEDITEMSPK pk = new MCRDELETEDITEMSPK(identifier, dateDeleted);
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        MCRDELETEDITEMS tab = entityManager.find(MCRDELETEDITEMS.class, pk);
        boolean create = tab == null;
        if (create) {
            tab = new MCRDELETEDITEMS();
            tab.setKey(pk);
        }
        tab.setUserid(mcrSession.getUserInformation().getUserID());
        tab.setIp(mcrSession.getCurrentIP());

        if (create) {
            LogManager.getLogger().debug("Inserting into MCRDELETEDITEMS table");
            entityManager.persist(tab);
        }
    }

    public static final Optional<ZonedDateTime> getFirstDate() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ZonedDateTime> query = cb.createQuery(ZonedDateTime.class);
        Root<MCRDELETEDITEMS> deletedItems = query.from(MCRDELETEDITEMS.class);
        try {
            return Optional.ofNullable(
                em.createQuery(
                    query.select(
                        cb.least(deletedItems.get(MCRDELETEDITEMS_.key).get(MCRDELETEDITEMSPK_.dateDeleted))))
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public static List<String> getDeletedItems(ZonedDateTime from, Optional<ZonedDateTime> until) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRDELETEDITEMS> deletedItems = query.from(MCRDELETEDITEMS.class);
        Path<MCRDELETEDITEMSPK> key = deletedItems.get(MCRDELETEDITEMS_.key);
        Path<ZonedDateTime> dateDeleted = key.get(MCRDELETEDITEMSPK_.dateDeleted);
        return em.createQuery(
            query
                .select(key.get(MCRDELETEDITEMSPK_.identifier))
                .where(
                    until
                        .map(u -> cb.between(dateDeleted, from, u))
                        .orElse(cb.greaterThan(dateDeleted, from))))
            .getResultList();
    }

    public static Optional<ZonedDateTime> getLastDeletedDate(String identifier) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ZonedDateTime> query = cb.createQuery(ZonedDateTime.class);
        Root<MCRDELETEDITEMS> deletedItems = query.from(MCRDELETEDITEMS.class);
        try {
            Path<MCRDELETEDITEMSPK> key = deletedItems.get(MCRDELETEDITEMS_.key);
            return Optional.of(
                em.createQuery(
                    query
                        .select(cb.greatest(key.get(MCRDELETEDITEMSPK_.dateDeleted)))
                        .where(cb.equal(key.get(MCRDELETEDITEMSPK_.identifier), identifier)))
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

}
