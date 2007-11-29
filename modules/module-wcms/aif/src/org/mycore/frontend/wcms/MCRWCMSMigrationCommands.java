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
import java.util.Collections;
import java.util.Enumeration;
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
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.user.MCRCrypt;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

public class MCRWCMSMigrationCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRWCMSMigrationCommands.class.getName());

    public MCRWCMSMigrationCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("migrate wcms-users", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.migrate",
                        "The command migrates WCMSUserDB.xml into MyCoRe-User-Management.");
        command.add(com);

        com = new MCRCommand("simulate migrate wcms-users", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.simulateMigrate",
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
        LOGGER.info(getSimText(simulate) + "Migration started\n");
        // 1
        Properties props = (Properties) (MCRConfiguration.instance().getProperties().clone());
        migrateNavi(simulate, props);
        // 2.1
        Document dbOrig = loadXML(props.getProperty("MCR.WCMS.wcmsUserDBFile"));
        Hashtable ht1 = getRootNodes(simulate, props, dbOrig);
        // 2.3
        Hashtable ht3 = getUserAndGroups(simulate, ht1, dbOrig);
        // 3.1
        Hashtable groupDescr_groupID = createGroups(simulate, ht3, props);
        // 3.2
        assignUsers(simulate, ht3, dbOrig, groupDescr_groupID);
        // 3.3
        addACLs(simulate, ht1, ht3, groupDescr_groupID);
        LOGGER.info(getSimText(simulate) + "Migration finished\n");
    }

    private static void addACLs(boolean simulate, Hashtable rootNodes, Hashtable userAndGroups, Hashtable groupDes_groupID) {
        // all root nodes
        for (Enumeration e = rootNodes.keys(); e.hasMoreElements();) {
            String rootNode = (String) e.nextElement();
            // all groups
            Hashtable groups4Rules = new Hashtable();
            for (Enumeration e2 = userAndGroups.keys(); e2.hasMoreElements();) {
                String userList = (String) e2.nextElement();
                String groupDes = getGroupDescrPrefix() + userAndGroups.get(userList).toString();
                boolean rootNodeContained = groupDes.contains(rootNode);
                if (rootNodeContained) {
                    String groupID = groupDes_groupID.get(groupDes).toString();
                    groups4Rules.put(groupID, "");
                }
            }
            // save rules as acl
            String aclObjId = MCRLayoutUtilities.getOBJIDPREFIX_WEBPAGE() + rootNode;
            String writePerm = MCRWCMSUtilities.getWritePermissionWebpage();
            // // build rule as XML
            Element rule = new Element("condition");
            rule.addContent(new Element("boolean").setAttribute("operator", "or"));
            for (Enumeration e3 = groups4Rules.keys(); e3.hasMoreElements();) {
                String groupID = (String) e3.nextElement();
                Element cond = new Element("condition");
                cond.setAttribute("field", "group");
                cond.setAttribute("operator", "=");
                cond.setAttribute("value", groupID);
                rule.getChild("boolean").addContent(cond);
            }
            // // save
            if (!simulate)
                MCRAccessManager.addRule(aclObjId, writePerm, rule, "automatically created ACL for WCMS-Write-Access");
            LOGGER.info(getSimText(simulate) + "saved ACL for object-ID=" + aclObjId + " with rule=" + rule);
        }
        LOGGER.info(getSimText(simulate) + "ACL creation finished, created " + rootNodes.size() + " ACL's\n");
    }

    private static void assignUsers(boolean simulate, Hashtable userAndGroups, Document userDB, Hashtable groupDescr_groupID) {
        MCRUserMgr uMan = MCRUserMgr.instance();
        for (Enumeration e = userAndGroups.keys(); e.hasMoreElements();) {
            String userList = (String) e.nextElement();
            String group4Users = userAndGroups.get(userList).toString();
            String mcrGroupID = (String) groupDescr_groupID.get(getGroupDescrPrefix() + group4Users);
            // seperate users
            StringTokenizer tok = new StringTokenizer(userList, getUserSeperator());
            while (tok.hasMoreTokens()) {
                String user = tok.nextToken().toString();
                // assign to group
                MCRGroup mcrGroup = null;
                if (!simulate)
                    mcrGroup = uMan.retrieveGroup(mcrGroupID);
                if (mcrGroup != null && mcrGroup.hasUserMember(user)) {
                    LOGGER.info(getSimText(simulate) + "user=" + user + " not added as member to group=" + mcrGroupID + ", because it already exist");
                } else {
                    // user exist ?
                    if (!uMan.existUser(user))
                        createUser(simulate, userDB, user, mcrGroup);
                    if (!simulate)
                        mcrGroup.addMemberUserID(user);
                    LOGGER.info(getSimText(simulate) + "added user=" + user + " as member to group=" + mcrGroupID);
                }
            }
        }
        LOGGER.info(getSimText(simulate) + "user assignment to groups finished sucessfully");
    }

    private static Hashtable createGroups(boolean simulate, Hashtable userAndGroups, Properties props) {
        String superUserID = props.getProperty("MCR.Users.Superuser.UserName", "administrator");
        MCRUserMgr uMan = MCRUserMgr.instance();
        int pos = 0;
        Hashtable groupID_groupDes = new Hashtable();
        // all groups
        for (Enumeration e = userAndGroups.keys(); e.hasMoreElements();) {
            pos++;
            String userList = (String) e.nextElement();
            String group = (String) userAndGroups.get(userList);
            String groupName = getGroupDescrPrefix() + group;

            // build mcrGroup
            int mcrGroupIDNum = getMCRGroupID(pos);
            String mcrGroupID = getGroupIDPrefix() + Integer.toString(mcrGroupIDNum);
            MCRGroup mcrGroup = new MCRGroup();
            mcrGroup.setID(mcrGroupID);
            mcrGroup.setDescription(groupName);
            mcrGroup.addAdminUserID(superUserID);
            // store in db
            if (!simulate)
                uMan.createGroup(mcrGroup);
            // store mcrGroupID - groupWithRootNodes
            groupID_groupDes.put(groupName, mcrGroupID);
            LOGGER.info(getSimText(simulate) + "group=" + mcrGroupID + " (" + groupName + ") added in DB");
        }
        LOGGER.info(getSimText(simulate) + "groups creation finished, " + userAndGroups.size() + " groups created");
        return groupID_groupDes;
    }

    private static int getMCRGroupID(int recommendedID) {
        MCRUserMgr uMan = MCRUserMgr.instance();
        int newID = recommendedID;
        while (uMan.existGroup(getGroupIDPrefix() + Integer.toString(newID))) {
            newID++;
        }
        return newID;
    }

    private static String getGroupIDPrefix() {
        return "WCMS_";
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
            if (userAndGroups.containsKey(userList)) {
                groups = userAndGroups.get(userList) + getGroupSeperator() + rootNode;
                // LOGGER.info(getSimText(simulate) + "recalculated user(s)=" +
                // userList + " to group=" + groups);
            } else {
                groups = rootNode;
                // LOGGER.info(getSimText(simulate) + "calculated user(s)=" +
                // userList + " to group=" + groups);
            }
            userAndGroups.put(userList, groups);

        }
        // LOGGER.info(getSimText(simulate) + "2.3 successfully \n");
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
            if (!simulate) {
                // encrypt password if property set
                String useCrypt = CONFIG.getString("MCR.Users.UsePasswordEncryption", "false");
                boolean useEncryption = (useCrypt.trim().equals("true")) ? true : false;
                String password = userID;
                if (useEncryption)
                    password = MCRCrypt.crypt(password);
                // create user
                MCRUser userNew = null;
                try {
                    userNew = new MCRUser(uMan.getMaxUserNumID() + 1, userID, "root", null, null, true, true, "", password, group.getID(), new ArrayList(), "",
                                    "", userName, "", "", "", "", "", "", "", "", "", "", "", "", "");
                } catch (MCRException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                uMan.createUser(userNew);
            }
            LOGGER.info(getSimText(simulate) + "user=" + userID + " (" + userName + ") missed, so created it\n");
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
        // LOGGER.info(getSimText(simulate) + "2.1 successfully \n");
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
        LOGGER.info(getSimText(simulate) + "navi migrated successfully, original navi backed up under " + backupLoc + " \n");
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