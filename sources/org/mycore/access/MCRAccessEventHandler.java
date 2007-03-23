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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;

import org.jdom.Element;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
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

    private static final SAXBuilder SAX_BUILDER = new org.jdom.input.SAXBuilder();

    private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    private static String storedrules = CONFIG.getString("MCR.StorePermissions", "read,write,delete");

    // get the standard read rule from config or it's the true rule
    private static String strReadRule = CONFIG.getString("MCR.AccessRule.STANDARD-READ-RULE", "<condition format=\"xml\"><boolean operator=\"true\" /></condition>");

    private static Element readrule = (Element) MCRXMLHelper.parseXML(strReadRule, false).getRootElement().detach();

    // get the standard edit rule from config or it's the true rule
    private static String strEditRule = CONFIG.getString("MCR.AccessRule.STANDARD-EDIT-RULE", "<condition format=\"xml\"><boolean operator=\"true\" /></condition>");

    private static Element editrule = (Element) MCRXMLHelper.parseXML(strEditRule, false).getRootElement().detach();

    /**
     * This method will be used to create the access rules for SWF for a
     * MCRClassification.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationCreated(MCREvent evt, MCRClassification obj) {
        handleClassificationCreated(obj);
    }

    /**
     * This method will be used to update the access rules for SWF for a
     * MCRClassification.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRlassification that caused the event
     */
    protected void handleClassificationUpdated(MCREvent evt, MCRClassification obj) {
        handleClassificationUpdated(obj);
    }

    /**
     * This method will be used to delete the access rules for SWF for a
     * MCRClassification.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationDeleted(MCREvent evt, MCRClassification obj) {
        handleClassificationDeleted(obj);
    }

    /**
     * This method will be used to repair the access rules for SWF for a
     * MCRClassification.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationRepaired(MCREvent evt, MCRClassification obj) {
        // add default ACLs if it does not exist
        // this is only for the migartion process
        handleClassificationRepaired(obj);
    }

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
        handleBaseCreated(obj);
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
        handleBaseUpdated(obj);
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
        handleBaseDeleted(obj);
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
     * This method will be used to create the access rules for SWF for a
     * MCRDerivate.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleBaseCreated(der);
    }

    /**
     * This method will be used to update the access rules for SWF for a
     * MCRDerivate.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleBaseUpdated(der);
    }

    /**
     * This method will be used to delete the access rules for SWF for a
     * MCRDerivate.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleBaseDeleted(der);
    }

    /**
     * This method will be used to repair the access rules for SWF for a
     * MCRDerivate.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        // Do nothing
    }

    private void handleBaseCreated(MCRBase base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        List li = AI.getPermissionsForID(base.getId().getId());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = base.getService().getRulesSize();
        if ((rulesize == 0) && (aclsize == 0)) {
            setDefaultPermissions(base.getId().getId(), true);
            LOGGER.warn("The ACL conditions for this object are empty!");
        }
        while (0 < rulesize) {
            org.jdom.Element conditions = base.getService().getRule(0).getCondition();
            String permission = base.getService().getRule(0).getPermission();
            if (storedrules.indexOf(permission) != -1) {
                MCRAccessManager.addRule(base.getId(), permission, conditions, "");
            }
            base.getService().removeRule(0);
            rulesize--;
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in " + diff + " sec.");
    }

    private void handleBaseUpdated(MCRBase base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        List li = AI.getPermissionsForID(base.getId().getId());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = base.getService().getRulesSize();
        if ((rulesize == 0) && (aclsize == 0)) {
            setDefaultPermissions(base.getId().getId(), false);
            LOGGER.warn("The ACL conditions for this object was empty!");
        }
        if (aclsize == 0) {
            while (0 < rulesize) {
                org.jdom.Element conditions = base.getService().getRule(0).getCondition();
                String permission = base.getService().getRule(0).getPermission();
                if (storedrules.indexOf(permission) != -1) {
                    MCRAccessManager.updateRule(base.getId(), permission, conditions, "");
                }
                base.getService().removeRule(0);
                rulesize--;
            }
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler update: done in " + diff + " sec.");
    }

    private void handleBaseDeleted(MCRBase base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRAccessManager.removeAllRules(base.getId());

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler delete: done in " + diff + " sec.");
    }

    private void handleClassificationCreated(MCRClassification base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        List li = AI.getPermissionsForID(base.getId().getId());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = 0;
        try {
            rulesize = base.getService().getRulesSize();
        } catch (Exception e) {
        }
        if ((rulesize == 0) && (aclsize == 0)) {
            handleClassificationRepaired(base);
            LOGGER.warn("The ACL conditions for this classification was empty!");
        }
        while (0 < rulesize) {
            org.jdom.Element conditions = base.getService().getRule(0).getCondition();
            String permission = base.getService().getRule(0).getPermission();
            if (storedrules.indexOf(permission) != -1) {
                MCRAccessManager.addRule(base.getId(), permission, conditions, "");
            }
            base.getService().removeRule(0);
            rulesize--;
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in " + diff + " sec.");
    }

    private void handleClassificationUpdated(MCRClassification base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        List li = AI.getPermissionsForID(base.getId().getId());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = 0;
        try {
            rulesize = base.getService().getRulesSize();
        } catch (Exception e) {
        }
        if ((rulesize == 0) && (aclsize == 0)) {
            handleClassificationRepaired(base);
            LOGGER.warn("The ACL conditions for this classification was empty!");
        }
        if (aclsize == 0) {
            while (0 < rulesize) {
                org.jdom.Element conditions = base.getService().getRule(0).getCondition();
                String permission = base.getService().getRule(0).getPermission();
                if (storedrules.indexOf(permission) != -1) {
                    MCRAccessManager.addRule(base.getId(), permission, conditions, "");
                }
                base.getService().removeRule(0);
                rulesize--;
            }
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in " + diff + " sec.");
    }

    private void handleClassificationDeleted(MCRClassification base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRAccessManager.removeAllRules(base.getId());

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler delete: done in " + diff + " sec.");
    }

    private void handleClassificationRepaired(MCRClassification base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // add default ACLs if it does not exist
        // this is only for the migartion process
        List li = AI.getPermissionsForID(base.getId().getId());
        if (li.size() == 0) {
            // Read default XML definition
            try {
                InputStream aclxml = MCRAccessEventHandler.class.getResourceAsStream("/editor_default_acls_classification.xml");
                org.jdom.Document xml = (SAX_BUILDER).build(aclxml);
                org.jdom.Element acls = xml.getRootElement().getChild("servacls");
                List acllist = acls.getChildren("servacl");
                for (int i = 0; i < acllist.size(); i++) {
                    org.jdom.Element acl = (org.jdom.Element) acllist.get(i);
                    org.jdom.Element conditions = acl.getChild("condition");
                    String permission = acl.getAttributeValue("permission");
                    if (storedrules.indexOf(permission) != -1) {
                        MCRAccessManager.addRule(base.getId(), permission, conditions, "");
                        LOGGER.info("Add Permission " + permission + " for " + base.getId().getId() + ".");
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error while parsing file editor_default_acls_classification.xml.");
                setDefaultPermissions(base.getId().getId(), false);
            }
        } else {
            LOGGER.warn("Permissions for ID " + base.getId().getId() + " allready exist.");
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler repair: done in " + diff + " sec.");
    }

    /**
     * This method sets Default Rules to all permissions that are configured. if
     * <i>overwrite</i> = true, then the old permission entries that are in the
     * database are overwritten, else not.
     * 
     * @param obj
     * @param overwrite
     */
    private void setDefaultPermissions(String id, boolean overwrite) {
        List savedPermissions = MCRAccessManager.getPermissionsForID(id);
        List configuredPermissions = AI.getAccessPermissionsFromConfiguration();
        for (Iterator it = configuredPermissions.iterator(); it.hasNext();) {
            String permission = (String) it.next();
            if (storedrules.indexOf(permission) != -1) {
                if (savedPermissions != null && savedPermissions.contains(permission)) {
                    if (overwrite) {
                        MCRAccessManager.removeRule(id, permission);
                        if (permission.startsWith("read"))
                            MCRAccessManager.addRule(id, permission, readrule, "");
                        else
                            MCRAccessManager.addRule(id, permission, editrule, "");
                    }
                } else {
                    if (permission.startsWith("read"))
                        MCRAccessManager.addRule(id, permission, readrule, "");
                    else
                        MCRAccessManager.addRule(id, permission, editrule, "");
                }
            }
        }
    }

}
