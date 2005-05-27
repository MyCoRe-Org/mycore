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
 * This class is an implementation of interface <code>MCRPermission</code>.
 * 
 * <P>
 * It overrides method <code>equals</code> from <code>java.lang.Object</code>,
 * defining permissions as equal when they are identical.
 * 
 * <P>
 * Classes protected by ACLs containing this type of permission should define
 * their supported permissions as static constants, so that a single instance of
 * this class represents the permission.
 * 
 * <P>
 * Instances of this class are immutable. This means that multiple ACLs can hold
 * references to the single instance representing a permission for a class.
 * 
 * <P>
 * The string representation of a permission is it's name.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */

public class MCRDefaultPermission implements MCRPermission {
    ///============================================================================/

    private String name;

    private String description;

    //+-----------------------------------------------------------------------------

    /**
     * Constructs a permission by name which is also the description.
     */

    public MCRDefaultPermission(String name) {

        this(name, name);

    }

    //>-----------------------------------------------------------------------------

    /**
     * Constructs a permission by name with an additional description.
     */

    public MCRDefaultPermission(String name, String description) {

        this.name = name;
        this.description = description;

    }

    //>-----------------------------------------------------------------------------

    /**
     * Returns the name of the permission.
     * 
     * @return string representing the name of the permission.
     */

    public String getName() {

        return name;

    }

    //------------------------------------------------------------------------------

    /**
     * Returns the description of the permission.
     * 
     * @return string representing the description of the permission.
     */

    public String getDescription() {

        return description;

    }

    //------------------------------------------------------------------------------

    /**
     * Compares two permissions.
     * 
     * @return true if both instances are identical!
     */

    public boolean equals(Object object) {

        return this == object;

    }

    //------------------------------------------------------------------------------

    /**
     * Returns the string representation of the permission, which is it's name.
     * 
     * @return string representing the permission.
     */

    public String toString() {

        return getName();

    }

    //-=============================================================================
}