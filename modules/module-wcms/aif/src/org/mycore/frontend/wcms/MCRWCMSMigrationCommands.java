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

package org.mycore.frontend.wcms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

public class MCRWCMSMigrationCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRWCMSMigrationCommands.class.getName());

    public MCRWCMSMigrationCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("m", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.migrate",
                        "The command migrates WCMSUserDB.xml into MyCoRe-User-Management.");
        command.add(com);

        com = new MCRCommand("s", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.simulateMigrate",
                        "NO database actions will be done. The command simulates a migration of WCMSUserDB.xml into MyCoRe-User-Management.");
        command.add(com);

    }

    public static final void simulateMigrate() {
        doMigration(true);
    }

    public static final void migrate() {
        doMigration(false);
    }

    private static final void doMigration(boolean simulate) {
        // 1
        Properties props = (Properties) (MCRConfiguration.instance().getProperties().clone());
        migrateNavi(simulate, props);
        // 2.1
        Document dbOrig = loadXML(props.getProperty("MCR.WCMS.wcmsUserDBFile"));
        Hashtable ht1 = getRootNodes(simulate, props, dbOrig);
        // 2.2
        // setupUser(simulate, dbOrig);
        // 2.3
        Hashtable ht3 = getUserAndGroups(simulate, ht1, dbOrig);
        // 3.1
        createGroups(simulate, ht3);

        // 3.2
        assignUsers(simulate, ht3, dbOrig);
    }

    private static void assignUsers(boolean simulate, Hashtable userAndGroups, Document userDB) {
        MCRUserMgr uMan = MCRUserMgr.instance();
        for (Enumeration e = userAndGroups.keys(); e.hasMoreElements();) {
            String userList = (String) e.nextElement();
            String group = getGroupDescrPrefix() + userAndGroups.get(userList).toString();
            // seperate users
            StringTokenizer tok = new StringTokenizer(userList, getUserSeperator());
            while (tok.hasMoreTokens()) {
                String user = tok.nextToken().toString();
                // assign to group
                MCRGroup mcrGroup = uMan.retrieveGroup(group);
                if (mcrGroup.hasUserMember(user)) {
                    LOGGER.debug(getSimText(simulate) + "user=" + user + " not added as member to group=" + group + ", because it already exist");
                } else {
                    // user exist ?
                    if (!uMan.existUser(user))
                        createUser(simulate, userDB, user, mcrGroup);
                    if (!simulate)
                        mcrGroup.addMemberUserID(user);
                    LOGGER.debug(getSimText(simulate) + "added user=" + user + " as member to group=" + group);
                }
            }
        }
        LOGGER.debug(getSimText(simulate) + "user assignment to group finished sucessfully");
    }

    private static void createGroups(boolean simulate, Hashtable userAndGroups) {
        MCRUserMgr uMan = MCRUserMgr.instance();
        for (Enumeration e = userAndGroups.keys(); e.hasMoreElements();) {
            String userList = (String) e.nextElement();
            String groupList = (String) userAndGroups.get(userList);
            String groupName = getGroupDescrPrefix() + groupList;
            MCRGroup group = new MCRGroup();
            group.setID(groupName);
            group.addAdminUserID("root");
            if (uMan.existGroup(groupName))
                LOGGER.debug(getSimText(simulate) + "group=" + group + " already exists, not added in DB");
            else {
                if (!simulate)
                    uMan.createGroup(group);
                LOGGER.debug(getSimText(simulate) + "group=" + group + " added in DB");
            }
        }
    }

    private static String getGroupDescrPrefix() {
        return "WCMS-Editors for: ";
    }

    private static Hashtable getUserAndGroups(boolean simulate, Hashtable ht1, Document userDB) {
        Hashtable userAndGroups = new Hashtable();
        // all root nodes
        for (Enumeration e = ht1.keys(); e.hasMoreElements();) {
            String rootNode = (String) e.nextElement();
            XPath xpath;
            List nodes = null;
            try {
                String xpathEx = "//rootNode[text()='" + rootNode + "']";
                xpath = XPath.newInstance(xpathEx);
                nodes = xpath.selectNodes(userDB);
            } catch (JDOMException e1) {
                e1.printStackTrace();
            }
            // get user belonging to this root node
            Hashtable user4Node = new Hashtable();
            Iterator selNodeIt = nodes.iterator();
            while (selNodeIt.hasNext()) {
                Element node = (Element) selNodeIt.next();
                String user = node.getParentElement().getAttributeValue("userID");
                user4Node.put(user, "");
            }
            // sort users alphabetically
            List user4NodeSorted = new ArrayList();
            Iterator it = user4Node.keySet().iterator();
            while (it.hasNext()) {
                String elem = (String) it.next();
                user4NodeSorted.add(elem);
            }
            Collections.sort(user4NodeSorted);
            // create csv version of user4NodeSorted
            String userList = "";
            Iterator it2 = user4NodeSorted.iterator();
            while (it2.hasNext()) {
                String user = (String) it2.next();
                userList = userList + getUserSeperator() + user.trim();
            }
            // add user list with belonging root nodes to hashmap
            String groups = "";
            if (userAndGroups.containsKey(userList))
                groups = userAndGroups.get(userList) + getGroupSeperator() + rootNode;
            else
                groups = rootNode;
            userAndGroups.put(userList, groups);
            LOGGER.debug(getSimText(simulate) + "calculated user=" + userList + " for group(s)=" + groups);
        }
        LOGGER.debug(getSimText(simulate) + "2.3 successfully \n");
        return userAndGroups;
    }

    private static String getUserSeperator() {
        return "#$#$#$#";
    }

    private static String getGroupSeperator() {
        return " AND ";
    }

    private static void createUser(boolean simulate, Document userDB, String userID, MCRGroup group) {
        // get users
        XPath xpath;
        Element user = null;
        try {
            xpath = XPath.newInstance("//user[@userID='" + userID + "']");
            user = (Element) xpath.selectSingleNode(userDB);
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        // verify if exist in system and if not add it
        MCRUserMgr uMan = MCRUserMgr.instance();
        if (!uMan.existUser(userID)) {
            String userName = user.getAttributeValue("userRealName");
            MCRUser userNew = new MCRUser();
            userNew.addGroupID(group.getID());
            userNew.getUserContact().setLastName(userName);
            userNew.setPassword(userID);
            if (!simulate) {
                uMan.createUser(userNew);
            }
            LOGGER.debug(getSimText(simulate) + "user=" + userID + " (" + userName + ") missed, so created it\n");
        }
    }

    private static Hashtable getRootNodes(boolean simulate, Properties props, Document userDB) {
        XPath xpath;
        List rootNodes = null;
        try {
            xpath = XPath.newInstance("//rootNode");
            rootNodes = xpath.selectNodes(userDB);
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        Hashtable rootNodesTable = new Hashtable(rootNodes.size());
        Iterator rootNodeIt = rootNodes.iterator();
        while (rootNodeIt.hasNext()) {
            Element categ = (Element) rootNodeIt.next();
            String rootNode = categ.getTextTrim();
            rootNodesTable.put(rootNode, "");
        }
        LOGGER.debug(getSimText(simulate) + "2.1 successfully \n");
        return rootNodesTable;
    }

    private static void migrateNavi(boolean simulate, Properties props) {
        String navLoc = props.getProperty("MCR.navigationFile");
        Document nav = loadXML(navLoc);
        // save original navi
        String backupLoc = navLoc + ".orig";
        if (!simulate)
            saveXML(backupLoc, nav);
        // set needed @href's
        nav.getRootElement().setAttribute("href", nav.getRootElement().getName());
        Iterator menuIt = nav.getRootElement().getChildren().iterator();
        while (menuIt.hasNext()) {
            Element menu = (Element) menuIt.next();
            menu.setAttribute("href", menu.getName());
        }
        if (!simulate)
            saveXML(navLoc, nav);
        LOGGER.debug(getSimText(simulate) + "navi migrated successfully, original navi backed up under " + backupLoc + " \n");
    }

    private static String getSimText(boolean simulate) {
        if (simulate) {
            return "NO ACTION - SIMULATION: ";
        } else {
            return "";
        }
    }

    private static Document loadXML(String location) {
        SAXBuilder sax = new SAXBuilder();
        Document nav = null;
        try {
            nav = sax.build(new File(location));
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nav;
    }

    private static void saveXML(String location, Document document) {
        XMLOutputter out = new XMLOutputter();
        try {
            out.output(document, new FileOutputStream(new File(location)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}