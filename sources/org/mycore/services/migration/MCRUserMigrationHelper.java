/*
 * 
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

package org.mycore.services.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUserMgr;

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
class MCRUserMigrationHelper {

    private static Logger LOGGER = Logger.getLogger(MCRUserMigrationHelper.class.getName());

    static File getUserFile() {
        return new File(getBaseDirectory(), "user-data.xml");
    }

    static File getGroupFile() {
        return new File(getBaseDirectory(), "group-data.xml");
    }

    private static File getBaseDirectory() {
        Properties props = (Properties) (MCRConfiguration.instance().getProperties());
        String workingDir = props.getProperty("MCR.BaseDirectory", null);
        if (workingDir == null) {
            workingDir = System.getProperty("java.io.tmpdir");
        }

        File dir = new File(workingDir, "user-migration");
        dir.mkdirs();
        return dir;
    }

    @SuppressWarnings("all")
    static final void dropTables() throws SQLException {
        try {
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
            throw e;
        }
        Session session = MCRHIBConnection.instance().getSession();
        session.clear();
    }

    static final void deleteTempFiles() {
        try {
            File groupFile = getGroupFile();
            if (groupFile.getAbsolutePath().endsWith(".xml")) {
                File f1org = new File(groupFile.getAbsolutePath() + ".org.xml");
                f1org.delete();
            }
            groupFile.delete();

            File userFile = getUserFile();
            if (userFile.getAbsolutePath().endsWith(".xml")) {
                File f1org = new File(userFile.getAbsolutePath() + ".org.xml");
                f1org.delete();
            }
            userFile.delete();

            File baseDir = getBaseDirectory();
            baseDir.delete();

        } catch (Exception e) {
            LOGGER.debug("Failed to cleanup temp files", e);
        }
    }

    /*
     * We have to delete all group member information, since we create the
     * groups before the users we also delete all groups - allready created by
     * "init superuser"
     */
    static void cleanupGroupFile() throws Exception {
        StringBuffer useradmins = new StringBuffer();
        StringBuffer groupadmins = new StringBuffer();
        try {
            final File groupFile = getGroupFile();
            copyFile(groupFile, new File(groupFile.getAbsolutePath() + ".org.xml"));
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
            throw e;
        }
    }

    /*
     * We have to delete all users - allready created by "init superuser"
     */
    static void cleanupUserFile() {
        try {
            final File userFile = getUserFile();
            copyFile(userFile, new File(userFile.getAbsolutePath() + ".org.xml"));
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

    static void updateAdmins() {
        SAXBuilder sb = new SAXBuilder();
        MCRUserMgr mgr = MCRUserMgr.instance();
        try {
            Document doc = sb.build(getGroupFile());
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