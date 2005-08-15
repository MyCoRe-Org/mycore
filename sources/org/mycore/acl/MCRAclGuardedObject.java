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

import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

///============================================================================|

/**
 * This class implements the interfaces MCRAclGuarded, MCRPermissionContext,
 * MCROwnedObject, thus providing full support for ACLs.
 * 
 * Extending this class establishes ACL support, Subclasses can also serve as
 * delegates and may be realized as inner classes.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 * @see MCROwnedObject
 * @see MCRPermissionContext
 * @see MCRAclGuarded
 */

public class MCRAclGuardedObject implements MCROwnedObject,
		MCRPermissionContext, MCRAclGuarded {
	///============================================================================/

	private MCRUser owner;

	private MCRGroup ownerGroup;

	private MCRPermission[] supportedPermissions;

	private MCRAcl acl;

	//+-----------------------------------------------------------------------------

	/**
	 * Constructs a guarded object with the specified owner and owning group.
	 */

	public MCRAclGuardedObject(MCRUser owner, MCRGroup ownerGroup,
			MCRPermission[] supportedPermissions) {

		this.owner = owner;
		this.ownerGroup = ownerGroup;
		this.acl = new MCRDefaultAcl(this, this);
		this.supportedPermissions = supportedPermissions;

	}

	//>-----------------------------------------------------------------------------
	// Implementation of MCROwnedObject
	//------------------------------------------------------------------------------

	/**
	 * Returns the owner of this guarded object.
	 * 
	 * @return the owner of this object.
	 */

	public final MCRUser getOwner() {

		return owner;

	}

	//------------------------------------------------------------------------------

	/**
	 * Sets a new owner for this guarded object.
	 * 
	 * @throws AccessException
	 *             if the currentUser is not allowed to set a new owner.
	 */

	public final void changeOwner(MCRUser newOwner) throws MCRAccessException {//@

		if (newOwner == null)
			return;

		MCRUser currentUser = MCRUserMgr.instance().getCurrentUser();

		if (currentUser == null)
			throw new MCRAccessException(MCRAclMessages.NO_USER_MESSAGE);

		if (currentUser.isMemberOf(MCRUserMgr.instance()
				.getOwnerAdministrationGroup()))
			owner = newOwner;
		else
			throw new MCRAccessException(MCRAclMessages.CHANGE_OWNER_MESSAGE);

	}

	//------------------------------------------------------------------------------

	/**
	 * Returns the owner of this guarded object.
	 * 
	 * @return the owner of this object.
	 */

	public final MCRGroup getOwnerGroup() {

		return ownerGroup;

	}

	//------------------------------------------------------------------------------

	/**
	 * Sets a new owner group for this guarded object.
	 * 
	 * @throws AccessException
	 *             if the currentUser is not allowed to set a new owner.
	 */

	public final void changeOwnerGroup(MCRGroup newOwnerGroup)
			throws MCRAccessException {

		if (newOwnerGroup == null)
			return;

		MCRUser currentUser = MCRUserMgr.instance().getCurrentUser();

		if (currentUser == null)
			throw new MCRAccessException(MCRAclMessages.NO_USER_MESSAGE);

		if (currentUser.isMemberOf(MCRUserMgr.instance()
				.getOwnerAdministrationGroup()))
			ownerGroup = newOwnerGroup;
		else
			throw new MCRAccessException(
					MCRAclMessages.CHANGE_OWNER_GROUP_MESSAGE);

	}

	//------------------------------------------------------------------------------
	// Implementation of MCRPermissionContext
	//------------------------------------------------------------------------------

	/**
	 * Returns the allowed permissions of this guarded object.
	 * 
	 * When ACL are modified they should check their entries for supported
	 * permissions of the objects they guard.
	 * 
	 * @return a set of permissions which are meaningful for this object.
	 */

	public final MCRPermission[] getValidPermissions() {

		return (MCRPermission[]) supportedPermissions.clone();

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if permission is valid for this context. This method is useful
	 * when editing ACLs to prevent inconsistent entries.
	 * 
	 * @return true if the permission is valid for this context.
	 */

	public boolean isValidPermission(MCRPermission permission) {

		for (int i = 0; i < supportedPermissions.length; i++)
			if (supportedPermissions[i].equals(permission))
				return true;

		return false;

	}

	//------------------------------------------------------------------------------

	/**
	 * Returns the single instance of the permission within this context which
	 * has the corresponding name.
	 * 
	 * This method is used when creating permissions from external
	 * representations.
	 * 
	 * @return a permissions valid within this context.
	 * 
	 * @throws MCRInvalidPermissionException
	 *             when no such permission exists
	 */

	public MCRPermission getPermission(String name)
			throws MCRInvalidPermissionException {

		for (int i = 0; i < supportedPermissions.length; i++)
			if (supportedPermissions[i].getName().equals(name))
				return supportedPermissions[i];

		throw new MCRInvalidPermissionException(
				MCRAclMessages.INVALID_PERMISSION_MESSAGE);

	}

	//------------------------------------------------------------------------------
	// Implementation of MCRAclGuarded
	//------------------------------------------------------------------------------

	/**
	 * Returns the ACL by which this object is guarded.
	 * 
	 * @return the ACL which guards this objects.
	 */

	public final MCRAcl getAcl() {

		checkAccess(MCRStandardPermissions.ACL_READ);

		return acl;

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if the given user can perform the action specified by permission.
	 * 
	 * @param permission
	 *            the permission required to perform the action.
	 * 
	 * @throws MCRAccessException
	 *             if the current user is not allowed to perform the action.
	 */

	public final void checkAccess(MCRPermission permission)
			throws MCRAccessException {

		MCRUser currentUser = MCRUserMgr.instance().getCurrentUser();

		if (currentUser == null)
			throw new MCRAccessException(MCRAclMessages.NO_USER_MESSAGE);

		if (!acl.isAccessPermitted(currentUser, this, permission))
			throw new MCRAccessException(MCRAclMessages.ACCESS_MESSAGE
					+ MCRAclMessages.userMessage(currentUser)
					+ MCRAclMessages.permissionMessage(permission));

	}

	//-============================================================================\
}