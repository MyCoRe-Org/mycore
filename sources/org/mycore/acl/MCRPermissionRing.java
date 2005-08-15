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

import java.util.Iterator;

///============================================================================|

/**
 * This interface defines a set of permissions.
 * 
 * <P>
 * Permissions are attached to principals and define the collection of rights to
 * access a certain object.
 * 
 * <P>
 * A permission can be granted or denied. A denied permission is similar to a
 * missing permission, but algorithms which check access to resources may come
 * to a (negative) decision earlier when a permission is explicitly denied.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */

public interface MCRPermissionRing {
	///============================================================================/

	/**
	 * Constant representing the status "granted" of a permission.
	 */

	public static final Boolean GRANTED = new Boolean(true);

	/**
	 * Constant representing the status "denied" of a permission.
	 */

	public static final Boolean DENIED = new Boolean(false);

	//------------------------------------------------------------------------------

	/**
	 * Adds a permission to this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be added
	 * @param grant
	 *            if <code>true</code> permission is granted, denied
	 *            otherwise.
	 */

	public void putPermission(MCRPermission permission, boolean grant);

	//------------------------------------------------------------------------------

	/**
	 * Deletes a permission from this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be deleted.
	 */

	public void deletePermission(MCRPermission permission);

	//------------------------------------------------------------------------------

	/**
	 * Checks if the given permission is contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be checked
	 * 
	 * @return true if the permission is contained in this set.
	 */

	public boolean containsPermission(MCRPermission permission);

	//------------------------------------------------------------------------------

	/**
	 * Checks if the given permission contained in this set of permissions is
	 * granted.
	 * 
	 * @param permission
	 *            the permission to be checked.
	 * 
	 * @return true if the permission is contained in this set and is granted,
	 *         false if it is not contained or denied explicitly.
	 */

	public boolean isPermissionGranted(MCRPermission permission);

	//------------------------------------------------------------------------------

	/**
	 * Grants the given permission contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be granted.
	 */

	public void grantPermission(MCRPermission permission);

	//------------------------------------------------------------------------------

	/**
	 * Denies the given permission contained in this set of permissions.
	 * 
	 * @param permission
	 *            the permission to be denied.
	 */

	public void denyPermission(MCRPermission permission);

	//------------------------------------------------------------------------------

	/**
	 * Creates a copy of this set of permissions. Sometimes principals have the
	 * same set of permissions. While single permissions may be shared this is
	 * not true for sets of permissions, because they are mutable.
	 * 
	 * @return a new set of permissions with identical content.
	 */

	public MCRPermissionRing copyPermissions();

	//------------------------------------------------------------------------------

	/**
	 * Returns an iterator over the containd permissions.
	 * 
	 * @return an iterator over the containd permissions.
	 */

	public Iterator iterator();

	//------------------------------------------------------------------------------

	/**
	 * Adds all permissions of <code>permissionRing</code> to this ring,
	 * excluding duplicates.
	 * 
	 * @param permissionRing
	 *            the ring to be added.
	 */

	public void addRing(MCRPermissionRing permissionRing);

	//-============================================================================\
}