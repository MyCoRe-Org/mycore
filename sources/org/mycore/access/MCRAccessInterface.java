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

import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This interface should be the access point of different Access System
 * implemenations against the Access EventHandler and the other access points
 * like servlets. The implementation have all methods to use an access system.
 * 
 * @author Jens Kupferschmidt
 * 
 */
public interface MCRAccessInterface {
    /**
     * The method return a singleton instance of MCRAccessManagerDummy.
     * 
     * @return a singleton instance of MCRAccessManagerDummy
     */
    public MCRAccessManagerDummy instance();

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
    public void addRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException;

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
    public void removeRule(MCRObjectID id, String pool) throws MCRException;

    /**
     * The method remove all rules of a MCRObjectID.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeAllRules(MCRObjectID id) throws MCRException;

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
    public void updateRule(MCRObjectID id, String pool, org.jdom.Element rule) throws MCRException;

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
    public boolean checkAccess(MCRObjectID id, String pool);
}
