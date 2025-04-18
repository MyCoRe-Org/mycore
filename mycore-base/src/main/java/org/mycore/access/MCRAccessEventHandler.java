/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

// package
package org.mycore.access;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class holds all EventHandler methods to manage the access part of the
 * simple workflow.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRAccessEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRRuleAccessInterface AI = MCRAccessManager.getAccessImpl();

    private static String storedrules = MCRConfiguration2.getString("MCR.Access.StorePermissions")
        .orElse("read,write,delete");

    private static Element readrule;

    private static Element editrule;
    static {
        try {
            // get the standard read rule from config or it's the true rule
            readrule = new MCRStringContent(MCRConfiguration2.getString("MCR.Access.Rule.STANDARD-READ-RULE")
                .orElse("<condition format=\"xml\"><boolean operator=\"true\" /></condition>"))
                .asXML().getRootElement().detach();
            // get the standard edit rule from config or it's the true rule
            editrule = new MCRStringContent(MCRConfiguration2.getString("MCR.Access.Rule.STANDARD-EDIT-RULE")
                .orElse("<condition format=\"xml\"><boolean operator=\"true\" /></condition>"))
                .asXML().getRootElement().detach();
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
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
    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleBaseCreated(obj, MCRConfiguration2.getBoolean("MCR.Access.AddObjectDefaultRule").orElse(true));
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
    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleBaseUpdated(obj, MCRConfiguration2.getBoolean("MCR.Access.AddObjectDefaultRule").orElse(true));
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
    @Override
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
    @Override
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
    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleBaseCreated(der, MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true));
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
    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleBaseUpdated(der, MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true));
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
    @Override
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
    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        // Do nothing
    }

    private void handleBaseCreated(MCRBase base, boolean addDefaultRules) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        Collection<String> li = AI.getPermissionsForID(base.getId().toString());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = base.getService().getRulesSize();
        if (rulesize == 0 && aclsize == 0 && addDefaultRules) {
            setDefaultPermissions(base.getId().toString(), true);
            LOGGER.warn("The ACL conditions for this object are empty!");
        }
        while (0 < rulesize) {
            Element conditions = base.getService().getRule(0).getCondition();
            String permission = base.getService().getRule(0).getPermission();
            if (storedrules.contains(permission)) {
                MCRAccessManager.addRule(base.getId(), permission, conditions, "");
            }
            base.getService().removeRule(0);
            rulesize--;
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in {} sec.", diff);
    }

    private void handleBaseUpdated(MCRBase base, boolean addDefaultRules) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        Collection<String> li = AI.getPermissionsForID(base.getId().toString());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        int rulesize = base.getService().getRulesSize();
        if (rulesize == 0 && aclsize == 0 && addDefaultRules) {
            setDefaultPermissions(base.getId().toString(), false);
            LOGGER.warn("The ACL conditions for this object was empty!");
        }
        if (aclsize == 0) {
            while (0 < rulesize) {
                Element conditions = base.getService().getRule(0).getCondition();
                String permission = base.getService().getRule(0).getPermission();
                if (storedrules.contains(permission)) {
                    MCRAccessManager.updateRule(base.getId(), permission, conditions, "");
                }
                base.getService().removeRule(0);
                rulesize--;
            }
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler update: done in {} sec.", diff);
    }

    private void handleBaseDeleted(MCRBase base) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRAccessManager.removeAllRules(base.getId());

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler delete: done in {} sec.", diff);
    }

    /**
     * This method sets Default Rules to all permissions that are configured. if
     * <i>overwrite</i> = true, then the old permission entries that are in the
     * database are overwritten, else not.
     */
    private void setDefaultPermissions(String id, boolean overwrite) {
        Collection<String> savedPermissions = MCRAccessManager.getPermissionsForID(id);
        Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
        for (String permission : configuredPermissions) {
            if (storedrules.contains(permission)) {
                if (savedPermissions != null && savedPermissions.contains(permission)) {
                    if (overwrite) {
                        MCRAccessManager.removeRule(id, permission);
                        if (permission.startsWith(MCRAccessManager.PERMISSION_READ)) {
                            MCRAccessManager.addRule(id, permission, readrule, "");
                        } else {
                            MCRAccessManager.addRule(id, permission, editrule, "");
                        }
                    }
                } else {
                    if (permission.startsWith(MCRAccessManager.PERMISSION_READ)) {
                        MCRAccessManager.addRule(id, permission, readrule, "");
                    } else {
                        MCRAccessManager.addRule(id, permission, editrule, "");
                    }
                }
            }
        }
    }

}
