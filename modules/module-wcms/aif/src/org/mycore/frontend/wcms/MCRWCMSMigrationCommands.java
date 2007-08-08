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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

public class MCRWCMSMigrationCommands extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRWCMSMigrationCommands.class.getName());

    public MCRWCMSMigrationCommands() {
        super();
        MCRCommand com = null;

        com = new MCRCommand("m", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.doMigration",
                        "The command migrates WCMSUserDB.xml into MyCoRe-User-Management.");
        command.add(com);
        
        com = new MCRCommand("s", "org.mycore.frontend.wcms.MCRWCMSMigrationCommands.doMigration",
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
        //2.2
        setupUser(dbOrig);

        //2.3
        
        
    }

    private static void setupUser(Document userDB) {
        // get users
        XPath xpath;
        List users = null;
        try {
            xpath = XPath.newInstance("//user");
            users = xpath.selectNodes(userDB);
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        Iterator userIt = users.iterator();
        MCRUserMgr uMan = MCRUserMgr.instance();
        while (userIt.hasNext()) {
            Element user = (Element)userIt.next();
            String userID = user.getAttributeValue("userID");
            // verify if exist in system and if not add it
            if (!uMan.existUser(userID)) {
                String userName = user.getAttributeValue("userRealName");
                MCRUser userNew = new MCRUser();
                userNew.getUserContact().setLastName(userName);
                userNew.setPassword(userID);
                uMan.createUser(userNew);
                LOGGER.debug("user="+userID+" ("+userName+") missed, so added it\n");
            }
        }
        LOGGER.debug("2.2 successfully \n");               
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
            Element categ = (Element)rootNodeIt.next();
            String rootNode=categ.getTextTrim();
            rootNodesTable.put(rootNode, "");
        }
        LOGGER.debug(getSimText(simulate)+"2.1 successfully \n");
        return rootNodesTable;
    }

    private static void migrateNavi(boolean simulate, Properties props) {
        String navLoc = props.getProperty("MCR.navigationFile");
        Document nav = loadXML(navLoc);
        // save original navi 
        String backupLoc = navLoc+".orig";
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
        LOGGER.debug(getSimText(simulate)+"navi migrated successfully, original navi backed up under "+backupLoc+" \n");
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
        Document nav=null;
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