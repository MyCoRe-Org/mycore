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
 * This class defines some standard permissions which are meaningful for most
 * classes.
 * 
 * <P>
 * The type is not an interface to avoid the constant interface antipattern, so
 * at present the static constants defined in this class will have to be used
 * with there full name. This will change in an upcoming release of java where
 * static imports are available
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */
public class MCRStandardPermissions {
    // /============================================================================/

    /**
     * Permission which allows the modification of an ACL.
     */
    public static final MCRPermission ACL_MODIFY = new MCRDefaultPermission("acl_modify", "Permission to modify the ACL of a guarded object");

    /**
     * Permission which allows to read parts of an ACL.
     */
    public static final MCRPermission ACL_READ = new MCRDefaultPermission("acl_read", "Permission to read the ACL of a guarded object");

    // +-----------------------------------------------------------------------------
    private MCRStandardPermissions() {
    }

    // >============================================================================\
}
