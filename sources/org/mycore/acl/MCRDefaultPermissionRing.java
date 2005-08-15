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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

///============================================================================|

/**
 * Default implementation of interface <code>MCRPermissionRing</code>
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */

public class MCRDefaultPermissionRing implements MCRPermissionRing {
	///============================================================================/

	private Map permissions;

	//+-----------------------------------------------------------------------------

	/**
	 * Constructs an empty permission list.
	 */

	public MCRDefaultPermissionRing() {

		//      permissions = new LinkedHashMap();
		permissions = new HashMap();

	}

	//>-----------------------------------------------------------------------------

	/**
	 * Constructs a permission ring with initial single entry
	 * <code>permission</code> setting it's status to <code>grant</code>.
	 */

	public MCRDefaultPermissionRing(MCRPermission permission, boolean grant) {

		this();

		putPermission(permission, grant);

	}

	//>-----------------------------------------------------------------------------

	/**
	 * Constructs a permission ring with entries taken from <code>perms</code>.
	 * The status of all entries is set to grant.
	 */

	public MCRDefaultPermissionRing(MCRPermission[] perms, boolean grant) {

		this();

		if (perms == null)
			return;

		for (int i = 0; i < perms.length; i++)
			putPermission(perms[i], grant);

	}

	//>-----------------------------------------------------------------------------

	/**
	 * Constructs a permission ring from a map. Used as copy constructor.
	 */

	private MCRDefaultPermissionRing(Map permissions) {

		this.permissions = permissions;

	}

	//>-----------------------------------------------------------------------------

	/**
	 * Adds a permission to this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be added.
	 * @param grant
	 *            if <code>true</code> permission is granted, denied
	 *            otherwise.
	 */

	public void putPermission(MCRPermission permission, boolean grant) {

		permissions.put(permission, grant ? GRANTED : DENIED);

	}

	//------------------------------------------------------------------------------

	/**
	 * Deletes a permission from this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be deleted.
	 */

	public void deletePermission(MCRPermission permission) {

		permissions.remove(permission);

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if the given permission is contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be checked.
	 * 
	 * @return true if the permission is contained in this set, false otherwise.
	 */

	public boolean containsPermission(MCRPermission permission) {

		return permissions.containsKey(permission);

	}

	//------------------------------------------------------------------------------

	/**
	 * Checks if the given permission contained in this set of permissions is
	 * granted.
	 * 
	 * @param permission
	 *            the permission to be checked.
	 * 
	 * @return true if the permission is contained in this ring and is granted,
	 *         false if it is not contained or denied explicitly.
	 */

	public boolean isPermissionGranted(MCRPermission permission) {

		if (!containsPermission(permission))
			return false;
		else
			return ((Boolean) permissions.get(permission)).booleanValue();

	}

	//------------------------------------------------------------------------------

	/**
	 * Grants the given permission contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be granted.
	 */

	public void grantPermission(MCRPermission permission) {

		if (!containsPermission(permission))
			return;
		else
			permissions.put(permission, GRANTED);

	}

	//------------------------------------------------------------------------------

	/**
	 * Denies the given permission contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be denied.
	 */

	public void denyPermission(MCRPermission permission) {

		if (!containsPermission(permission))
			return;
		else
			permissions.put(permission, DENIED);

	}

	//------------------------------------------------------------------------------

	/**
	 * Creates a copy of this set of permissions.
	 * 
	 * @return a new set of permissions with identical content.
	 */

	public MCRPermissionRing copyPermissions() {

		//MCRDefaultPermissionRing newPermissionRing = new
		// MCRDefaultPermissionRing(new LinkedHashMap(permissions));
		MCRDefaultPermissionRing newPermissionRing = new MCRDefaultPermissionRing(
				new HashMap(permissions));

		return newPermissionRing;

	}

	//------------------------------------------------------------------------------

	/**
	 * Returns an iterator over the containd permissions.
	 * 
	 * @return an iterator over the containd permissions.
	 */

	public Iterator iterator() {

		return permissions.keySet().iterator();

	}

	//------------------------------------------------------------------------------

	/**
	 * Adds all permissions of <code>permissionRing</code> to this ring,
	 * excluding duplicates
	 * 
	 * @param permissionRing
	 *            the ring to be added
	 */

	public void addRing(MCRPermissionRing permissionRing) {

		for (Iterator iterator = permissionRing.iterator(); iterator.hasNext();) {

			MCRPermission permission = (MCRPermission) iterator.next();
			if (!containsPermission(permission))
				putPermission(permission, permissionRing
						.isPermissionGranted(permission));

		}

	}

	//------------------------------------------------------------------------------

	/**
	 * String representation of permissions. Every permission is represented by
	 * it's name followed by the status (+) or (-).
	 * 
	 * @return string representation of a set of permissions.
	 */

	public String toString() {

		StringBuffer buffer = new StringBuffer();

		Iterator iterator = iterator();

		while (iterator.hasNext()) {

			MCRPermission permission = (MCRPermission) iterator.next();

			buffer.append(permission.getName());
			buffer.append("(");
			buffer.append(isPermissionGranted(permission) ? "+" : "-");
			buffer.append(")");
			buffer.append(", ");

		}

		int bufferLength = buffer.length();
		if (bufferLength > 2)
			buffer.delete(bufferLength - 2, bufferLength);

		return buffer.toString();

	}

	//-============================================================================\
}