/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.access.mcrimpl;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessInterface;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;

/**
 * MyCoRe-Standard Implementation of the MCRAccessInterface Maps object ids to rules
 *
 * @author Matthias Kramm
 * @author Heiko Helmbrecht
 */
public class MCRAccessControlSystem extends MCRAccessBaseImpl {

    public static final String systemRulePrefix = "SYSTEMRULE";

    public static final String poolPrivilegeID = "POOLPRIVILEGE";

    public static final String lexicographicalPattern = "0000000000";

    MCRAccessStore accessStore;

    MCRRuleStore ruleStore;

    MCRAccessRule dummyRule;

    boolean disabled = false;

    static Hashtable<String, String> ruleIDTable = new Hashtable<>();

    private static final Logger LOGGER = LogManager.getLogger(MCRAccessControlSystem.class);

    private MCRAccessControlSystem() {
        MCRConfiguration config = MCRConfiguration.instance();
        String pools = config.getString("MCR.Access.AccessPermissions", "read,write,delete");

        if (pools.trim().length() == 0) {
            disabled = true;
        }

        accessStore = MCRAccessStore.getInstance();
        ruleStore = MCRRuleStore.getInstance();

        nextFreeRuleID = new HashMap<>();

        dummyRule = new MCRAccessRule(null, null, null, null, "dummy rule, always true");
    }

    private static MCRAccessControlSystem singleton;

    private static HashMap<String, Integer> nextFreeRuleID;

    // extended methods
    public static synchronized MCRAccessInterface instance() {
        if (singleton == null) {
            singleton = new MCRAccessControlSystem();
        }
        return singleton;
    }

    @Override
    public void createRule(String ruleString, String creator, String description) {
        String ruleID = getNextFreeRuleID(systemRulePrefix);
        MCRAccessRule accessRule = new MCRAccessRule(ruleID, creator, new Date(), ruleString, description);
        ruleStore.createRule(accessRule);
    }

    @Override
    public void createRule(Element rule, String creator, String description) {
        createRule(getNormalizedRuleString(rule), creator, description);
    }

    @Override
    public void addRule(String id, String pool, Element rule, String description) throws MCRException {
        MCRRuleMapping ruleMapping = getAutoGeneratedRuleMapping(rule, "System", pool, id, description);
        String oldRuleID = accessStore.getRuleID(id, pool);
        if (oldRuleID == null || oldRuleID.equals("")) {
            accessStore.createAccessDefinition(ruleMapping);
        } else {
            accessStore.updateAccessDefinition(ruleMapping);
        }
    }

    @Override
    public void addRule(String permission, org.jdom2.Element rule, String description) {
        addRule(poolPrivilegeID, permission, rule, description);
    }

    @Override
    public void removeRule(String id, String pool) throws MCRException {
        MCRRuleMapping ruleMapping = accessStore.getAccessDefinition(pool, id);
        accessStore.deleteAccessDefinition(ruleMapping);
    }

    @Override
    public void removeRule(String permission) throws MCRException {
        removeRule(poolPrivilegeID, permission);
    }

    @Override
    public void removeAllRules(String id) throws MCRException {
        for (String pool : accessStore.getPoolsForObject(id)) {
            removeRule(id, pool);
        }
    }

    @Override
    public void updateRule(String id, String pool, org.jdom2.Element rule, String description) throws MCRException {
        MCRRuleMapping ruleMapping = getAutoGeneratedRuleMapping(rule, "System", pool, id, description);
        String oldRuleID = accessStore.getRuleID(id, pool);
        if (oldRuleID == null || oldRuleID.equals("")) {
            LOGGER.debug(
                "updateRule called for id <{}> and pool <{}>, but no rule is existing, so new rule was created", id,
                pool);
            accessStore.createAccessDefinition(ruleMapping);
        } else {
            accessStore.updateAccessDefinition(ruleMapping);
        }
    }

    @Override
    public void updateRule(String permission, Element rule, String description) throws MCRException {
        updateRule(poolPrivilegeID, permission, rule, description);
    }

    @Override
    @Deprecated
    public boolean checkPermission(String id, String permission, String userID) {
        return checkAccess(id, permission, userID, null);
    }

    @Override
    public boolean checkPermission(String id, String permission, MCRUserInformation userInfo) {
        return checkAccess(id, permission, userInfo, null);
    }

    @Override
    public boolean checkPermission(String permission) {
        LOGGER.debug("Execute MCRAccessControlSystem checkPermission for permission {}", permission);
        boolean ret = checkPermission(poolPrivilegeID, permission);
        LOGGER.debug("Execute MCRAccessControlSystem checkPermission result: {}", String.valueOf(ret));
        return ret;
    }

