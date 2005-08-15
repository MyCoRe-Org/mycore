/**
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 **/

package org.mycore.acl;

///============================================================================§

import java.rmi.AccessException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrincipal;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

///============================================================================|

/**
 * This is the default implementation of interface <code>MCRAcl</code>.
 * 
 * <P>
 * It contains three maps, one for generic entries, one for individual user
 * entries and one for the group entries. Additionally it holds references to
 * the object which owns this ACL and a corresponding context.
 * 
 * <P>
 * As this class also implements the <code>MCRAclGuarded</code> interface it
 * protects itself, i.e. modifications to an ACL are only allowed for principals
 * having the permission <code>MCRStandardPermissions.ACL_MODIFY</code>. This
 * permission is by default granted to the owner of the object which owns this
 * ACL.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */

public final class MCRDefaultAcl implements MCRAcl, MCRAclGuarded {
	///============================================================================/

	//   private final Map genericEntries = new LinkedHashMap();
	//   private final Map userEntries = new LinkedHashMap();
	//   private final Map groupEntries = new LinkedHashMap();

	private final Map genericEntries = new HashMap();

	private final Map userEntries = new HashMap();

	private final Map groupEntries = new HashMap();

	private final MCRPermissionContext context;

	private final MCROwnedObject object;

	//+-----------------------------------------------------------------------------

	/**
	 * Constructs an ACL for an object and a permission context
	 * 
	 * @param object
	 *            the object which owns this ACL.
	 * @param context
	 *            the permission context for the ACL.
	 */

	public MCRDefaultAcl(MCROwnedObject object, MCRPermissionContext context) {

		this.object = object;
		this.context = context;

		initializeGenericEntries();

	}

	//>-----------------------------------------------------------------------------

	private void initializeGenericEntries() {

		MCRPermissionRing ownerPermissions = new MCRDefaultPermissionRing();
		ownerPermissions.putPermission(MCRStandardPermissions.ACL_MODIFY, true);
		ownerPermissions.putPermission(MCRStandardPermissions.ACL_READ, true);

		MCRPermissionRing ownerGroupPermissions = new MCRDefaultPermissionRing();
		ownerGroupPermissions.putPermission(MCRStandardPermissions.ACL_READ,
				true);

		MCRPermissionRing otherPermissions = new MCRDefaultPermissionRing();

		MCRPermissionRing anyOtherPermissions = new MCRDefaultPermissionRing();

		genericEntries.put(MCRAclCategory.OWNER, ownerPermissions);
		genericEntries.put(MCRAclCategory.OWNER_GROUP, ownerGroupPermissions);
		genericEntries.put(MCRAclCategory.OTHER, otherPermissions);
		genericEntries.put(MCRAclCategory.ANY_OTHER, anyOtherPermissions);

	}

	//------------------------------------------------------------------------------
	// Methods for modifying/editing ACLs
	//------------------------------------------------------------------------------

	/**
	 * Sets permissions for the specified principal. If no corresponding entry
	 * exists the principal is added to this ACL.
	 * 
	 * @param principal
	 *            the category, user or group to be added to this ACL.
	 * @param permissionRing
	 *            the permissions assigned to the principal.
	 * 
	 * @throws MCRAccessException
	 *             if modifying this ACL is not allowed.
	 * @throws MCRInvalidPermissionException
	 *             if permissionRing contains invalid permissions for this
	 *             context.
	 * @throws MCRInvalidPrincipalException
	 *             if it is not allowed to add this kind of principal.
	 */

	public void putPermissions(MCRPrincipal principal,
			MCRPermissionRing permissionRing) throws MCRAccessException,
			MCRInvalidPrincipalException, MCRInvalidPermissionException {

		checkAccess(MCRStandardPermissions.ACL_MODIFY);

		checkPermissions(permissionRing);

		put(principal, permissionRing.copyPermissions());

	}

	//------------------------------------------------------------------------------

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

