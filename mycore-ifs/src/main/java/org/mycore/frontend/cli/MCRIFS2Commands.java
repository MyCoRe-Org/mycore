/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.backend.hibernate.tables.MCRFSNODES_;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFileMetadataManager;
import org.mycore.datamodel.ifs2.MCRCStoreIFS2;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

import com.google.common.io.ByteStreams;

@MCRCommandGroup(name = "IFS2 Maintenance Commands")
public class MCRIFS2Commands {

    private static Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "repair mcrdata.xml for project id {0} in content store {1}",
        help = "repair the entries in mcrdata.xml with data from content store {1} for project ID {0}")
    public static List<String> repairMcrdataXmlForProject(String projectId, String contentStore) {
        return MCRCommandUtils.getIdsForProjectAndType(projectId, "derivate")
            .map(id -> "repair mcrdata.xml for derivate " + id + " in content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair mcrdata.xml for object {0} in content store {1}",
        help = "repair the entries in mcrdata.xml with data from content store {1} for a MCRObject {0}")
    public static List<String> repairMcrdataXmlForObject(String objectId, String contentStore) {
        return getDerivatesOfObject(contentStore, objectId).parallelStream()
            .map(id -> "repair mcrdata.xml for derivate " + id + " in content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair mcrdata.xml for derivate {0} in content store {1}",
        help = "repair the entries in mcrdata.xml with data from content store {1} for MCRDerivate {0}")
    public static void repairMcrdataXmlForDerivate(String derivateId, String contentStore) {
        LOGGER.info("Start repair of mcrdata.xml for derivate {} in store {}", derivateId, contentStore);
        // check input;
        MCRObjectID mcrDerivateId;
        try {
            mcrDerivateId = MCRObjectID.getInstance(derivateId);
        } catch (MCRException e) {
            LOGGER.error("Wrong derivate parameter, it is not a MCRObjectID");
            return;
        }
        if (contentStore == null || contentStore.length() == 0) {
            LOGGER.error("Empty content store parameter");
            return;
        }
        MCRContentStore store = MCRContentStoreFactory.getStore(contentStore);
        if (!(store instanceof MCRCStoreIFS2)) {
            LOGGER.error("The content store is not a IFS2 type");
            return;
        }
        // repair
        try {
            MCRFileCollection fileCollection = ((MCRCStoreIFS2) store).getIFS2FileCollection(mcrDerivateId);
            fileCollection.repairMetadata();
        } catch (IOException e) {
            LOGGER.error("Error while repair derivate with ID {}", mcrDerivateId);
        }
    }

    @MCRCommand(syntax = "check mcrfsnodes for project id {0} of content store {1}",
        help = "check the entries of MCRFNODES with data from content store {1} for project ID {0}")
    public static List<String> checkMCRFSNODESForProject(String projectId, String contentStore) {
        LOGGER.info("Checking MCRFSNODES for project {}", projectId);
        return MCRCommandUtils.getIdsForProjectAndType(projectId, "derivate")
            .map(id -> "check mcrfsnodes for derivate " + id + " of content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "check mcrfsnodes for object {0} of content store {1}",
        help = "check the entries of MCRFNODES with data from content store {1} for MCRObject {0}")
    public static List<String> checkMCRFSNODESForObject(String objectId, String contentStore) {
        LOGGER.info("Checking MCRFSNODES for object {}", objectId);
        return getDerivatesOfObject(contentStore, objectId).parallelStream()
            .map(id -> "check mcrfsnodes for derivate " + id + " of content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "check mcrfsnodes for derivate {0} of content store {1}",
        help = "check the entries of MCRFSNODES with data from content store {1} for MCRDerivate {0}")
    public static void checkMCRFSNODESForDerivate(String derivateId, String contentStore) {
        LOGGER.info("Start check of MCRFSNODES for derivate {}", derivateId);
        fixMCRFSNODESForDerivate(contentStore, derivateId, true);
        LOGGER.info("Stop check of MCRFSNODES for derivate {}", derivateId);
    }

    @MCRCommand(syntax = "repair mcrfsnodes for project id {0} of content store {1}",
        help = "repair the entries of MCRFNODES with data from content store {1} for project ID {0}")
    public static List<String> repairMCRFSNODESForProject(String projectId, String contentStore) {
        LOGGER.info("Repairing MCRFSNODES for project {}", projectId);
        return MCRCommandUtils.getIdsForProjectAndType(projectId, "derivate")
            .map(id -> "repair mcrfsnodes for derivate " + id + " of content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair mcrfsnodes for object {0} of content store {1}",
        help = "repair the entries of MCRFNODES with data from content store {1} for MCRObject {0}")
    public static List<String> repairMCRFSNODESForObject(String objectId, String contentStore) {
        LOGGER.info("Repairing MCRFSNODES for object {}", objectId);
        return getDerivatesOfObject(contentStore, objectId).parallelStream()
            .map(id -> "repair mcrfsnodes for derivate " + id + " of content store " + contentStore)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "repair mcrfsnodes for derivate {0} of content store {1}",
        help = "repair the entries of MCRFSNODES with data from content store {1} for MCRDerivate {0}")
    public static void repairMCRFSNODESForDerivate(String derivateId, String contentStore) {
        LOGGER.info("Start repair of MCRFSNODES for derivate {}", derivateId);
        fixMCRFSNODESForDerivate(contentStore, derivateId, false);
        LOGGER.info("Stop repair of MCRFSNODES for derivate {}", derivateId);
    }

    @MCRCommand(syntax = "repair unicode in database {0}",
        help = "this fixes consequences of MCR-1423 in Database. If "
            + "{0} is false then nothing will be done (dry run).")
    public static void repairUnicodeInDatabase(String execute) {
        boolean dry = execute.toLowerCase(Locale.ROOT).equals(Boolean.FALSE.toString().toLowerCase(Locale.ROOT));
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> getQuery = cb.createQuery(MCRFSNODES.class);

        List<MCRFSNODES> resultList = em.createQuery(getQuery.select(getQuery.from(MCRFSNODES.class))).getResultList();

        resultList.stream().forEach(node -> {
            String unnormalName = node.getName();
            String normalName = MCRXMLFunctions.normalizeUnicode(unnormalName);
            if (!unnormalName.equals(normalName)) {
                LOGGER.info("{} node {} with name {}", (dry) ? "Would Fix" : "Fixing", node.getId(), unnormalName);
                if (!dry) {
                    node.setName(normalName);
                }
            }
        });
    }

    @MCRCommand(syntax = "repair unicode in content stores {0}",
        help = "this fixes consequences of MCR-1423 in content"
            + " stores . If {0} is false then nothing will be done (dry run).")
    public static void repairUnicodeInContentStores(String execute) {
        boolean dry = execute.toLowerCase(Locale.ROOT).equals(Boolean.FALSE.toString().toLowerCase(Locale.ROOT));
        MCRContentStoreFactory.getAvailableStores().forEach((name, cs) -> {
            LOGGER.info("{} store: {} ", dry ? "would fix" : "fixing", name);

            try {
                Path path = cs.getBaseDir().toPath();
                LOGGER.info("Starting with path : {}", path);
                java.nio.file.Files.walkFileTree(path, new MCRUnicodeFilenameNormalizer(dry));
            } catch (IOException e) {
                throw new MCRException("Error while get basedir of content store " + name, e);
            }
        });
    }

    private static void fixMCRFSNODESForDerivate(String contentStore, String derivateId, boolean checkOnly) {
        // check input
        MCRObjectID mcrDerivateId;
        try {
            mcrDerivateId = MCRObjectID.getInstance(derivateId);
        } catch (MCRException e) {
            LOGGER.error("Wrong derivate parameter, it is not a MCRObjectID");
            return;
        }
        if (contentStore == null || contentStore.length() == 0) {
            LOGGER.error("Empty content store parameter");
            return;
        }
        MCRContentStore store = MCRContentStoreFactory.getStore(contentStore);
        if (!(store instanceof MCRCStoreIFS2)) {
            LOGGER.error("The content store is not a IFS2 type");
            return;
        }
        // list all files
        try {
            MCRFileCollection fileCollection = ((MCRCStoreIFS2) store).getIFS2FileCollection(mcrDerivateId);
            Path rootNode = fileCollection.getLocalPath();
            String storageBase = rootNode.toAbsolutePath().toString();
            storageBase = storageBase.substring(0, storageBase.length() - derivateId.length());
            fixMCRFSNODESForNode(rootNode, contentStore, derivateId, storageBase, checkOnly);
        } catch (IOException e) {
            LOGGER.error("Error while list all files of derivate with ID {}", mcrDerivateId);
            e.printStackTrace();
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction tx = em.getTransaction();
        if (tx.isActive()) {
            tx.commit();
        }
    }

    private static void fixMCRFSNODESForNode(Path node, String contentStore, String derivateId, String storageBase,
        boolean checkOnly) throws IOException {
        if (Files.isDirectory(node)) {
            LOGGER.debug("fixMCRFSNODESForNode (directory) : {}", node.toAbsolutePath().toString());
            fixDirectoryEntry(node, derivateId, checkOnly);
            try (Stream<Path> stream = Files.list(node)) {
                Path[] nodes = stream.toArray(Path[]::new);
                for (Path nextNode : nodes) {
                    fixMCRFSNODESForNode(nextNode, contentStore, derivateId, storageBase, checkOnly);
                }
            }
        } else {
            if (node.getFileName().toString().equals("mcrdata.xml")) {
                return;
            }
            LOGGER.debug("fixMCRFSNODESForNode (file) : {}", node.toAbsolutePath().toString());
            fixFileEntry(node, contentStore, derivateId, storageBase, checkOnly);
        }

    }

    private static void fixDirectoryEntry(Path node, String derivateId, boolean checkOnly) {
        String name = node.getFileName().toString();
        LOGGER.debug("fixDirectoryEntry : name = {}", name);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction tx = em.getTransaction();
        if (!tx.isActive()) {
            tx.begin();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        try {
            em.detach(em.createQuery(query
                .where(
                    cb.equal(nodes.get(MCRFSNODES_.owner), derivateId),
                    cb.equal(nodes.get(MCRFSNODES_.name), name),
                    cb.equal(nodes.get(MCRFSNODES_.type), "D")))
                .getSingleResult());
            LOGGER.debug("Found directory entry for {}", name);
            return;
        } catch (NoResultException e) {
            LOGGER.error("Can't find directory entry for {}", name);
            if (checkOnly) {
                return;
            }
        } catch (NonUniqueResultException e) {
            LOGGER.error("Non unique directory entry for {}", name);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // fix entry
        LOGGER.info("Fix entry for directory {}", name);
        MCRFileMetadataManager fmmgr = MCRFileMetadataManager.instance();
        String id = fmmgr.createNodeID();
        String pid = null;
        try {
            pid = getParentID(node, derivateId);
        } catch (NoResultException e1) {
            if (!derivateId.equals(name)) {
                LOGGER.error("Can't find parent id for directory {}", name);
                return;
            }
        } catch (NonUniqueResultException e1) {
            LOGGER.error("The directory entry for {} and {} is not unique!", derivateId,
                node.getParent().getFileName());
            return;
        }
        try {
            MCRFSNODES mcrfsnodes = new MCRFSNODES();
            mcrfsnodes.setId(id);
            mcrfsnodes.setPid(pid);
            mcrfsnodes.setType("D");
            mcrfsnodes.setOwner(derivateId);
            mcrfsnodes.setName(node.getFileName().toString());
            mcrfsnodes.setDate(Date.from(Files.getLastModifiedTime(node).toInstant()));
            em.persist(mcrfsnodes);
            tx.commit();
            LOGGER.debug("Entry {} fixed.", name);
        } catch (PersistenceException pe) {
            if (tx != null) {
                tx.rollback();
            }
            pe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fixFileEntry(Path node, String contentStore, String derivateId, String storageBase,
        boolean checkOnly) {
        LOGGER.debug("fixFileEntry : name = {}", node.getFileName());
        String storageid = node.toAbsolutePath().toString().substring(storageBase.length()).replace("\\", "/");
        LOGGER.debug("fixFileEntry : storageid = {}", storageid);
        String id = "";
        String oldMd5 = "";
        long oldSize = 0;
        boolean foundEntry = false;
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        boolean transactionActive = mcrSession.isTransactionActive();
        if (!transactionActive) {
            mcrSession.beginTransaction();
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
            Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
            try {
                MCRFSNODES fsNode = em.createQuery(query
                    .where(
                        cb.equal(nodes.get(MCRFSNODES_.owner), derivateId),
                        cb.equal(nodes.get(MCRFSNODES_.storeid), contentStore),
                        cb.equal(nodes.get(MCRFSNODES_.storageid), storageid),
                        cb.equal(nodes.get(MCRFSNODES_.type), "F")))
                    .getSingleResult();
                LOGGER.debug("Found file entry for {}", storageid);
                foundEntry = true;
                id = fsNode.getId();
                oldMd5 = fsNode.getMd5();
                oldSize = fsNode.getSize();
                em.detach(fsNode);
            } catch (NoResultException e) {
                LOGGER.error("Can't find file entry for {}", storageid);
                if (checkOnly) {
                    return;
                }
            } catch (NonUniqueResultException e) {
                LOGGER.error("Non unique file entry for {}", storageid);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // check fctid, size and MD5 of the file
        String fctid = "";
        String md5 = "";
        long size;
        try (MCRContentInputStream cis = new MCRContentInputStream(Files.newInputStream(node))) {
            byte[] header = cis.getHeader();
            fctid = MCRFileContentTypeFactory.detectType(node.getFileName().toString(), header).getID();
            ByteStreams.copy(cis, ByteStreams.nullOutputStream());
            md5 = cis.getMD5String();
            size = Files.size(node);
        } catch (MCRException | IOException e1) {
            e1.printStackTrace();
            return;
        }
        LOGGER.debug("size old : {} <--> size : {}", Long.toString(oldSize), Long.toString(size));
        LOGGER.debug("MD5 old : {} <--> MD5 : {}", oldMd5, md5);
        if (oldSize == size && oldMd5.equals(md5)) {
            return;
        }
        if (foundEntry && oldSize != size) {
            LOGGER.warn("Wrong file size for {} : {} <-> {}", storageid, oldSize, size);
        }
        if (foundEntry && !md5.equals(oldMd5)) {
            LOGGER.warn("Wrong file md5 for {} : {} <-> {}", storageid, oldMd5, md5);
        }
        if (checkOnly) {
            return;
        }
        // fix entry
        LOGGER.info("Fix entry for file {}", storageid);
        if (!foundEntry) {
            MCRFileMetadataManager fmmgr = MCRFileMetadataManager.instance();
            id = fmmgr.createNodeID();
        }
        String pid = null;
        try {
            pid = getParentID(node, derivateId);
        } catch (NoResultException e1) {
            LOGGER.error("Can't find parent id of directory for file {}", storageid);
        } catch (NonUniqueResultException e1) {
            LOGGER.error("The directory entry for {} and {} is not unique!", derivateId,
                node.getParent().getFileName());
            return;
        }
        try {
            MCRFSNODES mcrfsnodes = new MCRFSNODES();
            mcrfsnodes.setId(id);
            mcrfsnodes.setPid(pid);
            mcrfsnodes.setType("F");
            mcrfsnodes.setOwner(derivateId);
            mcrfsnodes.setName(node.getFileName().toString());
            mcrfsnodes.setSize(size);
            mcrfsnodes.setDate(Date.from(Files.getLastModifiedTime(node).toInstant()));
            mcrfsnodes.setStoreid(contentStore);
            mcrfsnodes.setStorageid(storageid);
            mcrfsnodes.setFctid(fctid);
            mcrfsnodes.setMd5(md5);
            em.merge(mcrfsnodes);
            mcrSession.commitTransaction();
            LOGGER.debug("Entry {} fixed.", node.getFileName());
        } catch (PersistenceException pe) {
            mcrSession.rollbackTransaction();
            pe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getParentID(Path node, String derivateId)
        throws NoResultException, NonUniqueResultException {
        Path parentNode = node.getParent();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        MCRFSNODES fsNode = em.createQuery(query
            .where(
                cb.equal(nodes.get(MCRFSNODES_.owner), derivateId),
                cb.equal(nodes.get(MCRFSNODES_.name), parentNode.getFileName().toString()),
                cb.equal(nodes.get(MCRFSNODES_.type), "D")))
            .getSingleResult();
        LOGGER.debug("Found directory entry for {}", parentNode.getFileName());
        em.detach(fsNode);
        return fsNode.getId();
    }

    private static ArrayList<String> getDerivatesOfObject(String contentStore, String objectId) {
        ArrayList<String> derivates = new ArrayList<>();
        String contentStoreBasePath = MCRConfiguration2.getString("MCR.IFS.ContentStore." + contentStore + ".BaseDir")
            .orElse("");
        if (contentStoreBasePath.length() == 0) {
            LOGGER.error("Cant find base directory property in form MCR.IFS.ContentStore.{}.BaseDir", contentStore);
            return derivates;
        }
        derivates = (ArrayList<String>) MCRLinkTableManager.instance().getDestinationOf(objectId, "derivate");
        return derivates;
    }

    public static class MCRUnicodeFilenameNormalizer extends SimpleFileVisitor<Path> {

        private boolean dry;

        public MCRUnicodeFilenameNormalizer(final boolean dry) {
            super();
            this.dry = dry;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            LOGGER.debug("Checking: {}", dir.toString());

            if (canNormalize(dir)) {
                LOGGER.info("{} Directory {}", dry ? "Would fix" : "Fixing", dir.toString());
                Path normalizedPath = getNormalizedPath(dir);
                if (!dry) {
                    java.nio.file.Files.move(dir, normalizedPath, StandardCopyOption.ATOMIC_MOVE);
                }
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            LOGGER.debug("Checking: {}", file.toString());

            if (canNormalize(file)) {
                LOGGER.info("{} File {}", dry ? "Would fix" : "Fixing", file.toString());
                Path normalizedPath = getNormalizedPath(file);
                if (!dry) {
                    java.nio.file.Files.createDirectory(normalizedPath);
                }
            }

            return FileVisitResult.CONTINUE;
        }

        public boolean canNormalize(Path file) {
            String maybeWrongName = file.getFileName().toString();
            String normalName = MCRXMLFunctions.normalizeUnicode(maybeWrongName);
            return !maybeWrongName.equals(normalName);
        }

        public Path getNormalizedPath(Path file) {
            String maybeWrongName = file.getFileName().toString();
            String normalName = MCRXMLFunctions.normalizeUnicode(maybeWrongName);
            return file.getParent().resolve(normalName);
        }
    }

}
