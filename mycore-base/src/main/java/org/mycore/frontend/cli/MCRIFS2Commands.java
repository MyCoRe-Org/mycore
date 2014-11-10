/*
 * $Revision: 27557 $ $Date: 2013-08-02 15:57:50 +0200 (Fr, 02. Aug 2013) $
 * 
 * This file is part of M y C o R e See http://www.mycore.de/ for details. This
 * program is free software; you can use it, redistribute it and / or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation; either version 2 of the License or (at your option)
 * any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program, in a file called gpl.txt or
 * license.txt. If not, write to the Free Software Foundation Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
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

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

@MCRCommandGroup(name = "IFS2 Maintenance")
public class MCRIFS2Commands {

    private static Logger LOGGER = Logger.getLogger(MCRIFSCommands.class);

    @MCRCommand(syntax = "repair mcrdata.xml in content store {0} for project ID {1}", help = "repair the entries in mcrdata.xml with data from content store {0} for project ID {1}")
    public static void repairMcrdataXmlForProject(String content_store, String project_id) {
        ArrayList<String> derivates = getDetivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            repairMcrdataXmlForDreivate(content_store, derivate);
        }
    }

    @MCRCommand(syntax = "repair mcrdata.xml in content store {0} for derivate {1}", help = "repair the entries in mcrdata.xml with data from content store {0} for derivate {1}")
    public static void repairMcrdataXmlForDreivate(String content_store, String derivate_id) {
        LOGGER.info("Start repair of mcrdata.xml for derivate " + derivate_id + " in store " + content_store);
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
            LOGGER.error("Erroe while repair derivate with ID " + mcr_derivate_id.toString());
        }
    }

    @MCRCommand(syntax = "check mcrfsnodes of content store {0} for project ID {1}", help = "check the entries of MCRFNODES with data from content store {0} for project ID {1}")
    public static void checkMCRFSNODESForProject(String content_store, String project_id) {
        ArrayList<String> derivates = getDetivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            checkMCRFSNODESForDreivate(content_store, derivate);
        }
    }

    @MCRCommand(syntax = "check mcrfsnodes of content store {0} for derivate {1}", help = "check the entries of MCRFSNODES with data from content store {0} for derivate {1}")
    public static void checkMCRFSNODESForDreivate(String content_store, String derivate_id) {
        LOGGER.info("Start repair of MCRFSNODES for derivate " + derivate_id);
        fixMCRFSNODESForDreivate(content_store, derivate_id, true);
    }

    @MCRCommand(syntax = "repair mcrfsnodes of content store {0} for project ID {1}", help = "repair the entries of MCRFNODES with data from content store {0} for project ID {1}")
    public static void repairMCRFSNODESForProject(String content_store, String project_id) {
        ArrayList<String> derivates = getDetivatesOfProject(content_store, project_id);
        for (String derivate : derivates) {
            repairMCRFSNODESForDreivate(content_store, derivate);
        }
    }

    @MCRCommand(syntax = "repair mcrfsnodes of content store {0} for derivate {1}", help = "repair the entries of MCRFSNODES with data from content store {0} for derivate {1}")
    public static void repairMCRFSNODESForDreivate(String content_store, String derivate_id) {
        LOGGER.info("Start repair of MCRFSNODES for derivate " + derivate_id);
        fixMCRFSNODESForDreivate(content_store, derivate_id, false);
    }

    private static void fixMCRFSNODESForDreivate(String content_store, String derivate_id, boolean check_only) {
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
            LOGGER.error("Erroe while list all files of derivate with ID " + mcr_derivate_id.toString());
            e.printStackTrace();
        }
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            tx.commit();
        }
    }

    private static void fixMCRFSNODESForNode(File node, String content_store, String derivate_id, String storage_base,
        boolean check_only) {
        if (node.isDirectory()) {
            LOGGER.debug("fixMCRFSNODESForNode (directory) : " + node.getAbsolutePath());
            fixDirectoryEntry(node, derivate_id, storage_base, check_only);
            File[] nodes = node.listFiles();
            for (File next_node : nodes) {
                fixMCRFSNODESForNode(next_node, content_store, derivate_id, storage_base, check_only);
            }
        } else {
            if (node.getName().equals("mcrdata.xml")) {
                return;
            }
            LOGGER.debug("fixMCRFSNODESForNode (file) : " + node.getAbsolutePath());
            fixFileEntry(node, content_store, derivate_id, storage_base, check_only);
        }

    }

    private static void fixDirectoryEntry(File node, String derivate_id, String storage_base, boolean check_only) {
        String name = node.getName();
        LOGGER.debug("fixDirectoryEntry : name = " + node.getName());
        int counter = 0;
        String id = "";
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            tx.begin();
        }
        try {
            Criteria criteria_directory = session.createCriteria(MCRFSNODES.class);
            criteria_directory.add(Restrictions.eq("name", name));
            criteria_directory.add(Restrictions.eq("owner", derivate_id));
            criteria_directory.add(Restrictions.eq("type", "D"));
            ScrollableResults fsnodes = criteria_directory.scroll(ScrollMode.FORWARD_ONLY);
            while (fsnodes.next()) {
                counter++;
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                id = fsNode.getId();
                session.evict(fsNode);
            }
            fsnodes.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (counter == 1) {
            LOGGER.debug("Found directory entry for " + name);
            return;
        }
        if (counter < 1) {
            LOGGER.error("Can't find directory entry for " + name);
            if (check_only)
                return;
        }
        if (counter > 1) {
            LOGGER.error("Non unique directory entry for " + name);
            return;
        }
        // fix entry
        LOGGER.info("Fix entry for directory " + name);
        if (id.length() == 0) {
            MCRFileMetadataManager fmmgr = MCRFileMetadataManager.instance();
            id = fmmgr.createNodeID();
        }
        String pid = getParentID(node, derivate_id);
        if (pid != null && pid.length() == 0) {
            LOGGER.error("Parent id for directory " + node.getParentFile().getName() + " is not unique");
            return;
        }
        if (pid == null && !derivate_id.equals(name)) {
            LOGGER.error("Can't find parent id for directory " + name);
            return;
        }
        try {
            MCRFSNODES mcrfsnodes = new MCRFSNODES();
            mcrfsnodes.setId(id);
            mcrfsnodes.setPid(pid);
            mcrfsnodes.setType("D");
            mcrfsnodes.setOwner(derivate_id);
            mcrfsnodes.setName(node.getName());
            mcrfsnodes.setDate(new Timestamp(new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault())
                .getTime().getTime()));
            session.save(mcrfsnodes);
            tx.commit();
            LOGGER.debug("Entry " + name + " fixed.");
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
        LOGGER.debug("fixFileEntry : name = " + node.getName());
        String storageid = node.getAbsolutePath().substring(storage_base.length());
        LOGGER.debug("fixFileEntry : storageid = " + storageid);
        int counter = 0;
        String id = "";
        String md5_old = "";
        long size_old = 0;
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            tx.begin();
        }
        try {
            Criteria criteria_file = session.createCriteria(MCRFSNODES.class);
            criteria_file.add(Restrictions.eq("storeid", content_store));
            criteria_file.add(Restrictions.eq("storageid", storageid));
            criteria_file.add(Restrictions.eq("owner", derivate_id));
            criteria_file.add(Restrictions.eq("type", "F"));
            ScrollableResults fsnodes = criteria_file.scroll(ScrollMode.FORWARD_ONLY);
            while (fsnodes.next()) {
                counter++;
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                id = fsNode.getId();
                md5_old = fsNode.getMd5();
                size_old = fsNode.getSize();
                session.evict(fsNode);
            }
            fsnodes.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (counter == 1) {
            LOGGER.debug("Found file entry for " + storageid);
        }
        if (counter < 1) {
            LOGGER.error("Can't find file entry for " + storageid);
            if (check_only)
                return;
        }
        if (counter > 1) {
            LOGGER.error("Non unique file entry for " + storageid);
            return;
        }
        // check fctid, size and MD5 of the file
        String fctid = "";
        String md5 = "";
        try {
            MCRContentInputStream cis = new MCRContentInputStream(new FileInputStream(node));
            byte[] header = cis.getHeader();
            fctid = MCRFileContentTypeFactory.detectType(node.getName(), header).getID();
            cis.close();
            md5 = Files.hash(node, Hashing.md5()).toString();
        } catch (MCRException | IOException e1) {
            e1.printStackTrace();
            return;
        }
        long size = node.length();
        LOGGER.debug("size old : " + Long.toString(size_old) + " <--> size : " + Long.toString(size));
        LOGGER.debug("MD5 old : " + md5_old + " <--> MD5 : " + md5);
        if (size_old == size && md5_old.equals(md5)) {
            return;
        }
        if (size_old != size && size_old != 0) {
            LOGGER.warn("Wrong file size for " + storageid + " : " + size_old + " <-> " + size);
        }
        if (!md5_old.equals(md5) && md5_old.length() != 0) {
            LOGGER.warn("Wrong file md5 for " + storageid + " : " + md5_old + " <-> " + md5);
        }
        if (check_only)
            return;
        // fix entry
        LOGGER.info("Fix entry for file " + storageid);
        if (id.length() == 0) {
            MCRFileMetadataManager fmmgr = MCRFileMetadataManager.instance();
            id = fmmgr.createNodeID();
        }
        String pid = getParentID(node, derivate_id);
        if (pid != null && pid.length() == 0) {
            LOGGER.error("Parent id for directory " + node.getParentFile().getName() + " is not unique");
            return;
        }
        if (pid == null) {
            LOGGER.error("Can't find parent id of directory for file " + storageid);
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
            mcrfsnodes.setDate(new Timestamp(new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault())
                .getTime().getTime()));
            mcrfsnodes.setStoreid(content_store);
            mcrfsnodes.setStorageid(storageid);
            mcrfsnodes.setFctid(fctid);
            mcrfsnodes.setMd5(md5);
            session.saveOrUpdate(mcrfsnodes);
            tx.commit();
            LOGGER.debug("Entry " + node.getName() + " fixed.");
        } catch (HibernateException he) {
            if (tx != null) {
                tx.rollback();
            }
            he.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getParentID(File node, String derivate_id) {
        Session session = MCRHIBConnection.instance().getSession();
        File parent_node = node.getParentFile();
        Criteria criteria_file = session.createCriteria(MCRFSNODES.class);
        criteria_file.add(Restrictions.eq("name", parent_node.getName()));
        criteria_file.add(Restrictions.eq("owner", derivate_id));
        criteria_file.add(Restrictions.eq("type", "D"));
        ScrollableResults fsnodes = criteria_file.scroll(ScrollMode.FORWARD_ONLY);
        try {
            int counter = 0;
            String pid = null;
            while (fsnodes.next()) {
                counter++;
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                pid = fsNode.getId();
                session.evict(fsNode);
            }
            if (counter > 1) {
                LOGGER.error("The directory entry for " + derivate_id + " and " + parent_node.getName()
                    + " is not unique!");
                return "";
            }
            return pid;
        } catch (Exception e) {
            return "";
        }

    }

    private static ArrayList<String> getDetivatesOfProject(String content_store, String project_id) {
        ArrayList<String> derivates = new ArrayList<String>();
        // get the IFS1.5
        MCRConfiguration config = MCRConfiguration.instance();
        String content_store_basepath = config.getString("MCR.IFS.ContentStore." + content_store + ".BaseDir", "");
        if (content_store_basepath.length() == 0) {
            LOGGER
                .error("Cant find base directory property in form MCR.IFS.ContentStore." + content_store + ".BaseDir");
            return derivates;
        }
        String slot_layout = config.getString("MCR.IFS.ContentStore." + content_store + ".SlotLayout", "");
        if (slot_layout.length() == 0) {
            LOGGER
                .error("Cant find slot layout property in form MCR.IFS.ContentStore." + content_store + ".SlotLayout");
            return derivates;
        }
        File project_dir = new File(content_store_basepath, project_id);
        if (!project_dir.exists()) {
            LOGGER.error("Wrong project ID; can't find directory " + project_dir.getAbsolutePath());
            return derivates;
        }
        File derivate_dir = new File(project_dir, "derivate");
        int max_slot_deep = countCharacter(slot_layout, '-', 0) + 1;
        searchRecurive(derivates, derivate_dir, max_slot_deep, 0, project_id);
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
}