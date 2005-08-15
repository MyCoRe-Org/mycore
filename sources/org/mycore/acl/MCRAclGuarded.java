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

/**
 * This interface characterize objects which are guarded by an access control
 * list (ACL).
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */

public interface MCRAclGuarded {
	///============================================================================/

	/**
	 * Returns the ACL by which this object is guarded. This should be the only
	 * way to obtain an ACL for modifying. In implementing classes the reference
	 * to an ACL should be final, so that changing an ACL can only be made by
	 * modifying the existing one and not by assigning a new ACL.
	 * 
	 * @return the ACL which guards this objects.
	 * 
	 * @throws MCRAccessException
	 *             if the current user is not allowed to read the ACL.
	 */

	public MCRAcl getAcl() throws MCRAccessException;

	//------------------------------------------------------------------------------

	/**
	 * Checks if the current user can perform the action specified by
	 * permission.
	 * 
	 * @param permission
	 *            the permission required to perform the action.
	 * 
	 * @throws MCRAccessException
	 *             if the currentUser is not allowed to perform the action.
	 */

	public void checkAccess(MCRPermission permission) throws MCRAccessException;

	//-============================================================================\
}