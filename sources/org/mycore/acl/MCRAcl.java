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

package org.mycore.acl;

import java.util.Set;

import org.mycore.user.MCRPrincipal;
import org.mycore.user.MCRUser;

// /============================================================================|

/**
 * This interface defines the behaviour of an access control list (ACL) for an
 * object.
 * 
 * An ACL contains categories of users. Every category has an associated set of
 * permissions which allow method calls on the object to which the ACL is
 * attached.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */
public interface MCRAcl {
    // /============================================================================/

    /**
     * Sets permissions for the specified principal. If no corresponding entry
     * exists the principal is added to this ACL.
     * 
     * @param principal
     *            the category, user or group to be added to this ACL
     * @param permissionRing
     *            the permissions assigned to the principal
     * 
     * @throws MCRAccessException
     *             if modifying this ACL is not allowed
     * @throws MCRInvalidPermissionException
     *             if <code>permissionRing</code> contains invalid permissions
     *             for this context
     * @throws MCRInvalidPrincipalException
     *             if it is not allowed to add this kind of principal
     */
    public void putPermissions(MCRPrincipal principal, MCRPermissionRing permissionRing) throws MCRAccessException, MCRInvalidPrincipalException, MCRInvalidPermissionException;

    // ------------------------------------------------------------------------------

    /**
     * Gets the set of permissions for a principal. Implementations should
     * guarantee that changes made to the resulting permission ring do not
     * affect the ACL, i.e. normally a copy will be returned.
     * 
     * @param principal
     *            a category, user or group contained in this ACL.
     * 
     * @return the permission ring attached to <code>principal</code>.
     * 
     * @throws MCRInvalidPrincipalException
     *             if the principal is not contained in this ACL.
     */
    public MCRPermissionRing getPermissions(MCRPrincipal principal) throws MCRInvalidPrincipalException;

    // ------------------------------------------------------------------------------

    /**
     * Deletes a principal from this ACL.
     * 
     * @param principal
     *            a category, user or group to be deleted form this ACL.
     * 
     * @throws MCRAccessException
     *             if modifying this ACL is not allowed.
     * @throws MCRInvalidPrincipalException
     *             if the principal is not contained in this ACL.
     */
    public void deletePrincipal(MCRPrincipal principal) throws MCRAccessException, MCRInvalidPrincipalException;

    // ------------------------------------------------------------------------------

    /**
     * Checks if there is an entry for a principal in this ACL.
     * 
     * @param principal
     *            a category, user or group to be deleted form this ACL.
     * 
     * @return true if the principal is contained in this ACL.
     */
    public boolean containsPrincipal(MCRPrincipal principal);

    // ------------------------------------------------------------------------------

    /**
     * Copies permissions from one entry to another.
     * 
     * @param source
     *            a category, user or group.
     * @param target
     *            a category, user or group.
     * 
     * @throws MCRAccessException
     *             if modifying this ACL is not allowed.
     * @throws MCRInvalidPrincipalException
     *             if the source entry is not contained in this ACL.
     */
    public void copyPermissions(MCRPrincipal source, MCRPrincipal target) throws MCRAccessException, MCRInvalidPrincipalException;

    // ------------------------------------------------------------------------------

    /**
     * Gets a set of the users currently contained as entries in this ACL.
     * 
     * @return a set of the userss or the empty set if the ACL has no user
     *         entries.
     */
    public Set getUsers();

    // ------------------------------------------------------------------------------

    /**
     * Gets a set of the groups currently contained as entries in this ACL.
     * 
     * @return a set of the groups or the empty set if the ACL has no group
     *         entries.
     */
    public Set getGroups();

    // ------------------------------------------------------------------------------

    /**
     * Gets the permission context for this ACL. The context determines which
     * permissions are valid for this ACL. In general a permission context is
     * unique per class, so that one context is shared by all ACLs which are
     * attached to instances of that class.
     * 
     * @return the permission context of this ACL.
     */
    public MCRPermissionContext getPermissionContext();

    // ------------------------------------------------------------------------------

    /**
     * Gets the category <code>user</code> is assigned to by this ACL with
     * respect to <code>object</code>.
     * 
     * <P>
     * Obtaining the category normally is the first step of the algorithm which
     * determines if a permission is granted or not.
     * 
     * @param user
     *            an arbitrary user.
     * @param object
     *            the owner information of this object is used to determine if
     *            <code>user</code> is owner or in owner group.
     * @param members
     *            on return the set contains the members of the category.
     * 
     * @return the category the user is assigend to by the ACL.
     */
    public MCRAclCategory getCategory(MCRUser user, MCROwnedObject object, Set members);

    // ------------------------------------------------------------------------------

    /**
     * Gets the set of permissions for a user obtained by the algorithm of this
     * ACL. The ACL decides to which category the user matches and returns the
     * corresponding permissions. This method generally implements the logic of
     * the ACL and is therfore used in <code>checkAccess</code>. It can also
     * be used to implement a cache with permission information for users who
     * have already called methods on a guarded object.
     * 
     * @param user
     *            an arbitrary user.
     * @param object
     *            the owner information of this object is used to determine if
     *            user is owner or in owner group.
     * 
     * @return the permissions the user has with respect to this ACL. This can
     *         be null if no category matches for <code>user</code>.
     */
    public MCRPermissionRing getAccessPermissions(MCRUser user, MCROwnedObject object);

    // ------------------------------------------------------------------------------

    /**
     * Checks if the given user can perform the action specified by
     * <code>permission</code>. In general <code>user</code> will be the
     * current user who calls a guarded method and <code>permission</code> is
     * the permission which is needed to call the method. The third argument is
     * the instance where the method is called. This is normally the object to
     * which the ACL is attached, but it can be any other object inheriting the
     * ACL, e.g. when objects groups in a parent-child-hierarchy.
     * 
     * @param user
     *            the user who wants to perform an action.
     * @param object
     *            the owned object for which access is requested.
     * @param permission
     *            the permission required to perform the action.
     * 
     * @return true if the user is allowed to perform the action,
     *         <code>false</code> otherwise.
     */
    public boolean isAccessPermitted(MCRUser user, MCROwnedObject object, MCRPermission permission);

    // -============================================================================\
}
