/**
 *
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
 *
 **/


package org.mycore.acl;
///============================================================================§

import org.mycore.user.MCRUser;
///============================================================================|

/**
 * This class defines messages used for error handling.
 * Future versions will handle internationalization
 *
 * @author   Benno Süselbeck
 * @version  1.0.0, 01.11.2003
 **/

public class MCRAclMessages {
///============================================================================/

   public final static String ACL_MODIFY_MESSAGE             = "Modifying ACL not allowed for user ";
   public final static String UNSUPPORTED_PRINCIPAL_MESSAGE  = "Unsupported principal";
   public final static String MISSING_PRINCIPAL_MESSAGE      = "Principal not contained in this ACL";
   public final static String INVALID_CATEGORY_MESSAGE       = "Invalid category";
   public final static String NO_USER_MESSAGE                = "current user undefined";
   public final static String INVALID_PERMISSION_MESSAGE     = "Permission not valid within this context";
   public final static String ACCESS_MESSAGE                 = "Access failed";
   public final static String CHANGE_OWNER_GROUP_MESSAGE     = "changing owner group not allowed";
   public final static String CHANGE_OWNER_MESSAGE           = "changing owner not allowed";

//+-----------------------------------------------------------------------------

   public final static String PERMISSION_PREFIX  = "Permission \"";
   public final static String PERMISSION_POSTFIX = "\" denied.";

   public final static String USER_PREFIX  = " for user \"";
   public final static String USER_POSTFIX = "\": ";

//+-----------------------------------------------------------------------------

   public static String permissionMessage (MCRPermission permission) {

      return PERMISSION_PREFIX + permission + PERMISSION_POSTFIX;

     }

//------------------------------------------------------------------------------

   public static String userMessage (MCRUser user) {

      return USER_PREFIX + user.getID() + USER_POSTFIX;

     }

   
//-============================================================================\
}