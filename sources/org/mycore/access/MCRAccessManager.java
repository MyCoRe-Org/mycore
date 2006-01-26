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
package org.mycore.access;

import org.apache.log4j.Logger;
import org.jdom.Element;

import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObjectID;


/**
 * 
 * @author Thomas Scheffler
 * 
 * @version $Revision$ $Date$
 */
public class MCRAccessManager {

    private static final MCRAccessInterface ACCESS_IMPL = (MCRAccessInterface) MCRConfiguration.instance().getInstanceOf("MCR.Access_class_name",
            MCRAccessBaseImpl.class.getName());

    private static final Logger LOGGER = Logger.getLogger(MCRAccessManager.class);

    public static MCRAccessInterface getAccessImpl() {
        return ACCESS_IMPL;
    }

    /**
     * adds an access rule for an MCRObjectID to an access system.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     * @see MCRAccessInterface#addRule(String, String, org.jdom.Element)
     */
    public static void addRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException {
        getAccessImpl().addRule(id.getId(), pool, rule);
    }

    /**
     * removes the <code>permission</code> rule for the MCRObjectID.
     * 
     * @param id
     *            the MCRObjectID of an object
     * @param permission
     *            the access pool for the rule
     * @throws MCRException
     *             if an errow was occured
     * @see MCRAccessInterface#removeRule(String, String)
     */
    public static void removeRule(MCRObjectID id, String permission) throws MCRException {
        getAccessImpl().removeRule(id.getId(), permission);
    }

    /**
     * removes all rules for the MCRObjectID.
     * 
     * @param id
     *            the MCRObjectID of an object
     * @throws MCRException
     *             if an errow was occured
     * @see MCRAccessInterface#removeRule(String)
     */
    public static void removeAllRules(MCRObjectID id) throws MCRException {
        getAccessImpl().removeAllRules(id.getId());
    }

    /**
     * updates an access rule for an MCRObjectID.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     * @see MCRAccessInterface#updateRule(String, String, Element)
     */
    public static void updateRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException {
        getAccessImpl().updateRule(id.getId(), pool, rule);
    }

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @return true if the access is allowed otherwise it return
     * @see MCRAccessInterface#checkPermission(String, String)
     */
    public static boolean checkPermission(MCRObjectID id, String pool) {
        return getAccessImpl().checkPermission(id.getId(), pool);
    }

    /**
     * checks the access rule against the MCRSession and return true if the
     * access is allowed otherwise it return false.
     * 
     * This method is independent of the underlying
     * <code>MCRAccessInterface</code> implementation.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param condition
     *            the rule tree as a JDOM Element
     * @return true if the access is allowed, false otherwise
     */
    public static boolean checkAccessCondition(String id, String pool, Element rule) {
    	MCRAccessControlSystem mcrManager = (MCRAccessControlSystem) MCRAccessControlSystem.instance();
    	return mcrManager.checkAccessCondition(id, pool, rule);
    }

    /**
     * checks the access rule against the current MCRSession and return true if
     * the access is allowed otherwise it return false. It's the same as calling
     * 
     * <pre>
     * checkAccessCondition(id.getId(), pool, rule);
     * </pre>
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param condition
     *            the rule tree as a JDOM Element
     * @return true if the access is allowed otherwise it return
     * @see #checkAccessCondition(String, String, Element)
     */
    public static boolean checkAccessCondition(MCRObjectID id, String pool, Element rule) {
        return checkAccessCondition(id.getId(), pool, rule);
    }

}