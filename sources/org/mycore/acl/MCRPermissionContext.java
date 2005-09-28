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

// /============================================================================§

/**
 * This interface defines a permission context.
 * 
 * <P>
 * Every class can define it's own set of valid or supported permissions which
 * generally correspond to checked method calls and will be used in entries of
 * attached ACLs. If such a class implements this interface or has a delegate
 * which does, the connection to other parts of the ACL architecture is
 * established.
 * 
 * <P>
 * Since permissions should be unique instances, a permission context can be
 * asked for the instance by name and check if a permission is valid for this
 * context. It can also return all valid permission as an array which can e.g.
 * be used to present the valid choices for permissions in an ACL editor.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */
public interface MCRPermissionContext {
    // /============================================================================/

    /**
     * Checks if permission is valid for this context. This method is useful
     * when editing ACLs to prevent inconsistent entries.
     * 
     * @return true if the permission is valid for this context.
     */
    public boolean isValidPermission(MCRPermission permission);

    // ------------------------------------------------------------------------------

    /**
     * Returns the valid permissions within this context.
     * 
     * This method can be used to provide choices for legal permissions when
     * building an ACL editor.
     * 
     * @return an array of permissions which are valid with this context
     */
    public MCRPermission[] getValidPermissions();

    // ------------------------------------------------------------------------------

    /**
     * Returns the single instance of the permission within this context which
     * has the corresponding name.
     * 
     * This method is used when creating permissions from external
     * representations.
     * 
     * @return a permission valid within this context
     * 
     * @throws MCRInvalidPermissionException
     *             when no such permission exists
     */
    public MCRPermission getPermission(String name) throws MCRInvalidPermissionException;

    // -============================================================================\
}
