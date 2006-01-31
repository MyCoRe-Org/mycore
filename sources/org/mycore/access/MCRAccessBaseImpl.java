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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

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

    final protected static String AccessPools = MCRConfiguration.instance().getString("MCR.AccessPools", "");

    /** the logger */
    protected static Logger LOGGER = Logger.getLogger(MCRAccessBaseImpl.class.getName());

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
     *      java.lang.String, org.jdom.Element)
     */
    public void addRule(String id, String pool, org.jdom.Element rule) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl addRule");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#addRule(java.lang.String,
     *      org.jdom.Element)
     */    
	public void addRule(String permission, Element rule) throws MCRException {
		LOGGER.debug("Execute MCRAccessBaseImpl addRule");
	}    

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeRule(java.lang.String,
     *      java.lang.String)
     */
    public void removeRule(String id, String pool) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeRule");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeRule(java.lang.String)
     */
    public void removeRule(String pool) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeRule");
    }    

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#removeAllRules(java.lang.String)
     */
    public void removeAllRules(String id) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl removeAllRules");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#updateRule(java.lang.String,
     *      java.lang.String, org.jdom.Element)
     */
    public void updateRule(String id, String pool, org.jdom.Element rule) throws MCRException {
        LOGGER.debug("Execute MCRAccessBaseImpl updateRule");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#updateRule(java.lang.String,
     *      org.jdom.Element)
     */    
	public void updateRule(String permission, Element rule) throws MCRException {
		LOGGER.debug("Execute MCRAccessBaseImpl updateRule");
	}    

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String pool) {
        return true;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#checkAccess(java.lang.String)
     */    
	public boolean checkPermission(String permission) {
		return true;
	}    

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAccessRule(java.lang.String,
     *      java.lang.String)
     */
    public Element getRule(String objID, String pool) {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAccessRule(java.lang.String)
     */
    public Element getRule(String pool) {
        return null;
    }    

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getPoolsForObject(java.lang.String)
     */
    public List getPermissionsForID(String objid) {
        ArrayList ret = new ArrayList();
        return ret;
    }

    /**
     * checks wether a rule with the id and permission is defined. It's the same
     * as calling
     * 
     * <pre>
     *  (getRule(id, permission)!=null);
     * </pre>
     * 
     * @see #getRule(String, String)
     */
    public boolean hasRule(String id, String permission) {
        return (getRule(id, permission) != null);
    }

    /**
     * checks wether a rule with the id is defined. It's the same as calling
     * 
     * <pre>
     *  (getPermissionsForID(id).size()&gt;0);
     * </pre>
     * 
     * @see #getRule(String, String)
     */
    public boolean hasRule(String id) {
        return (getPermissionsForID(id).size() > 0);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.access.MCRAccessInterface#getAllControlledIDs()
     */
	public List getAllControlledIDs() {
		return null;
	}

}
