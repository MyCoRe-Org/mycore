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

package org.mycore.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;

/**
 * This class is a base implementation of the <code>MCRAccessInterface</code>.
 * 
 * It will simply allow everything and will do nothing on persistent operations.
 * Feel free to extend this class if your implementation can only support parts
 * of the Interface definition.
 * 
 * @author Jens Kupferschmidt
 * 
 */
public class MCRAccessBaseImpl implements MCRAccessInterface {

    private static MCRAccessInterface SINGLETON;

    protected static final String AccessPermissions = MCRConfiguration.instance().getString(
        "MCR.Access.AccessPermissions", "read,write,delete");

    /** the logger */
    private static final Logger LOGGER = LogManager.getLogger(MCRAccessBaseImpl.class);

    public MCRAccessBaseImpl() {
    }

    /**
     * The method return a singleton instance of MCRAccessInterface.
     * 
     * @return a singleton instance of MCRAccessInterface
     */
    public static synchronized MCRAccessInterface instance() {
        if (SINGLETON == null) {
            SINGLETON = new MCRAccessBaseImpl();
        }

        return SINGLETON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#addRule(java.lang.String,
     *      java.lang.String, org.jdom2.Element)
     */
    public void addRule(String id, String permission, org.jdom2.Element rule, String description) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl addRule for ID {} for permission {}", id, permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#addRule(java.lang.String,
     *      org.jdom2.Element)
     */
    public void addRule(String permission, Element rule, String description) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl addRule for permission {}", permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeRule(java.lang.String,
     *      java.lang.String)
     */
    public void removeRule(String id, String permission) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeRule for ID {} for permission {}", id, permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeRule(java.lang.String)
     */
    public void removeRule(String permission) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeRule for permission {}", permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeAllRules(java.lang.String)
     */
    public void removeAllRules(String id) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeAllRules for ID {}", id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#updateRule(java.lang.String,
     *      java.lang.String, org.jdom2.Element)
     */
    public void updateRule(String id, String permission, org.jdom2.Element rule, String description)
        throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl updateRule for ID {} for permission {}", id, permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#updateRule(java.lang.String,
     *      org.jdom2.Element)
     */
    public void updateRule(String permission, Element rule, String description) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl updateRule for permission {}", permission);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("Execute MCRAccessBaseImpl checkPermission for ID {} for permission {}", id, permission);
        long start = System.currentTimeMillis();
        try {
            MCRAccessRule rule = getAccessRule(id, permission);
            if (rule == null) {
                LOGGER.debug("No rule defined. Checking if current user is super user.");
                MCRSystemUserInformation superUserInstance = MCRSystemUserInformation.getSuperUserInstance();
                String superUserID = superUserInstance.getUserID();
                return superUserID.equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
            }
            boolean validate = rule.validate();
            LOGGER.debug(validate ? "Current user has permission." : "Current user does not have permission.");
            return validate;
        } finally {
            LOGGER.debug("Check {} on {} took:{}", permission, id, System.currentTimeMillis() - start);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String,
     *      java.lang.String, MCRUser)
     */
    public boolean checkPermission(String id, String permission, String userID) {
        LOGGER.debug("Execute MCRAccessBaseImpl checkPermission for ID {} for permission {} for user{}", id, permission,
            userID);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String)
     */
    public boolean checkPermission(String permission) {
        LOGGER.debug("Execute MCRAccessBaseImpl checkPermission for permission {}", permission);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String, MCRUser)
     */
    public boolean checkPermissionForUser(String permission, String userID) {
        LOGGER.debug("Execute MCRAccessBaseImpl checkPermission for permission {} for user {}", permission, userID);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkPermission(java.lang.String, java.lang.String, org.jdom2.Document)
     */
    public boolean checkPermission(Element rule) {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAccessRule(java.lang.String,
     *      java.lang.String)
     */
    public Element getRule(String objID, String permission) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAccessRule(java.lang.String)
     */
    public Element getRule(String permission) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getRuleDescription(java.lang.String)
     */
    public String getRuleDescription(String permission) {
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getRuleDescription(java.lang.String, java.lang.String)
     */
    public String getRuleDescription(String id, String permission) {
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getPermissionsForID(java.lang.String)
     */
    public Collection<String> getPermissionsForID(String objid) {
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getPermissions()
     */
    public Collection<String> getPermissions() {
        return Collections.emptySet();
    }

    /**
     * checks wether a rule with the id and permission is defined. It's the same
     * as calling
     * 
     * <pre>
     *    (getRule(id, permission)!=null);
     * </pre>
     * 
     * @see #getRule(String, String)
     */
    public boolean hasRule(String id, String permission) {
        return getRule(id, permission) != null;
    }

    /**
     * checks wether a rule with the id is defined. It's the same as calling
     * 
     * <pre>
     *    (getPermissionsForID(id).size()&gt;0);
     * </pre>
     * 
     * @see #getRule(String, String)
     */
    public boolean hasRule(String id) {
        return getPermissionsForID(id).size() > 0;
    }

    /**
     * just returns the String of Access Permissions configured in
     * property "MCR.AccessPermissions"
     * 
     * @return the permissions as List
     */
    public Collection<String> getAccessPermissionsFromConfiguration() {
        String[] permissions = AccessPermissions.split(",");
        return Arrays.asList(permissions);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAllControlledIDs()
     */
    public Collection<String> getAllControlledIDs() {
        return null;
    }

    public void createRule(String rule, String creator, String description) {
        LOGGER.debug("Execute MCRAccessBaseImpl createRule with rule {} \n and description {}", rule, description);

    }

    public void createRule(Element rule, String creator, String description) {
        // TODO Auto-generated method stub

    }

    public String getNormalizedRuleString(Element rule) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MCRAccessRule getAccessRule(String id, String permission) {
        return () -> true;
    }
}
