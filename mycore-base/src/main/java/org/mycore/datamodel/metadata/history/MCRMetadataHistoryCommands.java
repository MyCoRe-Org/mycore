/**
 * 
 */
package org.mycore.datamodel.metadata.history;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.jdom2.JDOMException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.common.MCRCreatorCache;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MCRCommandGroup(name = "Metadata history")
public class MCRMetadataHistoryCommands {

    @MCRCommand(syntax = "clear metadata history of base {0}", help = "clears metadata history of all objects with base id {0}")
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

    @MCRCommand(syntax = "clear metadata history of id {0}", help = "clears metadata history of object/derivate with id {0}")
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
        return MCRXMLMetadataManager.instance()
            .getObjectBaseIds()
            .stream()
            .map(s -> "clear metadata history of base " + s)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "build metadata history completely", help = "build metadata history completely")
    public static List<String> buildHistory() {
        return MCRXMLMetadataManager.instance()
            .getObjectBaseIds()
            .stream()
            .map(s -> "build metadata history of base " + s)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "build metadata history of base {0}", help = "build metadata history of all objects with base id {0}")
    public static List<String> buildHistory(String baseId) {
        MCRMetadataStore store = MCRXMLMetadataManager.instance().getStore(baseId);
        return IntStream.rangeClosed(1, store.getHighestStoredID())
            .mapToObj(i -> MCRObjectID.formatID(baseId, i))
            .map(id -> "build metadata history of id " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "build metadata history of id {0}", help = "build metadata history of object/derivate with id {0}")
    public static void buildSingleHistory(String mcrId) {
        MCRObjectID objId = MCRObjectID.getInstance(mcrId);
        if (objId.getTypeId().equals("derivate")) {
            buildDerivateHistory(objId);
        } else {
            buildObjectHistory(objId);
        }
    }

    private static void buildDerivateHistory(MCRObjectID derId) {
        try {
            List<MCRMetadataVersion> versions = Collections.emptyList();
            MCRMetadataStore store = MCRXMLMetadataManager.instance().getStore(derId);
            if (store instanceof MCRVersioningMetadataStore) {
                MCRVersionedMetadata versionedMetadata = ((MCRVersioningMetadataStore) store)
                    .retrieve(derId.getNumberAsInteger());
                versions = versionedMetadata.listVersions();
            }
            if (versions.isEmpty()) {
                buildSimpleDerivateHistory(derId);
            } else {
                buildDerivateHistory(derId, versions);
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Error while getting history of " + derId);
        }
    }

    private static void buildObjectHistory(MCRObjectID objId) {
        try {
            List<MCRMetadataVersion> versions = Collections.emptyList();
            MCRMetadataStore store = MCRXMLMetadataManager.instance().getStore(objId);
            if (store instanceof MCRVersioningMetadataStore) {
                MCRVersionedMetadata versionedMetadata = ((MCRVersioningMetadataStore) store)
                    .retrieve(objId.getNumberAsInteger());
                versions = versionedMetadata.listVersions();
            }
            if (versions.isEmpty()) {
                buildSimpleObjectHistory(objId);
            } else {
                buildObjectHistory(objId, versions);
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Error while getting history of " + objId);
        }
    }

    private static void buildSimpleDerivateHistory(MCRObjectID derId) throws IOException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        boolean exist = false;
        LogManager.getLogger().info("Store of {} has no old revisions. History rebuild is limited", derId);
        if (MCRMetadataManager.exists(derId)) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(derId);
            Instant lastModified = Instant
                .ofEpochMilli(MCRXMLMetadataManager.instance().getLastModified(derId));
            String creator;
            try {
                creator = MCRCreatorCache.getCreator(der.getId());
            } catch (ExecutionException e) {
                LogManager.getLogger().warn("Error while getting creator of " + derId, e);
                creator = null;
            }
            String user = Optional.ofNullable(creator)
                .orElseGet(() -> MCRSystemUserInformation.getSystemUserInstance().getUserID());
            em.persist(create(derId,
                user,
                lastModified));
            exist = true;
            boolean objectIsHidden = !der.getDerivate().isDisplayEnabled();
            if (objectIsHidden && exist) {
                em.persist(delete(derId, user, lastModified.plusMillis(1)));
                exist ^= true;
            }
        } else {
            em.persist(delete(derId, null, Instant.now()));
        }
    }

    private static void buildSimpleObjectHistory(MCRObjectID objId) throws IOException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        boolean exist = false;
        LogManager.getLogger().info("Store of {} has no old revisions. History rebuild is limited", objId);
        if (MCRMetadataManager.exists(objId)) {
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(objId);
            Instant lastModified = Instant
                .ofEpochMilli(MCRXMLMetadataManager.instance().getLastModified(objId));
            String creator;
            try {
                creator = MCRCreatorCache.getCreator(obj.getId());
            } catch (ExecutionException e) {
                LogManager.getLogger().warn("Error while getting creator of " + objId, e);
                creator = null;
            }
            String user = Optional.ofNullable(creator)
                .orElseGet(() -> MCRSystemUserInformation.getSystemUserInstance().getUserID());
            em.persist(create(objId,
                user,
                lastModified));
            exist = true;
            boolean objectIsHidden = MCRMetadataHistoryManager.objectIsHidden(obj);
            if (objectIsHidden && exist) {
                em.persist(delete(objId, user, lastModified.plusMillis(1)));
                exist ^= true;
            }
        } else {
            em.persist(delete(objId, null, Instant.now()));
        }
    }

    private static void buildDerivateHistory(MCRObjectID derId, List<MCRMetadataVersion> versions) throws IOException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        boolean exist = false;
        LogManager.getLogger().info("Complete history rebuild of {} should be possible", derId);
        for (MCRMetadataVersion version : versions) {
            String user = version.getUser();
            Instant revDate = version.getDate().toInstant();
            if (version.getType() == MCRMetadataVersion.DELETED) {
                if (exist) {
                    em.persist(delete(derId, user, revDate));
                    exist = false;
                }
            } else {
                //created or updated
                int timeOffset = 0;
                if (version.getType() == MCRMetadataVersion.CREATED && !exist) {
                    em.persist(create(derId, user, revDate));
                    timeOffset = 1;
                    exist = true;
                }
                try {
                    MCRDerivate derivate = new MCRDerivate(version.retrieve().asXML());
                    boolean derivateIsHidden = !derivate.getDerivate().isDisplayEnabled();
                    if (derivateIsHidden && exist) {
                        em.persist(delete(derId, user, revDate.plusMillis(timeOffset)));
                        exist = false;
                    } else if (!derivateIsHidden && !exist) {
                        em.persist(create(derId, user,
                            revDate.plusMillis(timeOffset)));
                        exist = true;
                    }
                } catch (JDOMException | SAXException e) {
                    LogManager.getLogger()
                        .error("Error while reading revision " + version.getRevision() + " of " + derId, e);
                }
            }
        }
    }

    private static void buildObjectHistory(MCRObjectID objId, List<MCRMetadataVersion> versions) throws IOException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        boolean exist = false;
        LogManager.getLogger().info("Complete history rebuild of {} should be possible", objId);
        for (MCRMetadataVersion version : versions) {
            String user = version.getUser();
            Instant revDate = version.getDate().toInstant();
            if (version.getType() == MCRMetadataVersion.DELETED) {
                if (exist) {
                    em.persist(delete(objId, user, revDate));
                    exist = false;
                }
            } else {
                //created or updated
                int timeOffset = 0;
                if (version.getType() == MCRMetadataVersion.CREATED && !exist) {
                    em.persist(create(objId, user, revDate));
                    timeOffset = 1;
                    exist = true;
                }
                try {
                    MCRObject obj = new MCRObject(version.retrieve().asXML());
                    boolean objectIsHidden = MCRMetadataHistoryManager.objectIsHidden(obj);
                    if (objectIsHidden && exist) {
                        em.persist(delete(objId, user, revDate.plusMillis(timeOffset)));
                        exist = false;
                    } else if (!objectIsHidden && !exist) {
                        em.persist(create(objId, user, revDate.plusMillis(timeOffset)));
                        exist = true;
                    }
                } catch (JDOMException | SAXException e) {
                    LogManager.getLogger()
                        .error("Error while reading revision " + version.getRevision() + " of " + objId, e);
                }
            }
        }
    }

    private static MCRMetaHistoryItem create(MCRObjectID mcrid, String author, Instant instant) {
        return newHistoryItem(mcrid, author, instant, MCRMetadataHistoryEventType.Create);
    }

    private static MCRMetaHistoryItem delete(MCRObjectID mcrid, String author, Instant instant) {
        return newHistoryItem(mcrid, author, instant, MCRMetadataHistoryEventType.Delete);
    }

    private static MCRMetaHistoryItem newHistoryItem(MCRObjectID mcrid, String author, Instant instant,
        MCRMetadataHistoryEventType eventType) {
        MCRMetaHistoryItem item = new MCRMetaHistoryItem();
        item.setId(mcrid);
        item.setTime(instant);
        item.setUserID(author);
        item.setEventType(eventType);
        LogManager.getLogger().info(() -> item);
        return item;
    }

}
