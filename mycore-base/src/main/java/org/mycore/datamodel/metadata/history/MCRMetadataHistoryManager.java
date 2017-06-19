/**
 * 
 */
package org.mycore.datamodel.metadata.history;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRMetadataHistoryManager extends MCREventHandlerBase {

    public static Optional<MCRObjectID> getHighestStoredID(String project, String type) {
        String looksLike = Objects.requireNonNull(project) + "\\_" + Objects.requireNonNull(type) + "%";
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRObjectID> query = em.createNamedQuery("MCRMetaHistory.getHighestID", MCRObjectID.class);
        query.setParameter("looksLike", looksLike);
        return Optional.ofNullable(query.getSingleResult());
    }

    public static Optional<Instant> getHistoryStart() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Instant> query = em.createNamedQuery("MCRMetaHistory.getFirstDate", Instant.class);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public static Map<MCRObjectID, Instant> getDeletedItems(Instant from, Optional<Instant> until) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRMetaHistoryItem> query = em.createNamedQuery("MCRMetaHistory.getLastEventByID",
            MCRMetaHistoryItem.class);
        query.setParameter("from", from);
        query.setParameter("until", until.orElseGet(Instant::now));
        query.setParameter("eventType", MCRMetadataHistoryEventType.Delete);
        return query.getResultList()
            .stream()
            .collect(Collectors.toMap(MCRMetaHistoryItem::getId, MCRMetaHistoryItem::getTime));
    }

    public static Optional<Instant> getLastDeletedDate(MCRObjectID identifier) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Instant> query = em.createNamedQuery("MCRMetaHistory.getLastOfType", Instant.class);
        query.setParameter("id", identifier);
        query.setParameter("type", MCRMetadataHistoryEventType.Delete);
        return Optional.ofNullable(query.getSingleResult());
    }

    private void createNow(MCRObjectID id) {
        store(MCRMetaHistoryItem.createdNow(id));
    }

    private void deleteNow(MCRObjectID id) {
        store(MCRMetaHistoryItem.deletedNow(id));
    }

    private void store(MCRMetaHistoryItem item) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(item);
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        createNow(obj.getId());
        if (objectIsHidden(obj)) {
            deleteNow(obj.getId());
        }
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRObject oldVersion = (MCRObject) evt.get(MCREvent.OBJECT_OLD_KEY);
        if (updateRequired(oldVersion, obj)) {
            if (objectIsHidden(obj)) {
                deleteNow(obj.getId());
            } else {
                createNow(obj.getId());
            }
        }
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        deleteNow(obj.getId());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        createNow(der.getId());
        if (!der.getDerivate().isDisplayEnabled()) {
            deleteNow(der.getId());
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        MCRDerivate oldVersion = (MCRDerivate) evt.get(MCREvent.DERIVATE_OLD_KEY);
        if (updateRequired(oldVersion, der)) {
            if (der.getDerivate().isDisplayEnabled()) {
                createNow(der.getId());
            } else {
                deleteNow(der.getId());
            }
        }
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        deleteNow(der.getId());
    }

    private boolean updateRequired(MCRObject oldVersion, MCRObject newVersion) {
        return objectIsHidden(oldVersion) != objectIsHidden(newVersion);
    }

    static boolean objectIsHidden(MCRObject obj) {
        MCRCategoryID state = obj.getService().getState();
        if (state == null) {
            LogManager.getLogger().debug("{} is visible as it does not use a service state.", obj.getId());
            return false;
        }
        boolean hidden = !"published".equals(state.getID());
        LogManager.getLogger().debug("{} is hidden due to service state '{}': {}", obj.getId(), state, hidden);
        return hidden;
    }

    private boolean updateRequired(MCRDerivate oldVersion, MCRDerivate newVersion) {
        return oldVersion.getDerivate().isDisplayEnabled() != newVersion.getDerivate().isDisplayEnabled();
    }

}
