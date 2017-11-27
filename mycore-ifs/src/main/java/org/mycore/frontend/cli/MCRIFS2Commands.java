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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.backend.hibernate.tables.MCRFSNODES_;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
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
    public static void repairMcrdataXmlForProject(String project_id, String content_store) {
        ArrayList<String> derivates = getDerivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            repairMcrdataXmlForDerivate(derivate, content_store);
        }
    }

    @MCRCommand(syntax = "repair mcrdata.xml for object {0} in content store {1}",
        help = "repair the entries in mcrdata.xml with data from content store {1} for a MCRObject {0}")
    public static void repairMcrdataXmlForObject(String object_id, String content_store) {
        ArrayList<String> derivates = getDerivatesOfObject(content_store, object_id);
        for (String derivate : derivates) {
            repairMcrdataXmlForDerivate(derivate, content_store);
        }
    }

    @MCRCommand(syntax = "repair mcrdata.xml for derivate {0} in content store {1}",
        help = "repair the entries in mcrdata.xml with data from content store {1} for MCRDerivate {0}")
    public static void repairMcrdataXmlForDerivate(String derivate_id, String content_store) {
        LOGGER.info("Start repair of mcrdata.xml for derivate {} in store {}", derivate_id, content_store);
        // check input;
        MCRObjectID mcr_derivate_id;
        try {
            mcr_derivate_id = MCRObjectID.getInstance(derivate_id);
        } catch (MCRException e) {
            LOGGER.error("Wrong derivate parameter, it is not a MCRObjectID");
            return;
        }
        if (content_store == null || content_store.length() == 0) {
            LOGGER.error("Empty content store parameter");
            return;
        }
        MCRContentStore store = MCRContentStoreFactory.getStore(content_store);
        if (!(store instanceof MCRCStoreIFS2)) {
            LOGGER.error("The content store is not a IFS2 type");
            return;
        }
        // repair
        try {
            MCRFileCollection file_collection = ((MCRCStoreIFS2) store).getIFS2FileCollection(mcr_derivate_id);
            file_collection.repairMetadata();
        } catch (IOException e) {
            LOGGER.error("Erroe while repair derivate with ID {}", mcr_derivate_id);
        }
    }

    @MCRCommand(syntax = "check mcrfsnodes for project id {0} of content store {1}",
        help = "check the entries of MCRFNODES with data from content store {1} for project ID {0}")
    public static void checkMCRFSNODESForProject(String project_id, String content_store) {
        LOGGER.info("Start check of MCRFSNODES for project {}", project_id);
        ArrayList<String> derivates = getDerivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            checkMCRFSNODESForDerivate(derivate, content_store);
        }
        LOGGER.info("Stop check of MCRFSNODES for project {}", project_id);
    }

    @MCRCommand(syntax = "check mcrfsnodes for object {0} of content store {1}",
        help = "check the entries of MCRFNODES with data from content store {1} for MCRObject {0}")
    public static void checkMCRFSNODESForObject(String object_id, String content_store) {
        LOGGER.info("Start check of MCRFSNODES for object {}", object_id);
        ArrayList<String> derivates = getDerivatesOfObject(content_store, object_id);
        for (String derivate : derivates) {
            checkMCRFSNODESForDerivate(derivate, content_store);
        }
        LOGGER.info("Stop check of MCRFSNODES for object {}", object_id);
    }

    @MCRCommand(syntax = "check mcrfsnodes for derivate {0} of content store {1}",
        help = "check the entries of MCRFSNODES with data from content store {1} for MCRDerivate {0}")
    public static void checkMCRFSNODESForDerivate(String derivate_id, String content_store) {
        LOGGER.info("Start check of MCRFSNODES for derivate {}", derivate_id);
        fixMCRFSNODESForDerivate(content_store, derivate_id, true);
        LOGGER.info("Stop check of MCRFSNODES for derivate {}", derivate_id);
    }

    @MCRCommand(syntax = "repair mcrfsnodes for project id {0} of content store {1}",
        help = "repair the entries of MCRFNODES with data from content store {1} for project ID {0}")
    public static void repairMCRFSNODESForProject(String project_id, String content_store) {
        LOGGER.info("Start repair of MCRFSNODES for project {}", project_id);
        ArrayList<String> derivates = getDerivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            repairMCRFSNODESForDerivate(derivate, content_store);
        }
        LOGGER.info("Stop repair of MCRFSNODES for project {}", project_id);
    }

    @MCRCommand(syntax = "repair mcrfsnodes for object {0} of content store {1}",
        help = "repair the entries of MCRFNODES with data from content store {1} for MCRObject {0}")
    public static void repairMCRFSNODESForObject(String object_id, String content_store) {
        LOGGER.info("Start repair of MCRFSNODES for object {}", object_id);
        ArrayList<String> derivates = getDerivatesOfObject(content_store, object_id);
        for (String derivate : derivates) {
            repairMCRFSNODESForDerivate(derivate, content_store);
        }
        LOGGER.info("Stop repair of MCRFSNODES for project {}", object_id);
    }

    @MCRCommand(syntax = "repair mcrfsnodes for derivate {0} of content store {1}",
        help = "repair the entries of MCRFSNODES with data from content store {1} for MCRDerivate {0}")
    public static void repairMCRFSNODESForDerivate(String derivate_id, String content_store) {
        LOGGER.info("Start repair of MCRFSNODES for derivate {}", derivate_id);
        fixMCRFSNODESForDerivate(content_store, derivate_id, false);
        LOGGER.info("Stop repair of MCRFSNODES for derivate {}", derivate_id);
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

    private static void fixMCRFSNODESForDerivate(String content_store, String derivate_id, boolean check_only) {
        // check input
        MCRObjectID mcr_derivate_id;
        try {
            mcr_derivate_id = MCRObjectID.getInstance(derivate_id);
        } catch (MCRException e) {
            LOGGER.error("Wrong derivate parameter, it is not a MCRObjectID");
            return;
        }
        if (content_store == null || content_store.length() == 0) {
            LOGGER.error("Empty content store parameter");
            return;
        }
        MCRContentStore store = MCRContentStoreFactory.getStore(content_store);
        if (!(store instanceof MCRCStoreIFS2)) {
            LOGGER.error("The content store is not a IFS2 type");
            return;
        }
        // list all files
        try {
            MCRFileCollection file_collection = ((MCRCStoreIFS2) store).getIFS2FileCollection(mcr_derivate_id);
            File root_node = file_collection.getLocalFile();
            String storage_base = root_node.getAbsolutePath();
            storage_base = storage_base.substring(0, storage_base.length() - derivate_id.length());
            fixMCRFSNODESForNode(root_node, content_store, derivate_id, storage_base, check_only);
        } catch (IOException e) {
            LOGGER.error("Error while list all files of derivate with ID {}", mcr_derivate_id);
            e.printStackTrace();
        }
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.getTransaction();
        if (tx.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
            tx.commit();
        }
    }

    private static void fixMCRFSNODESForNode(File node, String content_store, String derivate_id, String storage_base,
        boolean check_only) {
        if (node.isDirectory()) {
            LOGGER.debug("fixMCRFSNODESForNode (directory) : {}", node.getAbsolutePath());
            fixDirectoryEntry(node, derivate_id, storage_base, check_only);
            File[] nodes = node.listFiles();
            for (File next_node : nodes) {
                fixMCRFSNODESForNode(next_node, content_store, derivate_id, storage_base, check_only);
            }
        } else {
            if (node.getName().equals("mcrdata.xml")) {
                return;
            }
            LOGGER.debug("fixMCRFSNODESForNode (file) : {}", node.getAbsolutePath());
            fixFileEntry(node, content_store, derivate_id, storage_base, check_only);
        }

    }

    private static void fixDirectoryEntry(File node, String derivate_id, String storage_base, boolean check_only) {
        String name = node.getName();
        LOGGER.debug("fixDirectoryEntry : name = {}", name);
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.getTransaction();
        if (tx.getStatus().isNotOneOf(TransactionStatus.ACTIVE)) {
            tx.begin();
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        try {
            em.detach(em.createQuery(query
                .where(
                    cb.equal(nodes.get(MCRFSNODES_.owner), derivate_id),
                    cb.equal(nodes.get(MCRFSNODES_.name), name),
                    cb.equal(nodes.get(MCRFSNODES_.type), "D")))
                .getSingleResult());
            LOGGER.debug("Found directory entry for {}", name);
            return;
        } catch (NoResultException e) {
            LOGGER.error("Can't find directory entry for {}", name);
            if (check_only)
                return;
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
            pid = getParentID(node, derivate_id);
        } catch (NoResultException e1) {
            if (!derivate_id.equals(name)) {
                LOGGER.error("Can't find parent id for directory {}", name);
                return;
            }
        } catch (NonUniqueResultException e1) {
            LOGGER.error("The directory entry for {} and {} is not unique!", derivate_id,
                node.getParentFile().getName());
            return;
        }
        try {
            MCRFSNODES mcrfsnodes = new MCRFSNODES();
            mcrfsnodes.setId(id);
            mcrfsnodes.setPid(pid);
            mcrfsnodes.setType("D");
            mcrfsnodes.setOwner(derivate_id);
            mcrfsnodes.setName(node.getName());
            mcrfsnodes.setDate(new Date(node.lastModified()));
            em.persist(mcrfsnodes);
            tx.commit();
            LOGGER.debug("Entry {} fixed.", name);
        } catch (HibernateException he) {
            if (tx != null) {
                tx.rollback();
            }
            he.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fixFileEntry(File node, String content_store, String derivate_id, String storage_base,
        boolean check_only) {
        LOGGER.debug("fixFileEntry : name = {}", node.getName());
        String storageid = node.getAbsolutePath().substring(storage_base.length()).replace("\\", "/");
        LOGGER.debug("fixFileEntry : storageid = {}", storageid);
        String id = "";
        String md5_old = "";
        long size_old = 0;
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
                        cb.equal(nodes.get(MCRFSNODES_.owner), derivate_id),
                        cb.equal(nodes.get(MCRFSNODES_.storeid), content_store),
                        cb.equal(nodes.get(MCRFSNODES_.storageid), storageid),
                        cb.equal(nodes.get(MCRFSNODES_.type), "F")))
                    .getSingleResult();
                LOGGER.debug("Found file entry for {}", storageid);
                foundEntry = true;
                id = fsNode.getId();
                md5_old = fsNode.getMd5();
                size_old = fsNode.getSize();
                em.detach(fsNode);
            } catch (NoResultException e) {
                LOGGER.error("Can't find file entry for {}", storageid);
                if (check_only)
                    return;
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
        try (MCRContentInputStream cis = new MCRContentInputStream(new FileInputStream(node))) {
            byte[] header = cis.getHeader();
            fctid = MCRFileContentTypeFactory.detectType(node.getName(), header).getID();
            ByteStreams.copy(cis, ByteStreams.nullOutputStream());
            md5 = cis.getMD5String();
        } catch (MCRException | IOException e1) {
            e1.printStackTrace();
            return;
        }
        long size = node.length();
        LOGGER.debug("size old : {} <--> size : {}", Long.toString(size_old), Long.toString(size));
        LOGGER.debug("MD5 old : {} <--> MD5 : {}", md5_old, md5);
        if (size_old == size && md5_old.equals(md5)) {
            return;
        }
        if (foundEntry && size_old != size) {
            LOGGER.warn("Wrong file size for {} : {} <-> {}", storageid, size_old, size);
        }
        if (foundEntry && !md5.equals(md5_old)) {
            LOGGER.warn("Wrong file md5 for {} : {} <-> {}", storageid, md5_old, md5);
        }
        if (check_only)
            return;
        // fix entry
        LOGGER.info("Fix entry for file {}", storageid);
        if (!foundEntry) {
            MCRFileMetadataManager fmmgr = MCRFileMetadataManager.instance();
            id = fmmgr.createNodeID();
        }
        String pid = null;
        try {
            pid = getParentID(node, derivate_id);
        } catch (NoResultException e1) {
            LOGGER.error("Can't find parent id of directory for file {}", storageid);
        } catch (NonUniqueResultException e1) {
            LOGGER.error("The directory entry for {} and {} is not unique!", derivate_id,
                node.getParentFile().getName());
            return;
        }
        try {
            MCRFSNODES mcrfsnodes = new MCRFSNODES();
            mcrfsnodes.setId(id);
            mcrfsnodes.setPid(pid);
            mcrfsnodes.setType("F");
            mcrfsnodes.setOwner(derivate_id);
            mcrfsnodes.setName(node.getName());
            mcrfsnodes.setSize(size);
            mcrfsnodes.setDate(new Date(node.lastModified()));
            mcrfsnodes.setStoreid(content_store);
            mcrfsnodes.setStorageid(storageid);
            mcrfsnodes.setFctid(fctid);
            mcrfsnodes.setMd5(md5);
            em.merge(mcrfsnodes);
            mcrSession.commitTransaction();
            LOGGER.debug("Entry {} fixed.", node.getName());
        } catch (PersistenceException pe) {
            mcrSession.rollbackTransaction();
            pe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getParentID(File node, String derivate_id)
        throws NoResultException, NonUniqueResultException {
        File parent_node = node.getParentFile();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRFSNODES> query = cb.createQuery(MCRFSNODES.class);
        Root<MCRFSNODES> nodes = query.from(MCRFSNODES.class);
        MCRFSNODES fsNode = em.createQuery(query
            .where(
                cb.equal(nodes.get(MCRFSNODES_.owner), derivate_id),
                cb.equal(nodes.get(MCRFSNODES_.name), parent_node.getName()),
                cb.equal(nodes.get(MCRFSNODES_.type), "D")))
            .getSingleResult();
        LOGGER.debug("Found directory entry for {}", parent_node.getName());
        em.detach(fsNode);
        return fsNode.getId();
    }

    private static ArrayList<String> getDerivatesOfProject(String content_store, String project_id) {
        ArrayList<String> derivates = new ArrayList<>();
        // get the IFS1.5
        MCRConfiguration config = MCRConfiguration.instance();
        String content_store_basepath = config.getString("MCR.IFS.ContentStore." + content_store + ".BaseDir", "");
        if (content_store_basepath.length() == 0) {
            LOGGER.error("Cant find base directory property in form MCR.IFS.ContentStore.{}.BaseDir", content_store);
            return derivates;
        }
        String slot_layout = config.getString("MCR.IFS.ContentStore." + content_store + ".SlotLayout", "");
        if (slot_layout.length() == 0) {
            LOGGER.error("Cant find slot layout property in form MCR.IFS.ContentStore.{}.SlotLayout", content_store);
            return derivates;
        }
        File project_dir = new File(content_store_basepath, project_id);
        if (!project_dir.exists()) {
            LOGGER.error("Wrong project ID; can't find directory {}", project_dir.getAbsolutePath());
            return derivates;
        }
        File derivate_dir = new File(project_dir, "derivate");
        int max_slot_deep = countCharacter(slot_layout, '-', 0) + 1;
        searchRecurive(derivates, derivate_dir, max_slot_deep, 0, project_id);
        return derivates;
    }

    private static ArrayList<String> getDerivatesOfObject(String content_store, String object_id) {
        ArrayList<String> derivates = new ArrayList<>();
        MCRConfiguration config = MCRConfiguration.instance();
        String content_store_basepath = config.getString("MCR.IFS.ContentStore." + content_store + ".BaseDir", "");
        if (content_store_basepath.length() == 0) {
            LOGGER.error("Cant find base directory property in form MCR.IFS.ContentStore.{}.BaseDir", content_store);
            return derivates;
        }
        derivates = (ArrayList<String>) MCRLinkTableManager.instance().getDestinationOf(object_id, "derivate");
        return derivates;
    }

    private static void searchRecurive(ArrayList<String> derivates, File dir, int max_slot_deep, int current_slot_deep,
        String project_id) {
        if (current_slot_deep == max_slot_deep)
            return;
        File[] dir_list = dir.listFiles();
        current_slot_deep++;
        for (File dir_dir : dir_list) {
            if (current_slot_deep < max_slot_deep && dir_dir.isDirectory()) {
                searchRecurive(derivates, dir_dir, max_slot_deep, current_slot_deep, project_id);
            }
            if (dir_dir.getName().startsWith(project_id + "_derivate")) {
                derivates.add(dir_dir.getName());
            }
        }

    }

    private static int countCharacter(String haystack, char needle, int i) {
        return ((i = haystack.indexOf(needle, i)) == -1) ? 0 : 1 + countCharacter(haystack, needle, i + 1);
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
