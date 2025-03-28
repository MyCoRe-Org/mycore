/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.metadata.history;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRMetadataHistoryManager extends MCREventHandlerBase {

    private static final String QUERY_PARAM_KIND = "kind";

    private static final String QUERY_PARAM_AFTER_ID = "afterID";

    private static final String QUERY_PARAM_TYPE = "type";

    private static final String QUERY_PARAM_ID = "id";

    private static final String QUERY_PARAM_FROM = "from";

    private static final String QUERY_PARAM_UNTIL = "until";

    private static final String QUERY_PARAM_EVENT_TYPE = "eventType";

    private static final String QUERY_PARAM_LOOKS_LIKE = "looksLike";

    public static Optional<MCRObjectID> getHighestStoredID(String project, String type) {
        String looksLike = Objects.requireNonNull(project) + "\\_" + Objects.requireNonNull(type) + "%";
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRObjectID> query = em.createNamedQuery("MCRMetaHistory.getHighestID", MCRObjectID.class);
        query.setParameter(QUERY_PARAM_LOOKS_LIKE, looksLike);
        return Optional.ofNullable(query.getSingleResult());
    }

    public static Optional<Instant> getHistoryStart() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Instant> query = em.createNamedQuery("MCRMetaHistory.getFirstDate", Instant.class);
        return Optional.ofNullable(query.getSingleResult());
    }

    public static Map<MCRObjectID, Instant> getDeletedItems(Instant from, Optional<Instant> until) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRMetaHistoryItem> query = em.createNamedQuery("MCRMetaHistory.getLastEventByID",
            MCRMetaHistoryItem.class);
        query.setParameter(QUERY_PARAM_FROM, from);
        query.setParameter(QUERY_PARAM_UNTIL, until.orElseGet(Instant::now));
        query.setParameter(QUERY_PARAM_EVENT_TYPE, MCRMetadataHistoryEventType.DELETE.getAbbr());
        return query.getResultList()
            .stream()
            .collect(Collectors.toMap(MCRMetaHistoryItem::getId, MCRMetaHistoryItem::getTime));
    }

    public static Optional<Instant> getLastDeletedDate(MCRObjectID identifier) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Instant> query = em.createNamedQuery("MCRMetaHistory.getLastOfType", Instant.class);
        query.setParameter(QUERY_PARAM_ID, identifier);
        query.setParameter(QUERY_PARAM_TYPE, MCRMetadataHistoryEventType.DELETE.getAbbr());
        return Optional.ofNullable(query.getSingleResult());
    }

    public static List<MCRMetaHistoryItem> listNextObjectIDs(MCRObjectID afterID, int maxResults) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRMetaHistoryItem> query = em.createNamedQuery("MCRMetaHistory.getNextActiveIDs",
            MCRMetaHistoryItem.class);
        query.setParameter(QUERY_PARAM_AFTER_ID, afterID);
        query.setParameter(QUERY_PARAM_KIND, "object");
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public static List<MCRMetaHistoryItem> listNextDerivateIDs(MCRObjectID afterID, int maxResults) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRMetaHistoryItem> query = em.createNamedQuery("MCRMetaHistory.getNextActiveIDs",
            MCRMetaHistoryItem.class);
        query.setParameter(QUERY_PARAM_AFTER_ID, afterID);
        query.setParameter(QUERY_PARAM_KIND, "derivate");
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public static Long countObjectIDs() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Long> query = em.createNamedQuery("MCRMetaHistory.countActiveIDs",
            Long.class);
        query.setParameter(QUERY_PARAM_KIND, "object");
        return query.getSingleResult();
    }

    public static Long countDerivateIDs() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Long> query = em.createNamedQuery("MCRMetaHistory.countActiveIDs",
            Long.class);
        query.setParameter(QUERY_PARAM_KIND, "derivate");
        return query.getSingleResult();
    }

    private void createNow(MCRObjectID id) {
        store(MCRMetaHistoryItem.now(id, MCRMetadataHistoryEventType.CREATE));
    }

    private void deleteNow(MCRObjectID id) {
        store(MCRMetaHistoryItem.now(id, MCRMetadataHistoryEventType.DELETE));
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
        if (!MCRAccessManager.checkDerivateDisplayPermission(der.getId().toString())) {
            deleteNow(der.getId());
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        MCRDerivate oldVersion = (MCRDerivate) evt.get(MCREvent.DERIVATE_OLD_KEY);
        if (updateRequired(oldVersion, der)) {
            if (MCRAccessManager.checkDerivateDisplayPermission(der.getId().toString())) {
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
            LogManager.getLogger().debug("{} is visible as it does not use a service state.", obj::getId);
            return false;
        }
        boolean hidden = !"published".equals(state.getId());
        LogManager.getLogger().debug("{} is hidden due to service state '{}': {}",
            obj::getId, () -> state, () -> hidden);
        return hidden;
    }

    private boolean updateRequired(MCRDerivate oldVersion, MCRDerivate newVersion) {
        return MCRAccessManager.checkDerivateDisplayPermission(oldVersion.getId().toString()) != MCRAccessManager
            .checkDerivateDisplayPermission(newVersion.getId().toString());
    }

}
