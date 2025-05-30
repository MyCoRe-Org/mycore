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

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRCreatorCache;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.util.concurrent.MCRTransactionableCallable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MCRCommandGroup(name = "Metadata history")
public class MCRMetadataHistoryCommands {

    @MCRCommand(syntax = "clear metadata history of base {0}",
        help = "clears metadata history of all objects with base id {0}")
    public static void clearHistory(String baseId) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<MCRMetaHistoryItem> delete = cb.createCriteriaDelete(MCRMetaHistoryItem.class);
        Root<MCRMetaHistoryItem> item = delete.from(MCRMetaHistoryItem.class);
        int rowsDeleted = em.createQuery(
            delete.where(
                cb.like(item.get(MCRMetaHistoryItem_.id).as(String.class), baseId + "_".replace("_", "$_") + '%', '$')))
            .executeUpdate();
        LogManager.getLogger().info("Deleted {} items in history of {}", rowsDeleted, baseId);
    }

    @MCRCommand(syntax = "clear metadata history of id {0}",
        help = "clears metadata history of object/derivate with id {0}")
    public static void clearSingleHistory(String mcrId) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<MCRMetaHistoryItem> delete = cb.createCriteriaDelete(MCRMetaHistoryItem.class);
        Root<MCRMetaHistoryItem> item = delete.from(MCRMetaHistoryItem.class);
        int rowsDeleted = em.createQuery(
            delete.where(cb.equal(item.get(MCRMetaHistoryItem_.id).as(String.class), mcrId)))
            .executeUpdate();
        LogManager.getLogger().info("Deleted {} items in history of {}", rowsDeleted, mcrId);
    }

    @MCRCommand(syntax = "clear metadata history completely", help = "clears metadata history completely")
    public static List<String> clearHistory() {
        return MCRXMLMetadataManager.getInstance()
            .getObjectBaseIds()
            .stream()
            .map(s -> "clear metadata history of base " + s)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "build metadata history completely", help = "build metadata history completely")
    public static List<String> buildHistory() {
        return MCRXMLMetadataManager.getInstance()
            .getObjectBaseIds()
            .stream()
            .map(s -> "build metadata history of base " + s)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "build metadata history of base {0}",
        help = "build metadata history of all objects with base id {0}")
    public static List<String> buildHistory(String baseId) {
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.getInstance();
        mm.verifyStore(baseId);
        ExecutorService executorService = Executors.newWorkStealingPool();
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        String[] idParts = MCRObjectID.getIDParts(baseId);
        if (idParts.length != 2) {
            throw new MCRUsageException("Valid base ID required!");
        }
        int maxId = mm.getHighestStoredID(idParts[0], idParts[1]);
        AtomicInteger completed = new AtomicInteger(maxId);
        IntStream.rangeClosed(1, maxId)
            .parallel()
            .mapToObj(i -> MCRObjectID.formatID(baseId, i))
            .map(MCRObjectID::getInstance)
            .map(id -> new MCRTransactionableCallable<>(Executors.callable(() -> {
                EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
                getHistoryItems(id).sequential().forEach(em::persist);
                completed.decrementAndGet();
            }), currentSession))
            .forEach(executorService::submit);
        executorService.shutdown();
        boolean waitToFinish = true;
        while (!executorService.isTerminated() && waitToFinish) {
            LogManager.getLogger().info("Waiting for history of {} objects/derivates.", completed::get);
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                waitToFinish = false;
            }
        }
        return Collections.emptyList();
    }

    @MCRCommand(syntax = "build metadata history of id {0}",
        help = "build metadata history of object/derivate with id {0}")
    public static void buildSingleHistory(String mcrId) {
        MCRObjectID objId = MCRObjectID.getInstance(mcrId);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        getHistoryItems(objId).sequential().forEach(em::persist);
    }

    private static Stream<MCRMetaHistoryItem> getHistoryItems(MCRObjectID objId) {
        return objId.getTypeId().equals(MCRDerivate.OBJECT_TYPE) ? buildDerivateHistory(objId)
            : buildObjectHistory(objId);
    }

    private static Stream<MCRMetaHistoryItem> buildDerivateHistory(MCRObjectID derId) {
        try {
            List<? extends MCRAbstractMetadataVersion<?>> versions = MCRXMLMetadataManager.getInstance()
                .listRevisions(derId);
            if (versions == null || versions.isEmpty()) {
                return buildSimpleDerivateHistory(derId);
            } else {
                return buildDerivateHistory(derId, versions);
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Error while getting history of {}", derId);
            return Stream.empty();
        }
    }

    private static Stream<MCRMetaHistoryItem> buildObjectHistory(MCRObjectID objId) {
        try {
            List<? extends MCRAbstractMetadataVersion<?>> versions = MCRXMLMetadataManager.getInstance()
                .listRevisions(objId);
            if (versions == null || versions.isEmpty()) {
                return buildSimpleObjectHistory(objId);
            } else {
                return buildObjectHistory(objId, versions);
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Error while getting history of {}", objId);
            return Stream.empty();
        }
    }

    private static Stream<MCRMetaHistoryItem> buildSimpleDerivateHistory(MCRObjectID derId) throws IOException {
        LogManager.getLogger().debug("Store of {} has no old revisions. History rebuild is limited", derId);
        if (MCRMetadataManager.exists(derId)) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derId);
            Instant lastModified = Instant
                .ofEpochMilli(MCRXMLMetadataManager.getInstance().getLastModified(derId));
            String creator;
            try {
                creator = MCRCreatorCache.getCreator(der.getId());
            } catch (ExecutionException e) {
                LogManager.getLogger().warn("Error while getting creator of {}", derId, e);
                creator = null;
            }
            String user = Optional.ofNullable(creator)
                .orElseGet(MCRSystemUserInformation.SYSTEM_USER::getUserID);
            MCRMetaHistoryItem create = create(derId,
                user,
                lastModified);
            boolean objectIsHidden = !MCRAccessManager.checkDerivateDisplayPermission(derId.toString());
            if (objectIsHidden) {
                return Stream.of(create, delete(derId, user, lastModified.plusMillis(1)));
            }
            return Stream.of(create);
        } else {
            return Stream.of(delete(derId, null, Instant.now()));
        }
    }

    private static Stream<MCRMetaHistoryItem> buildSimpleObjectHistory(MCRObjectID objId) throws IOException {
        LogManager.getLogger().debug("Store of {} has no old revisions. History rebuild is limited", objId);
        if (MCRMetadataManager.exists(objId)) {
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(objId);
            Instant lastModified = Instant
                .ofEpochMilli(MCRXMLMetadataManager.getInstance().getLastModified(objId));
            String creator;
            try {
                creator = MCRCreatorCache.getCreator(obj.getId());
            } catch (ExecutionException e) {
                LogManager.getLogger().warn("Error while getting creator of {}", objId, e);
                creator = null;
            }
            String user = Optional.ofNullable(creator)
                .orElseGet(MCRSystemUserInformation.SYSTEM_USER::getUserID);
            MCRMetaHistoryItem create = create(objId, user, lastModified);
            boolean objectIsHidden = MCRMetadataHistoryManager.objectIsHidden(obj);
            if (objectIsHidden) {
                return Stream.of(create, delete(objId, user, lastModified.plusMillis(1)));
            }
            return Stream.of(create);
        } else {
            return Stream.of(delete(objId, null, Instant.now()));
        }
    }

    private static Stream<MCRMetaHistoryItem> buildDerivateHistory(MCRObjectID derId,
        List<? extends MCRAbstractMetadataVersion<?>> versions)
        throws IOException {
        boolean exist = false;
        LogManager.getLogger().debug("Complete history rebuild of {} should be possible", derId);
        List<MCRMetaHistoryItem> items = new ArrayList<>(100);
        for (MCRAbstractMetadataVersion<?> version : versions) {
            String user = version.getUser();
            Instant revDate = version.getDate().toInstant();
            if (version.getType() == MCRMetadataVersion.DELETED) {
                if (exist) {
                    items.add(delete(derId, user, revDate));
                    exist = false;
                }
            } else {
                //created or updated
                int timeOffset = 0;
                if (version.getType() == MCRMetadataVersion.CREATED && !exist) {
                    items.add(create(derId, user, revDate));
                    timeOffset = 1;
                    exist = true;
                }
                try {
                    MCRDerivate derivate = new MCRDerivate(version.retrieve().asXML());
                    boolean derivateIsHidden = !MCRAccessManager
                        .checkDerivateDisplayPermission(derivate.getId().toString());
                    if (derivateIsHidden && exist) {
                        items.add(delete(derId, user, revDate.plusMillis(timeOffset)));
                        exist = false;
                    } else if (!derivateIsHidden && !exist) {
                        items.add(create(derId, user,
                            revDate.plusMillis(timeOffset)));
                        exist = true;
                    }
                } catch (JDOMException e) {
                    LogManager.getLogger()
                        .error(() -> "Error while reading revision " + version.getRevision() + " of " + derId, e);
                }
            }
        }
        return items.stream();
    }

    private static Stream<MCRMetaHistoryItem> buildObjectHistory(MCRObjectID objId,
        List<? extends MCRAbstractMetadataVersion<?>> versions)
        throws IOException {
        boolean exist = false;
        LogManager.getLogger().debug("Complete history rebuild of {} should be possible", objId);
        List<MCRMetaHistoryItem> items = new ArrayList<>(100);
        for (MCRAbstractMetadataVersion<?> version : versions) {
            String user = version.getUser();
            Instant revDate = version.getDate().toInstant();
            if (version.getType() == MCRMetadataVersion.DELETED) {
                if (exist) {
                    items.add(delete(objId, user, revDate));
                    exist = false;
                }
            } else {
                //created or updated
                int timeOffset = 0;
                if (version.getType() == MCRMetadataVersion.CREATED && !exist) {
                    items.add(create(objId, user, revDate));
                    timeOffset = 1;
                    exist = true;
                }
                try {
                    MCRObject obj = new MCRObject(version.retrieve().asXML());
                    boolean objectIsHidden = MCRMetadataHistoryManager.objectIsHidden(obj);
                    if (objectIsHidden && exist) {
                        items.add(delete(objId, user, revDate.plusMillis(timeOffset)));
                        exist = false;
                    } else if (!objectIsHidden && !exist) {
                        items.add(create(objId, user, revDate.plusMillis(timeOffset)));
                        exist = true;
                    }
                } catch (JDOMException e) {
                    LogManager.getLogger()
                        .error(() -> "Error while reading revision " + version.getRevision() + " of " + objId, e);
                }
            }
        }
        return items.stream();
    }

    private static MCRMetaHistoryItem create(MCRObjectID mcrid, String author, Instant instant) {
        return newHistoryItem(mcrid, author, instant, MCRMetadataHistoryEventType.CREATE);
    }

    private static MCRMetaHistoryItem delete(MCRObjectID mcrid, String author, Instant instant) {
        return newHistoryItem(mcrid, author, instant, MCRMetadataHistoryEventType.DELETE);
    }

    private static MCRMetaHistoryItem newHistoryItem(MCRObjectID mcrid, String author, Instant instant,
        MCRMetadataHistoryEventType eventType) {
        MCRMetaHistoryItem item = new MCRMetaHistoryItem();
        item.setId(mcrid);
        item.setTime(instant);
        item.setUserID(author);
        item.setEventType(eventType);
        LogManager.getLogger().debug(() -> item);
        return item;
    }

}
