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

import java.util.List;

import org.jdom.Element;

import org.mycore.common.MCRException;

/**
 * This serves as an interface to an underlying access controll system.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 1.3
 */
public interface MCRAccessInterface {

    /**
     * adds an access rule for an ID to an access system. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an error occured
     */
    public void addRule(String id, String permission, org.jdom.Element rule) throws MCRException;
    
    /**
     * adds an access rule for an "a priori-permission" like "create-document"
     * 
     * @param permission
     *            the access permission for the rule (e.g. "create-document")
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an error occured
     */
    public void addRule(String permission, org.jdom.Element rule) throws MCRException;    

    /**
     * removes a rule. The parameter <code>id</code> serves as an identifier
     * for the concrete underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error occured
     */
    public void removeRule(String id, String permission) throws MCRException;
    
    /**
     * removes a rule for an "a priori permission" like "create-document"
     * 
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error occured
     */
    public void removeRule(String permission) throws MCRException;    

    /**
     * removes all rules of the <code>id</code>. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @throws MCRException
     *             if an errow was occured
     */
    public void removeAllRules(String id) throws MCRException;

    /**
     * updates an access rule for an ID to an access system. The parameter
     * <code>id</code> serves as an identifier for the concrete underlying
     * rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(String id, String permission, Element rule) throws MCRException;
    
    /**
     * updates an access rule for an "a priori permission" 
     * of an access system like "create-document".
     * 
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @throws MCRException
     *             if an errow was occured
     */
    public void updateRule(String permission, Element rule) throws MCRException;    

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the permission/action to be granted, e.g. "read"
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermission(String id, String permission);
    
    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * 
     * This method is used for checking "a priori permissions" like "create-document"
     *     where a String ID does not exist yet
     * 
     * @param permission
     *            the permission/action to be granted, e.g. "create-document"
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */
    public boolean checkPermission(String permission);    

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * All information regarding the current user is capsulated by a
     * <code>MCRSession</code> instance which can be retrieved by
     * 
     * <pre>
     * MCRSession currentSession = MCRSessionMgr.getCurrentSession();
     * </pre>
     * @param rule
     *            the jdom-representation of a mycore access rule           
     * @return true if the permission is granted, else false
     * @see org.mycore.common.MCRSessionMgr#getCurrentSession()
     * @see org.mycore.common.MCRSession
     */    
    public boolean checkPermission(org.jdom.Element rule) ;
    
    /**
     * exports a access rule as JDOM element.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return the rule as jdom element, or <code>null</code> if no rule is
     *         defined
     */
    public Element getRule(String id, String permission);
    
    /**
     * exports a access rule for a "a priori permission"
     * as JDOM element.
     * 
     * @param permission
     *            the access permission for the rule
     * @return the rule as jdom element, or <code>null</code> if no rule is
     *         defined
     */
    public Element getRule(String permission);    

    /**
     * lists all permissions defined for the <code>id</code>.
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     * @return a <code>List</code> of all for <code>id</code> defined
     *         permission
     */
    public List getPermissionsForID(String id);
    
    /**
     * lists all String IDs, a permission is assigned to.
     * 
     * The parameter <code>id</code> serves as an identifier for the concrete
     * underlying rule, e.g. a MCRObjectID.
     * 
     * @param id
     * @return a sorted and distinct <code>List</code> of all  <code>String</code> IDs
     */
    public List getAllControlledIDs();    

    /**
     * checks wether a rule with the <code>id</code> and
     * <code>permission</code> is defined.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return false, if getRule(id, permission) would return null, else true
     */
    public boolean hasRule(String id, String permission);

    /**
     * checks wether a rule with the <code>id</code> is defined.
     * 
     * @param id
     *            the ID-String of the object
     * @param permission
     *            the access permission for the rule
     * @return false, if getPermissionsForID(id) would return an empty list,
     *         else true
     */
    public boolean hasRule(String id);

}