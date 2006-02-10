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

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * This class provides a set of commands for the org.mycore.access management
 * which can be used by the command line interface.
 * 
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRAccessCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRAccessCommands.class.getName());

    /**
     * The constructor.
     */
    public MCRAccessCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("load permissions data from file {0}", "org.mycore.frontend.cli.MCRAccessCommands.loadPermissionsFromFile String", "The command loads the permissions data of the access control system with data from the file {0}.");
        command.add(com);
        
        com = new MCRCommand("update permissions data from file {0}", "org.mycore.frontend.cli.MCRAccessCommands.updatePermissionsFromFile String", "The command update the permissions data of the access control system with data from the file {0}.");
        command.add(com);

        com = new MCRCommand("list all permissions", "org.mycore.frontend.cli.MCRAccessCommands.listAllPermissions", "");
        command.add(com);

        com = new MCRCommand("delete all permissions", "org.mycore.frontend.cli.MCRAccessCommands.deleteAllPermissions", "");
        command.add(com);
        
        com = new MCRCommand("save all permissions to file {0}", "org.mycore.frontend.cli.MCRAccessCommands.saveAllPermissionsToFile String", "");
        command.add(com);
    }

    /**
     * Check the file name
     * 
     * @param filename
     *            the filename of the user data input
     * @return true if the file name is okay
     */
    private static final boolean checkFilename(String filename) {
        if (!filename.endsWith(".xml")) {
            LOGGER.warn(filename + " ignored, does not end with *.xml");

            return false;
        }

        if (!new File(filename).isFile()) {
            LOGGER.warn(filename + " ignored, is not a file.");

            return false;
        }

        return true;
    }

    /**
     * This method sets the new permissions given in a certain file
     * 
     * @param filename 
     *            the filename of the file that contains the mcrpermissions
     *            
     */    
    public static void createPermissionsFromFile(String filename) {
    	MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
        if (!checkFilename(filename)) {
            return;
        }

        LOGGER.info("Reading file " + filename + " ...");

        try {
            org.jdom.Document doc = MCRXMLHelper.parseURI(filename, true);
            org.jdom.Element rootelm = doc.getRootElement();

            if (!rootelm.getName().equals("mcrpermissions")) {
                throw new MCRException("The data are not for mcrpermissions.");
            }

            List listelm = rootelm.getChildren("mcrpermission");

            for (Iterator it = listelm.iterator(); it.hasNext();) {
            	Element mcrpermission = (Element) it.next();
            	String permissionName = mcrpermission.getAttributeValue("name");
            	String ruleDescription = mcrpermission.getAttributeValue("ruledescription");
            	if(ruleDescription == null)
            		ruleDescription = "";
            	Element rule = (Element)mcrpermission.getChild("condition").clone();
            	AI.addRule(permissionName, rule, ruleDescription);
			}
        } catch (Exception e) {
            throw new MCRException("Error while loading permissions data.", e);
        }    	
    }
    
    /**
     * This method deletes the old permissions (if given any) 
     *  and sets the new permissions given in a certain file
     * 
     * @param filename 
     *            the filename of the file that contains the mcrpermissions
     * @see same as updatePermissionsFromFile            
     */
    public static void loadPermissionsFromFile(String filename) throws Exception {
    	updatePermissionsFromFile(filename);
    }

    /**
     * This method deletes the old permissions (if given any) 
     *  and sets the new permissions given in a certain file
     * 
     * @param filename 
     *            the filename of the file that contains the mcrpermissions
     */
    public static void updatePermissionsFromFile(String filename) throws Exception {
    	deleteAllPermissions();
    	createPermissionsFromFile(filename);
    }
    
    /**
     * This method deletes the old permissions (if given any) 
     * 
     * @param filename 
     *            the filename of the file that contains the mcrpermissions
     */
    public static void deleteAllPermissions() throws Exception {
    	MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
    	for(Iterator it = AI.getPermissions().iterator(); it.hasNext();) {
    		String permission = (String) it.next();
    		AI.removeRule(permission);
    	}
    }    

    /**
     * This method invokes MCRUserMgr.getAllPrivileges() and retrieves a
     * ArrayList of all privileges stored in the persistent datastore.
     */
    public static void listAllPermissions() throws MCRException {
        try {
        	MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            List permissions = AI.getPermissions();
            boolean noPermissionsDefined = true;
            for (Iterator it = permissions.iterator(); it.hasNext();) {
            	noPermissionsDefined = false;
				String permission = (String) it.next();
				String description = AI.getRuleDescription(permission);
				if(description.equals("")) description = permission;
                System.out.println("       " + permission);
                System.out.println("           " + description);
			}
            if(noPermissionsDefined) 
            	System.out.println("No permissions defined");
            System.out.println();
        } catch (Exception e) {
            throw new MCRException("Error while command saveAllGroupsToFile()", e);
        }
    }    


    /**
     * This method just saves the permissions to a file
     * 
     * @param filename
     *            the file written to
     */
    public static final void saveAllPermissionsToFile(String filename) throws MCRException {
    	MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

        try {
        	Document doc = new Document(new Element("mcrpermissions"));
        	List permissions = AI.getPermissions();
        	for (Iterator it = permissions.iterator(); it.hasNext();) {
        		String permission = (String) it.next();
        		Element mcrpermission = new Element("mcrpermission");
        		mcrpermission.setAttribute("name", permission);
        		String ruleDescription = AI.getRuleDescription(permission);
        		if(!ruleDescription.equals("")){
        			mcrpermission.setAttribute("ruledescription", ruleDescription);
        		}
        		Element rule = AI.getRule(permission);
        		mcrpermission.addContent(rule);
        		doc.addContent(mcrpermission);
			}
        	File file = new File(filename);
        	if(file.exists()){
        		System.out.println("File yet exists");
        		return;
        	}
        	FileOutputStream fos = new FileOutputStream(file);
        	LOGGER.info("Writing to file " + filename + " ...");
        	String mcr_encoding = CONFIG.getString("MCR.metadata_default_encoding", MCRDefaults.ENCODING);
        	XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));
        	out.output(doc, fos);
        } catch (Exception e) {
            throw new MCRException("Error while loading permissions data.", e);
        }   
    }
}