	public MCRPermissionRing getPermissions(MCRPrincipal principal)
			throws MCRInvalidPrincipalException {

		return get(principal).copyPermissions();

	}

	//------------------------------------------------------------------------------

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

	public void deletePrincipal(MCRPrincipal principal)
			throws MCRAccessException, MCRInvalidPrincipalException {

		checkAccess(MCRStandardPermissions.ACL_MODIFY);

		delete(principal);

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if there is an entry for a principal in this ACL.
	 * 
	 * @param principal
	 *            a category, user or group to be deleted form this ACL.
	 * 
	 * @return true if the principal is contained in this ACL.
	 */

	public boolean containsPrincipal(MCRPrincipal principal)
			throws MCRAccessException, MCRInvalidPrincipalException {

		return contains(principal);

	}

	//------------------------------------------------------------------------------

	/**
	 * Copies permissions from one entry to another.
	 * 
	 * @param source
	 *            a category, user or group.
	 * @param target
	 *            a category, user or group.
	 * 
	 * @throws MCRAclAccessException
	 *             if modifying this ACL is not allowed.
	 * @throws MCRInvalidPrincipalException
	 *             if the source entry is not contained in this ACL.
	 */

	public void copyPermissions(MCRPrincipal source, MCRPrincipal target)
			throws MCRAccessException, MCRInvalidPrincipalException {

		checkAccess(MCRStandardPermissions.ACL_MODIFY);

		put(target, get(source).copyPermissions());

	}

	//------------------------------------------------------------------------------

	/**
	 * Gets a set of the users currently contained as entries in this ACL.
	 * 
	 * @return a set of the users or the empty set if the ACL has no user
	 *         entries.
	 */

	public Set getUsers() {

		// A MCRException may be thrown if the user database changes during the
		// lifetime of an ACL !!!
		return MCRUserMgr.instance().retrieveUsers(
				new HashSet(userEntries.keySet()));

	}

	//------------------------------------------------------------------------------

	/**
	 * Gets a set of the groups currently contained as entries in this ACL.
	 * 
	 * @return a set of the groups or the empty set if the ACL has no group
	 *         entries.
	 */

	public Set getGroups() {

		// A MCRException may be thrown if the user database changes during the
		// lifetime of an ACL !!!
		return MCRUserMgr.instance().retrieveGroups(
				new HashSet(groupEntries.keySet()));

	}

	//------------------------------------------------------------------------------

	/**
	 * Gets the permission context for this ACL. The context determines which
	 * permissions are valid for this ACL. In general a permission context is
	 * unique per class, so that one context is shared by all ACLs which are
	 * attached to instances of that class.
	 * 
	 * @return the permission context of this ACL.
	 */

	public MCRPermissionContext getPermissionContext() {

		return context;

	}

	//------------------------------------------------------------------------------
	// Methods for checking access
	//------------------------------------------------------------------------------

	/**
	 * Gets the category <code>user</code> is assigned to by this ACL with
	 * respect to object.
	 * 
	 * Obtaining the category normally is the first step of the algorithm which
	 * determines if a permission is granted or not.
	 * 
	 * @param user
	 *            an arbitrary user.
	 * @param object
	 *            the owner information of this object is used to determine. if
	 *            <code>user</code> is owner or in owner group.
	 * @param members
	 *            on return the set contains the members of the category.
	 * 
	 * @return the category the user is assigend to by the ACL.
	 */

	public MCRAclCategory getCategory(MCRUser user, MCROwnedObject object,
			Set members) {

		if (user.equals(object.getOwner()))
			return MCRAclCategory.OWNER;
		if (containsPrincipal(user))
			return MCRAclCategory.USERS;
		if (user.isMemberOf(object.getOwnerGroup()))
			return MCRAclCategory.OWNER_GROUP;

		Set groups = getGroupsContainingUser(user, getGroups()); // this may
		// throw an
		// MCRException
		// at
		// runtime!!!

		if (!groups.isEmpty()) {

			if (members != null) {

				members.clear();
				members.addAll(groups);

			}

			return MCRAclCategory.GROUPS;

		}

		if (user.isAuthenticated())
			return MCRAclCategory.OTHER;
		return MCRAclCategory.ANY_OTHER;
	}

	//------------------------------------------------------------------------------

	/**
	 * Gets the set of permissions for a user established by the algorithm of
	 * this ACL. The ACL decides to which category the user matches and returns
	 * the corresponding permissions. This method generally implements the logic
	 * of the ACL and is therfore used in <code>checkAccess</code>. It can
	 * also be used to implement a cache with permission information for users
	 * who have already called methods on a guarded object.
	 * 
	 * @param user
	 *            an arbitrary user.
	 * @param object
	 *            the owner information of this object is used to determine. if
	 *            user is owner or in owner group.
	 * 
	 * @return the permissions the user has with respect to this ACL. This can
	 *         be null if no category matches for <code>user</code>.
	 */

	public MCRPermissionRing getAccessPermissions(MCRUser user,
			MCROwnedObject object) {

		Set categoryMembers = new HashSet();

		MCRAclCategory category = getCategory(user, object, categoryMembers);

		if (category == MCRAclCategory.USERS)
			return getPermissions(user);

		if (category == MCRAclCategory.GROUPS)
			return accumulatePermissions(categoryMembers);

		if (containsPrincipal(category))
			return getPermissions(category);
		else
			return null;

	}

	//------------------------------------------------------------------------------

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

	public boolean isAccessPermitted(MCRUser user, MCROwnedObject object,
			MCRPermission permission) {

		MCRPermissionRing permissionRing = getAccessPermissions(user, object);

		if (permissionRing == null)
			return false; // no category
		else
			return permissionRing.isPermissionGranted(permission); // the status
		// of the
		// permission

	}

	//------------------------------------------------------------------------------
	// Methods of interface MCRAclGuarded
	//------------------------------------------------------------------------------

	/**
	 * Returns the ACL by which this object is guarded, which is the ACL itself
	 * 
	 * @return the ACL which guards this objects
	 */

	public MCRAcl getAcl() {

		return this;

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if the current user can perform the action specified by
	 * permission.
	 * 
	 * @param permission
	 *            the permission required to perform the action.
	 * 
	 * @throws AccessException
	 *             if the currentUser is not allowed to perform the action.
	 */

	public void checkAccess(MCRPermission permission) throws MCRAccessException {

		MCRUser currentUser = MCRUserMgr.instance().getCurrentUser();

		if (currentUser == null)
			throw new MCRAccessException(MCRAclMessages.NO_USER_MESSAGE);

		if (currentUser.equals(object.getOwner())
				&& (permission == MCRStandardPermissions.ACL_MODIFY))
			return; // the owner has always permission to edit an ACL.

		if (!isAccessPermitted(currentUser, object, permission))
			throw new MCRAccessException(MCRAclMessages.ACL_MODIFY_MESSAGE
					+ currentUser.getID());

	}

	//------------------------------------------------------------------------------
	// Private utility methods
	//------------------------------------------------------------------------------

	private void checkPermission(MCRPermission permission)
			throws MCRInvalidPermissionException {

		if (!context.isValidPermission(permission))
			throw new MCRInvalidPermissionException(
					MCRAclMessages.INVALID_PERMISSION_MESSAGE + ": "
							+ permission);

	}

	//------------------------------------------------------------------------------

	private void checkPermissions(MCRPermissionRing permissionRing)
			throws MCRInvalidPermissionException {

		for (Iterator iterator = permissionRing.iterator(); iterator.hasNext();)
			checkPermission((MCRPermission) iterator.next());

	}

	//------------------------------------------------------------------------------

	private final Set getGroupsContainingUser(MCRUser user, Set groups) {

		Set set = new HashSet();

		Iterator iterator = groups.iterator();

		while (iterator.hasNext()) {

			Object element = iterator.next();

			if (element instanceof MCRGroup) {

				MCRGroup group = (MCRGroup) element;

				if (user.isMemberOf(group))
					set.add(group);

			}

		}

		return set;

	}

	//------------------------------------------------------------------------------
	// Methods for storing and retrieving
	//------------------------------------------------------------------------------

	private Map getMap(MCRPrincipal principal)
			throws MCRInvalidPrincipalException {

		if (principal instanceof MCRAclCategory)
			return genericEntries;
		else if (principal instanceof MCRUser)
			return userEntries;
		else if (principal instanceof MCRGroup)
			return groupEntries;
		else
			throw new MCRInvalidPrincipalException(
					MCRAclMessages.UNSUPPORTED_PRINCIPAL_MESSAGE);

	}

	//------------------------------------------------------------------------------

	private Object getKey(MCRPrincipal principal)
			throws MCRInvalidPrincipalException {

		if (principal instanceof MCRAclCategory)
			return principal;
		else if (principal instanceof MCRUser)
			return principal.getID();
		else if (principal instanceof MCRGroup)
			return principal.getID();
		else
			throw new MCRInvalidPrincipalException(
					MCRAclMessages.UNSUPPORTED_PRINCIPAL_MESSAGE);

	}

	//------------------------------------------------------------------------------

	private MCRPermissionRing get(MCRPrincipal principal)
			throws MCRInvalidPrincipalException {

		Map map = getMap(principal);
		Object key = getKey(principal);

		if (!map.containsKey(key))
			throw new MCRInvalidPrincipalException(
					MCRAclMessages.MISSING_PRINCIPAL_MESSAGE);

		return (MCRPermissionRing) map.get(key);

	}

	//------------------------------------------------------------------------------

	private void put(MCRPrincipal principal, MCRPermissionRing permissionRing)
			throws MCRInvalidPrincipalException {

		// Categories USERS and GROUPS are not allowed as entries in ACLs, only
		// their
		// individual members

		if (principal == MCRAclCategory.USERS
				|| principal == MCRAclCategory.GROUPS)
			throw new MCRInvalidPrincipalException(
					MCRAclMessages.INVALID_CATEGORY_MESSAGE);

		Map map = getMap(principal);
		Object key = getKey(principal);

		map.put(key, permissionRing);

	}

	//------------------------------------------------------------------------------

	private void delete(MCRPrincipal principal)
			throws MCRInvalidPrincipalException {

		Map map = getMap(principal);
		Object key = getKey(principal);

		if (!map.containsKey(key))
			throw new MCRInvalidPrincipalException(
					MCRAclMessages.MISSING_PRINCIPAL_MESSAGE);

		map.remove(key);

	}

	//------------------------------------------------------------------------------

	private boolean contains(MCRPrincipal principal) {

		Map map = getMap(principal);
		Object key = getKey(principal);

		return map.containsKey(key);

	}

	//------------------------------------------------------------------------------

	public MCRPermissionRing accumulatePermissions(Set set) {

		MCRPermissionRing permissionRing = null;

		Iterator iterator = set.iterator();

		while (iterator.hasNext()) {

			Object object = iterator.next();

			if (object instanceof MCRPrincipal) {

				MCRPrincipal principal = (MCRPrincipal) object;

				if (containsPrincipal(principal)) {

					if (permissionRing == null)
						permissionRing = new MCRDefaultPermissionRing();

					permissionRing.addRing(getPermissions(principal));

				}

			}

		}

		return permissionRing;

	}

	//------------------------------------------------------------------------------

	/**
	 * Returns a string representation of the ACL
	 * 
	 * @return a string used for printing the ACL.
	 */

	public String toString() {

		return MCRAclUtilities.printAcl(this);

	}

	//-============================================================================\
}