/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * Update of User Management to comply with new MyCoRe 2.0
 * 
 * Therefore we first checkout all users and groups, drop the concerning tables
 * and check all users and groups back in. Before check-in the new user tables
 * will be automatically created by Hibernate which takes care of new primary
 * key and foreign key constraints.
 * 
 * 
 * @author Robert Stephan
 */
public class MCRUserMigrationCommands extends MCRAbstractCommands {
    private static boolean DEBUG = true;

    private static Logger LOGGER = Logger.getLogger(MCRUserMigrationCommands.class.getName());

    public MCRUserMigrationCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("migrate users", "org.mycore.user.MCRUserMigrationCommands.createMigrationCommands",
                "The command migrates the user management to MyCoRe 2.0.");
        command.add(com);

        com = new MCRCommand("private migrate users drop tables", "org.mycore.user.MCRUserMigrationCommands.dropTables",
                "This private command drops the tables for user managment migration to MyCoRe 2.0");
        command.add(com);

        com = new MCRCommand("private migrate user cleanup group file {0}", "org.mycore.user.MCRUserMigrationCommands.cleanupGroupFile String",
                "This private command removes existing (initial) groups from the exported group data file.");
        command.add(com);

        com = new MCRCommand("private migrate user cleanup user file {0}", "org.mycore.user.MCRUserMigrationCommands.cleanupUserFile String",
                "This private command removes existing (initial) users from the exported user data file.");
        command.add(com);

        com = new MCRCommand("private migrate user delete temp files {0} {1} {2}",
                "org.mycore.user.MCRUserMigrationCommands.deleteTempFiles String String String",
                "This private command removes existing (initial) users from the exported user data file.");
        command.add(com);

