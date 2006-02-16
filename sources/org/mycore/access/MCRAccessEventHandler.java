/**
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

// package
package org.mycore.access;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class holds all EventHandler methods to manage the access part of the
 * simple workflow.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRAccessEventHandler extends MCREventHandlerBase {

    // the logger
    private static Logger LOGGER = Logger.getLogger(MCRAccessEventHandler.class);
    private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
    
    /**
     * This method will be used to create the access rules for SWF for a
     * MCRObject.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        List li = AI.getPermissionsForID(obj.getId().getId());
        int aclsize = 0; 
        if (li != null) { aclsize = li.size(); }
        int rulesize = obj.getService().getRulesSize();
        if((rulesize == 0) && (aclsize == 0)) {
        	setDefaultPermissions(obj, true);
            LOGGER.warn("The ACL conditions for this object are empty!");
        }
        while(0 < rulesize) {
            org.jdom.Element conditions = obj.getService().getRule(0).getCondition();
            String pool = obj.getService().getRule(0).getPermission();
            MCRAccessManager.addRule(obj.getId(), pool, conditions, "");
            obj.getService().removeRule(0);
            rulesize--;
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in " + diff + " sec.");
    }

    /**
     * This method will be used to update the access rules for SWF for a
     * MCRObject.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        List li = AI.getPermissionsForID(obj.getId().getId());
        int aclsize = 0; 
        if (li != null) { aclsize = li.size(); }
        int rulesize = obj.getService().getRulesSize();
        if((rulesize == 0) && (aclsize == 0)) {
            setDefaultPermissions(obj, false);
            LOGGER.warn("The ACL conditions for this object are empty!");
        }
        while(0 < rulesize){
            org.jdom.Element conditions = obj.getService().getRule(0).getCondition();
            String pool = obj.getService().getRule(0).getPermission();
            MCRAccessManager.updateRule(obj.getId(), pool, conditions, "");
            obj.getService().removeRule(0);
            rulesize--;
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler update: done in " + diff + " sec.");
    }

    /**
     * This method will be used to delete the access rules for SWF for a
     * MCRObject.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRAccessManager.removeAllRules(obj.getId());

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler delete: done in " + diff + " sec.");
    }

    /**
     * This method will be used to repair the access rules for SWF for a
     * MCRObject.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        // Do nothing
    }
    
    /**
     * This method sets Default Rules to all permissions that are configured.
     * if <i>overwrite</i> = true, then the old permission entries that are in the database 
     * are overwritten, else not.
     * @param obj
     * @param overwrite
     */
    private void setDefaultPermissions(MCRObject obj, boolean overwrite) {
    	List savedPermissions = MCRAccessManager.getPermissionsForID(obj.getId());
    	List configuredPermissions = AI.getAccessPermissionsFromConfiguration();
    	for (Iterator it = configuredPermissions.iterator(); it.hasNext();) {
			String permission = (String) it.next();
			if(savedPermissions != null && savedPermissions.contains(permission)) {
				if(overwrite) {
					MCRAccessManager.removeRule(obj.getId(), permission);
					MCRAccessManager.addRule(obj.getId(), permission, MCRAccessManager.getFalseRule(),"");					
				}
			}else{
				MCRAccessManager.addRule(obj.getId(), permission, MCRAccessManager.getFalseRule(), "" );
			}
		}
    }

}