    @Override
    @Deprecated
    public boolean checkPermissionForUser(String permission, String userID) {
        return checkAccess(poolPrivilegeID, permission, userID, null);
    }

    @Override
    public boolean checkPermissionForUser(String permission, MCRUserInformation userInfo) {
        return checkAccess(poolPrivilegeID, permission, userInfo, null);
    }

    @Override
    public boolean checkPermission(Element rule) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String ruleStr = getNormalizedRuleString(rule);
        MCRAccessRule accessRule = new MCRAccessRule(null, "System", new Date(), ruleStr, "");
        try {
            return accessRule.checkAccess(session.getUserInformation(), new Date(), new MCRIPAddress(
                session.getCurrentIP()));
        } catch (MCRException | UnknownHostException e) {
            // only return true if access is allowed, we dont know this
            LOGGER.debug("Error while checking rule.", e);
            return false;
        }
    }

    @Override
    public Element getRule(String objID, String permission) {
        MCRAccessRule accessRule = getAccessRule(objID, permission);
        MCRRuleParser parser = new MCRRuleParser();
        Element rule = parser.parse(accessRule.rule).toXML();
        Element condition = new Element("condition");
        condition.setAttribute("format", "xml");
        if (rule != null) {
            condition.addContent(rule);
        }
        return condition;
    }

    @Override
    public Element getRule(String permission) {
        return getRule(poolPrivilegeID, permission);
    }

    @Override
    public String getRuleDescription(String permission) {
        return getRuleDescription(poolPrivilegeID, permission);
    }

    @Override
    public String getRuleDescription(String objID, String permission) {
        MCRAccessRule accessRule = getAccessRule(objID, permission);
        if (accessRule != null && accessRule.getDescription() != null) {
            return accessRule.getDescription();
        }
        return "";
    }

    @Override
    public Collection<String> getPermissionsForID(String objid) {
        return accessStore.getPoolsForObject(objid);
    }

    @Override
    public Collection<String> getPermissions() {
        return accessStore.getPoolsForObject(poolPrivilegeID);
    }

    @Override
    public boolean hasRule(String id, String permission) {
        return accessStore.existsRule(id, permission);
    }

    @Override
    public boolean hasRule(String id) {
        return hasRule(id, null);
    }

    @Override
    public Collection<String> getAllControlledIDs() {
        return accessStore.getDistinctStringIDs();
    }

    // not extended methods

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public MCRAccessRule getAccessRule(String objID, String pool) {
        if (disabled) {
            return dummyRule;
        }
        LOGGER.debug("accessStore.getRuleID()");
        String ruleID = accessStore.getRuleID(objID, pool);
        if (ruleID == null) {
            LOGGER.debug("accessStore.getRuleID() done with null");
            return null;
        } else {
            LOGGER.debug("accessStore.getRuleID() done with {}", ruleID);
        }
        return ruleStore.getRule(ruleID);
    }

    /**
     * Validator methods to validate access definition for given object and pool
     *
     * @param permission
     *            poolname as string
     * @param objID
     *            MCRObjectID as string
     * @param userID
     *            MCRUser
     * @param ip
     *            ip-Address
     * @return true if access is granted according to defined access rules
     */
    @Deprecated
    public boolean checkAccess(String objID, String permission, String userID, MCRIPAddress ip) {
        Date date = new Date();
        LOGGER.debug("getAccess()");
        MCRAccessRule rule = getAccessRule(objID, permission);
        LOGGER.debug("getAccess() is done");
        if (rule == null) {
            return userID.equals(MCRSystemUserInformation.getSuperUserInstance().getUserID());
        }
        return rule.checkAccess(userID, date, ip);
    }

    /**
     * Validator methods to validate access definition for given object and pool
     *
     * @param permission
     *            poolname as string
     * @param objID
     *            MCRObjectID as string
     * @param userInfo
     *            MCRUser
     * @param ip
     *            ip-Address
     * @return true if access is granted according to defined access rules
     */
    public boolean checkAccess(String objID, String permission, MCRUserInformation userInfo, MCRIPAddress ip) {
        Date date = new Date();
        LOGGER.debug("getAccess()");
        MCRAccessRule rule = getAccessRule(objID, permission);
        LOGGER.debug("getAccess() is done");
        if (rule == null) {
            return userInfo.getUserID().equals(MCRSystemUserInformation.getSuperUserInstance().getUserID());
        }
        return rule.checkAccess(userInfo, date, ip);
    }

    /**
     * method that delivers the next free ruleID for a given Prefix and sets the counter to counter + 1
     *
     * @param prefix
     *            String
     * @return String
     */
    public synchronized String getNextFreeRuleID(String prefix) {
        int nextFreeID;
        String sNextFreeID;
        if (nextFreeRuleID.containsKey(prefix)) {
            nextFreeID = nextFreeRuleID.get(prefix);
        } else {
            nextFreeID = ruleStore.getNextFreeRuleID(prefix);
        }
        sNextFreeID = lexicographicalPattern + nextFreeID;
        sNextFreeID = sNextFreeID.substring(sNextFreeID.length() - lexicographicalPattern.length());
        nextFreeRuleID.put(prefix, nextFreeID + 1);
        return prefix + sNextFreeID;
    }

    /**
     * delivers the rule as string, after normalizing it via sorting with MCRAccessConditionsComparator
     *
     * @param rule
     *            Jdom-Element
     * @return String
     */
    @Override
    public String getNormalizedRuleString(Element rule) {
        if (rule.getChildren() == null || rule.getChildren().size() == 0) {
            return "false";
        }
        Element normalizedRule = normalize(rule.getChildren().get(0));
        MCRRuleParser parser = new MCRRuleParser();
        return parser.parse(normalizedRule).toString();
    }

    /**
     * returns a auto-generated MCRRuleMapping, needed to create Access Definitions
     *
     * @param rule
     *            JDOM-Representation of a MCRAccess Rule
     * @param creator
     *            String
     * @param pool
     *            String
     * @param id
     *            String
     * @return MCRRuleMapping
     */
    public MCRRuleMapping getAutoGeneratedRuleMapping(Element rule, String creator, String pool, String id,
        String description) {
        String ruleString = getNormalizedRuleString(rule);
        String ruleID = ruleIDTable.get(ruleString);
        if (ruleID == null || ruleID.length() == 0) {
            Collection<String> existingIDs = ruleStore.retrieveRuleIDs(ruleString, description);
            if (existingIDs != null && existingIDs.size() > 0) {
                // rule yet exists
                ruleID = existingIDs.iterator().next();
            } else {
                ruleID = getNextFreeRuleID(systemRulePrefix);
                MCRAccessRule accessRule = new MCRAccessRule(ruleID, creator, new Date(), ruleString, description);
                ruleStore.createRule(accessRule);
            }
            ruleIDTable.put(ruleString, ruleID);
        }
        MCRRuleMapping ruleMapping = new MCRRuleMapping();
        ruleMapping.setCreator(creator);
        ruleMapping.setCreationdate(new Date());
        ruleMapping.setPool(pool);
        ruleMapping.setRuleId(ruleID);
        ruleMapping.setObjId(id);
        return ruleMapping;
    }

    /**
     * method, that normalizes the jdom-representation of a mycore access condition
     *
     * @param rule
     *            condition-JDOM of an access-rule
     * @return the normalized JDOM-Rule
     */
    public Element normalize(Element rule) {
        Element newRule = new Element(rule.getName());
        rule.getAttributes()
            .stream()
            .map(Attribute::clone)
            .forEach(newRule::setAttribute);
        rule.getChildren()
            .stream()
            .map(Element::clone)
            .map(this::normalize)
            .sorted(MCRAccessControlSystem::compareAccessConditions)
            .forEachOrdered(newRule::addContent);
        return newRule;
    }

    /**
     * A Comparator for the Condition Elements for normalizing the access conditions
     */
    private static int compareAccessConditions(Element el0, Element el1) {
        String nameEl0 = el0.getName();
        String nameEl1 = el1.getName();
        int nameCompare = nameEl0.compareTo(nameEl1);
        // order "boolean" before "condition"
        if (nameCompare != 0) {
            return nameCompare;
        }
        if (nameEl0.equals("boolean")) {
            String opEl0 = el0.getAttributeValue("operator");
            String opEl1 = el0.getAttributeValue("operator");
            return opEl0.compareToIgnoreCase(opEl1);
        } else if (nameEl0.equals("condition")) {
            String fieldEl0 = el0.getAttributeValue("field");
            String fieldEl1 = el1.getAttributeValue("field");
            int fieldCompare = fieldEl0.compareToIgnoreCase(fieldEl1);
            if (fieldCompare != 0) {
                return fieldCompare;
            }
            String valueEl0 = el0.getAttributeValue("value");
            String valueEl1 = el1.getAttributeValue("value");
            return valueEl0.compareTo(valueEl1);
        }
        return 0;
    }

}