        com = new MCRCommand("private migrate user update admins from file {0}", "org.mycore.user.MCRUserMigrationCommands.updateAdmins String",
                "This private command restores previously saved admin information.");
        command.add(com);

    }

    // public static final void migrate() {
    // doMigration();
    // }

    public static List<String> createMigrationCommands() {
        List<String> cmds = new ArrayList<String>();
        LOGGER.info("User Migration started\n");
        // 1
        Properties props = (Properties) (MCRConfiguration.instance().getProperties());
        String workingDir = props.getProperty("MCR.BaseDirectory", null);
        if (workingDir == null) {
            workingDir = System.getProperty("java.io.tmpdir");
        }

        File dir = new File(workingDir + "/user-migration");
        dir.mkdirs();
        File groupFile = new File(dir + "/group-data.xml");
        File userFile = new File(dir + "/user-data.xml");

        // MCRUserCommands.saveAllGroupsToFile(groupFile.getAbsolutePath());
        cmds.add("save all groups to file " + groupFile.getAbsolutePath());

        // MCRUserCommands.saveAllUsersToFile(userFile.getAbsolutePath());
        cmds.add("save all users to file " + userFile.getAbsolutePath());

        // Drop tables
        cmds.add("private migrate users drop tables");

        // MCRHIBCtrlCommands.createTables();
        cmds.add("init hibernate");

        // MCRUserCommands.initSuperuser();
        cmds.add("init superuser");

        // cleanupGroupFile(groupFile);
        cmds.add("private migrate user cleanup group file " + groupFile.getAbsolutePath());

        // cleanupUserFile(userFile);
        cmds.add("private migrate user cleanup user file " + userFile.getAbsolutePath());

        // MCRUserCommands.importGroupFromFile(groupFile.getAbsolutePath());
        cmds.add("import group data from file " + groupFile.getAbsolutePath());

        // MCRUserCommands.importUserFromFile(userFile.getAbsolutePath());
        cmds.add("import user data from file " + userFile.getAbsolutePath());

        // MCRUserCommands.importGroupFromFile(groupFile.getAbsolutePath());
        cmds.add("private migrate user update admins from file " + groupFile.getAbsolutePath());

        /*
         * //for debugging
         * //MCRUserCommands.saveAllGroupsToFile(groupFile.getAbsolutePath().replace(".xml",
         * ".new")); cmds.add("save all groups to file
         * "+groupFile.getAbsolutePath().replace(".xml", ".new"));
         * 
         * //MCRUserCommands.saveAllUsersToFile(userFile.getAbsolutePath().replace(".xml",
         * ".new")); cmds.add("save all users to file
         * "+userFile.getAbsolutePath().replace(".xml", ".new"));
         */

        // userFile.delete();
        // groupFile.delete();
        // dir.delete();
        if (!DEBUG)
            cmds.add("private migrate user delete temp files " + userFile.getAbsolutePath() + " " + groupFile.getAbsolutePath() + " " + dir.getAbsolutePath());

        LOGGER.info("Migration der Nutzer beendet\n");

        return cmds;
    }

    @SuppressWarnings("all")
    public static final void dropTables() {
        try {
            /*
             * Session session = MCRHIBConnection.instance().getSession(); Query
             * deleteQuery = session.createQuery( "delete MCRUSER where true");
             * deleteQuery.executeUpdate(); deleteQuery = session.createQuery(
             * "delete MCRGROUP where true"); deleteQuery.executeUpdate();
             * session.clear(); MCRHIBConnection.instance().flushSession();
             */

            Connection con = MCRHIBConnection.instance().getSession().connection();
            Statement stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE MCRGROUPMEMBERS");
            stmt.executeUpdate("DROP TABLE MCRGROUPADMINS");
            stmt.executeUpdate("DROP TABLE MCRUSERS");
            stmt.executeUpdate("DROP TABLE MCRGROUPS");
            stmt.close();
            con.commit();

        } catch (SQLException e) {
            LOGGER.debug("Failed to drop table", e);
        }
        MCRHIBConnection.instance().flushSession();
        Session session = MCRHIBConnection.instance().getSession();
        session.clear();
    }

    public static final void deleteTempFiles(String f1, String f2, String f3) {
        try {
            File file1 = new File(f1);
            if (file1.getAbsolutePath().endsWith(".xml")) {
                File f1org = new File(file1.getAbsolutePath() + ".org.xml");
                f1org.delete();
            }
            file1.delete();

            File file2 = new File(f2);
            if (file2.getAbsolutePath().endsWith(".xml")) {
                File f1org = new File(file2.getAbsolutePath() + ".org.xml");
                f1org.delete();
            }
            file2.delete();

            File file3 = new File(f3);
            if (file3.getAbsolutePath().endsWith(".xml")) {
                File f1org = new File(file3.getAbsolutePath() + ".org.xml");
                f1org.delete();
            }
            file3.delete();

        } catch (Exception e) {
            LOGGER.debug("Failed to cleanup temp files", e);
        }
    }

    // private static final void doMigration() {
    // LOGGER.info("User Migration started\n");
    // // 1
    // Properties props = (Properties)
    // (MCRConfiguration.instance().getProperties());
    // String workingDir = props.getProperty("MCR.BaseDirectory", null);
    // if(workingDir!=null){
    // File dir = new File(workingDir+"/user-migration");
    // dir.mkdirs();
    // File groupFile = new File(dir+"/group-data.xml");
    // File userFile = new File(dir+"/user-data.xml");
    //        	
    // MCRUserCommands.saveAllGroupsToFile(groupFile.getAbsolutePath());
    // MCRUserCommands.saveAllUsersToFile(userFile.getAbsolutePath());
    // MCRHIBConnection.instance().flushSession();
    // try{
    // Connection con = MCRHIBConnection.instance().getSession().connection();
    // Statement stmt = con.createStatement();
    // stmt.executeUpdate("DROP TABLE MCRGROUPMEMBERS");
    // stmt.executeUpdate("DROP TABLE MCRGROUPADMINS");
    // stmt.executeUpdate("DROP TABLE MCRUSERS");
    // stmt.executeUpdate("DROP TABLE MCRGROUPS");
    // stmt.close();
    // con.commit();
    // }catch(SQLException e){
    // LOGGER.debug("Failed to drop table", e);
    // }
    // MCRHIBConnection.instance().flushSession();
    // MCRHIBCtrlCommands.createTables();
    // MCRUserCommands.initSuperuser();
    // MCRHIBConnection.instance().flushSession();
    // evictUserData(userFile);
    // evictGroupData(groupFile);
    // cleanupGroupFile(groupFile.getAbsolutePath());
    // cleanupUserFile(userFile.getAbsolutePath());
    // MCRHIBConnection.instance().flushSession();
    // MCRUserCommands.importGroupFromFile(groupFile.getAbsolutePath());
    // MCRHIBConnection.instance().flushSession();
    // MCRUserCommands.importUserFromFile(userFile.getAbsolutePath());
    //        	
    // MCRUserCommands.saveAllGroupsToFile(groupFile.getAbsolutePath().replace(".xml",
    // ".new"));
    // MCRUserCommands.saveAllUsersToFile(userFile.getAbsolutePath().replace(".xml",
    // ".new"));
    //        	
    // /*userFile.delete();
    // groupFile.delete();
    // dir.delete();*/
    //        	
    // }
    // LOGGER.info("Migration der Nutzer beendet\n");
    // }

    /*
     * We have to delete all group member information, since we create the
     * groups before the users we also delete all groups - allready created by
     * "init superuser"
     */
    public static void cleanupGroupFile(String groupFile) {
        StringBuffer useradmins = new StringBuffer();
        StringBuffer groupadmins = new StringBuffer();
        try {
            copyFile(new File(groupFile), new File(groupFile + ".org.xml"));
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(groupFile);
            // delete group.members
            XPath x = XPath.newInstance("//group.members");
            List list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                e.removeContent();
            }

            // remove empty admins.userID and admins.groupID
            x = XPath.newInstance("//admins.userID");
            list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                if (useradmins.length() > 0)
                    useradmins.append(";");
                useradmins.append(e.getParentElement().getParentElement().getAttributeValue("ID"));
                useradmins.append("=");
                useradmins.append(e.getText());

                e.detach();
            }

            x = XPath.newInstance("//admins.groupID");
            list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                if (groupadmins.length() > 0)
                    groupadmins.append(";");
                groupadmins.append(e.getParentElement().getParentElement().getAttributeValue("ID"));
                groupadmins.append("=");
                groupadmins.append(e.getText());

                e.detach();
            }

            // deltete existing Groups
            x = XPath.newInstance("//group");
            list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                if (MCRUserMgr.instance().existGroup(e.getAttributeValue("ID"))) {
                    e.getParent().removeContent(e);
                }
            }
            doc.addContent(new Comment(useradmins.toString() + "|" + groupadmins.toString()));
            XMLOutputter xmlOut = new XMLOutputter();
            xmlOut.output(doc, new OutputStreamWriter(new FileOutputStream(groupFile), "utf-8"));
        } catch (Exception e) {
            LOGGER.debug("Error in user migration while modifying group members", e);
        }
    }

    /*
     * We have to delete all users - allready created by "init superuser"
     */
    public static void cleanupUserFile(String userFile) {
        try {
            copyFile(new File(userFile), new File(userFile + ".org.xml"));
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(userFile);

            // deltete existing Users
            XPath x = XPath.newInstance("//user");
            List list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                if (MCRUserMgr.instance().existUser(e.getAttributeValue("ID"))) {
                    e.getParent().removeContent(e);
                }
            }
            XMLOutputter xmlOut = new XMLOutputter();
            xmlOut.output(doc, new OutputStreamWriter(new FileOutputStream(userFile), "utf-8"));
        } catch (Exception e) {
            LOGGER.debug("Error in user migration while modifying group members", e);
        }
    }

    public static void updateAdmins(String groupFile) {
        SAXBuilder sb = new SAXBuilder();
        MCRUserMgr mgr = MCRUserMgr.instance();
        try {
            Document doc = sb.build(groupFile);
            for (Object o : doc.getContent()) {
                LOGGER.info(o.getClass().getCanonicalName());
                if (o instanceof Comment) {
                    LOGGER.info("Found Comment");
                    String s = ((Comment) o).getText();
                    String[] sa = s.split("\\|");
                    String users = sa[0];
                    String groups = "";
                    if (sa.length > 1)
                        groups = sa[1];
                    String[] userA = users.split(";");
                    String[] groupA = groups.split(";");
                    for (String x : userA) {
                        final String[] groupAdmin = x.split("=");
                        if (groupAdmin.length > 1) {
                            String id = groupAdmin[0];
                            String value = groupAdmin[1];
                            MCRGroup mcrGroup = mgr.retrieveGroup(id);
                            mcrGroup.addAdminUserID(value);
                            mgr.updateGroup(mcrGroup);
                            LOGGER.info("Adding user '" + value + "' to admins of group '" + id + "'.");
                        }
                    }
                    for (String x : groupA) {
                        final String[] groupAdmin = x.split("=");
                        if (groupAdmin.length > 1) {
                            String id = groupAdmin[0];
                            String value = groupAdmin[1];
                            MCRGroup mcrGroup = mgr.retrieveGroup(id);
                            mcrGroup.addAdminGroupID(value);
                            mgr.updateGroup(mcrGroup);
                            LOGGER.info("Adding group '" + value + "' to admins of group '" + id + "'.");
                        }
                    }
                }

            }

        } catch (Exception e) {
            LOGGER.warn("Error in user migration while updateing group admin information", e);
        }
    }

    private static void evictUserData(File userFile) {
        try {
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(userFile);
            Session session = MCRHIBConnection.instance().getSession();
            // deltete existing Users
            XPath x = XPath.newInstance("//user");
            List list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                String id = e.getAttributeValue("ID");
                session.evict(session.get(MCRUser.class, id));
            }
        } catch (Exception e) {
            LOGGER.debug("Error in user migration while modifying group members", e);
        }
    }

    private static void evictGroupData(File groupFile) {
        try {
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(groupFile);
            Session session = MCRHIBConnection.instance().getSession();
            XPath x = XPath.newInstance("//group");
            List list = x.selectNodes(doc);
            for (Object o : list) {
                Element e = (Element) o;
                String id = e.getAttributeValue("ID");
                session.evict(session.get(MCRGroup.class, id));
            }
        } catch (Exception e) {
            LOGGER.debug("Error in user migration while modifying group members", e);
        }
    }

    // just for debugging
    private static void copyFile(File fromFile, File toFile) {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        finally {
            if (from != null)
                try {
                    from.close();
                } catch (IOException e) {
                    ;
                }
            if (to != null)
                try {
                    to.close();
                } catch (IOException e) {
                    ;
                }
        }
    }

}