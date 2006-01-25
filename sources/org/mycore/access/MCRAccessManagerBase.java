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
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This interface should be the access point of different Access System
 * implemenations against the Access EventHandler and the other access points
 * like servlets. The implementation have all methods to use an access system.
 * 
 * @author Jens Kupferschmidt
 * 
 */
public class MCRAccessManagerBase {

    private static MCRAccessManagerBase singleton;

    /** the logger */
    static Logger logger = Logger.getLogger(MCRAccessManagerBase.class.getName());

    public MCRAccessManagerBase() {
    }

    /**
     * The method return a singleton instance of MCRAccessManagerBase.
     * 
     * @return a singleton instance of MCRAccessManagerBase
     */
    public static synchronized MCRAccessManagerBase instance() {
        if (singleton == null) {
            singleton = new MCRAccessManagerBase();
        }

        return singleton;
    }

    /**
     * The method add an access rule for an ID to an access system.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void addRule(String id, String pool, org.jdom.Element rule) throws MCRException {
        logger.debug("Execute MCRAccessManagerBase addRule");
    }

    /**
     * The method add an access rule for an MCRObjectID to an access system.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void addRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException {
        addRule(id.getId(), pool, rule);
    }

    /**
     * The method remove rule of a ID for a given pool.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeRule(String id, String pool) throws MCRException {
        logger.debug("Execute MCRAccessManagerBase removeRule");
    }

    /**
     * The method remove rule of a MCRObjectID for a given pool.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeRule(MCRObjectID id, String pool) throws MCRException {
        removeRule(id.getId(), pool);
    }

    /**
     * The method remove all rules of a ID.
     * 
     * @param id
     *            the ID-String of the object
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeAllRules(String id) throws MCRException {
        logger.debug("Execute MCRAccessManagerBase removeAllRules");
    }

    /**
     * The method remove all rules of a MCRObjectID.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeAllRules(MCRObjectID id) throws MCRException {
        removeAllRules(id.getId());
    }

    /**
     * The method update an access rule for an ID to an access system.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(String id, String pool, org.jdom.Element rule) throws MCRException {
        logger.debug("Execute MCRAccessManagerBase updateRule");
    }

    /**
     * The method update an access rule for an MCRObjectID to an access system.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException {
        updateRule(id.getId(), pool, rule);
    }

    /**
     * The method check the access of a pool against the MCRSession and return
     * true if the access is allowed otherwise it return false.
     * 
     * @param pool
     *            the access pool for the rule
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccess(String pool) {
        logger.debug("Execute MCRAccessManagerBase checkAccess");
        return true;
    }

    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccess(String id, String pool) {
        return checkAccess(id, pool, MCRSessionMgr.getCurrentSession());
    }
    
    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param session
     * 			  the current MCRSession            
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccess(String id, String pool, MCRSession session) {
        logger.debug("Execute MCRAccessManagerBase checkAccess");
        return true;
    }    

    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccess(MCRObjectID id, String pool) {
        return checkAccess(id.getId(), pool);
    }

    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param condition
     *            the rule tree as a JDOM Element
     * @param session
     * 			  the current MCRSession            
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccessCondition(String id, String pool, org.jdom.Element rule, MCRSession session) {
        logger.debug("Execute MCRAccessManagerBase checkAccess");
        return true;
    }
    
    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule
     * @param condition
     *            the rule tree as a JDOM Element
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccessCondition(String id, String pool, org.jdom.Element rule) {
        return checkAccessCondition(id, pool, rule, MCRSessionMgr.getCurrentSession());
    }

    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param pool
     *            the access pool for the rule
     * @param condition
     *            the rule tree as a JDOM Element
     * @return true if the access is allowed otherwise it return
     */
    public boolean checkAccessCondition(MCRObjectID id, String pool, org.jdom.Element rule) {
        return checkAccessCondition(id.getId(), pool, rule);
    }

    /**
     * The method check the access rule against the MCRSession and return true
     * if the access is allowed otherwise it return false.
     * 
     * @param id
     *            the ID-String of the object
     * @param pool
     *            the access pool for the rule

     * @return the rule tree as jdom element
     */    
    public Element getAccessRule(String objID, String pool) {
    	return null;
    }
    
}
