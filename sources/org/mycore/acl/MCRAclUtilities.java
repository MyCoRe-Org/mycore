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

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.mycore.user.MCRPrincipal;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRGroup;
///============================================================================|

/**
 * This class defines some utility methods for ACLs.
 *
 * @author   Benno Süselbeck
 * @version  1.0.0, 01.11.2003
 **/

public class MCRAclUtilities {
///============================================================================/

   private final static String SEPARATOR = ": ";
   private final static String NEWLINE = "\n";
   private static String ENTRY_PREFIX = "  ";

   private static String GENERIC_TITLE = "Generic entries";
   private static String USER_TITLE    = "User entries";
   private static String GROUP_TITLE   = "Group entries";
   

//------------------------------------------------------------------------------

   private static void printEntry (MCRPrincipal principal, MCRAcl acl, StringBuffer buffer) {
   
       if (!acl.containsPrincipal(principal)) return;
   
       buffer.append(ENTRY_PREFIX);
       buffer.append(principal.getID());
       buffer.append(SEPARATOR);
       buffer.append(acl.getPermissions(principal).toString());
       buffer.append(NEWLINE);
   
     }

//------------------------------------------------------------------------------

   private static void printCategory (Set category, MCRAcl acl, StringBuffer buffer) {
         
      Iterator iterator = category.iterator();
      
      while (iterator.hasNext()) {
      
         Object element = iterator.next();
 
         if (element instanceof MCRPrincipal) {
         
            MCRPrincipal principal = (MCRPrincipal)element;

            printEntry(principal, acl, buffer);

           }
   
        }

     }
  
//------------------------------------------------------------------------------

  /**
   * Returns a string representation of an ACL
   * 
   * @return a string used for printing an ACL..   
   */

   public static String printAcl(MCRAcl acl) {
   
      StringBuffer buffer = new StringBuffer(); 
  
      buffer.append(GENERIC_TITLE);
      buffer.append(SEPARATOR);
      buffer.append(NEWLINE);
      printEntry(MCRAclCategory.OWNER,       acl, buffer);
      printEntry(MCRAclCategory.OWNER_GROUP, acl, buffer);
      printEntry(MCRAclCategory.OTHER,       acl, buffer);
      printEntry(MCRAclCategory.ANY_OTHER,   acl, buffer);
  
  
      buffer.append(USER_TITLE);
      buffer.append(SEPARATOR);
      buffer.append(NEWLINE);
      printCategory(acl.getUsers(), acl, buffer);
      
      buffer.append(GROUP_TITLE);
      buffer.append(SEPARATOR);
      buffer.append(NEWLINE);
      printCategory(acl.getGroups(), acl, buffer);	       

      return buffer.toString();
  
     }

//------------------------------------------------------------------------------

   public static String toXML (MCRAcl acl) {

      StringBuffer buffer = new StringBuffer();
      addXML(buffer,acl);
      return buffer.toString();
   
     }

//------------------------------------------------------------------------------

   private static final void addXML (StringBuffer buffer, MCRAcl acl) {

      buffer.append("<acl>"); 
      buffer.append("\n");


      if (acl.containsPrincipal(MCRAclCategory.OWNER))
      buffer.append("<owner>");
      buffer.append("\n");
      addPermissionRing(buffer, acl.getPermissions(MCRAclCategory.OWNER));
      buffer.append("</owner>");
      buffer.append("\n");

      if (acl.containsPrincipal(MCRAclCategory.OWNER_GROUP))
      buffer.append("<owner_group>");
      buffer.append("\n");
      addPermissionRing(buffer, acl.getPermissions(MCRAclCategory.OWNER_GROUP));
      buffer.append("</owner_group>");
      buffer.append("\n");

      addUsers(buffer, acl);
      addGroups(buffer, acl);

      if (acl.containsPrincipal(MCRAclCategory.OWNER_GROUP))
      buffer.append("<other>");
      buffer.append("\n");
      addPermissionRing(buffer, acl.getPermissions(MCRAclCategory.OWNER_GROUP));
      buffer.append("</other>");
      buffer.append("\n");

      if (acl.containsPrincipal(MCRAclCategory.OWNER_GROUP))
      buffer.append("<any_other>");
      buffer.append("\n");
      addPermissionRing(buffer, acl.getPermissions(MCRAclCategory.OWNER_GROUP));
      buffer.append("</any_other>");
      buffer.append("\n");


      buffer.append("</acl>");
      buffer.append("\n");      

     }

//------------------------------------------------------------------------------

   private static final void addUsers (StringBuffer buffer, MCRAcl acl) {

      Set users = acl.getUsers();

      Iterator iterator = users.iterator();

      while (iterator.hasNext()) {

         MCRUser user = (MCRUser) iterator.next();
         MCRPermissionRing permissions = acl.getPermissions(user);

         addUser(buffer, user, permissions);

        }

     }

//------------------------------------------------------------------------------

   private static final void addGroups (StringBuffer buffer, MCRAcl acl) {

      Set groups = acl.getGroups();

      Iterator iterator = groups.iterator();

      while (iterator.hasNext()) {

         MCRGroup group = (MCRGroup) iterator.next();
         MCRPermissionRing permissions = acl.getPermissions(group);

         addGroup(buffer, group, permissions);

        }

     }

//------------------------------------------------------------------------------

   private static final void addUser (StringBuffer buffer, MCRUser user, MCRPermissionRing permissions) {

      buffer.append("<user id=\"" + user.getID() + "\">");
      buffer.append("\n");
      addPermissionRing(buffer, permissions);
      buffer.append("</user>");
      buffer.append("\n");

     }

//------------------------------------------------------------------------------

   private static final void addGroup (StringBuffer buffer, MCRGroup group, MCRPermissionRing permissions) {

      buffer.append("<group id=\"" + group.getID() + "\">");
      buffer.append("\n");
      addPermissionRing(buffer, permissions);
      buffer.append("</group>");
      buffer.append("\n");

     }

//------------------------------------------------------------------------------

   private static final void addPermissionRing(StringBuffer buffer, MCRPermissionRing permissionRing) {

      Iterator iterator = permissionRing.iterator();  

      while (iterator.hasNext() ) {

         Object object = iterator.next();

         if (object instanceof MCRPermission) {

            MCRPermission permission = (MCRPermission) object;
         
            if (permissionRing.containsPermission(permission)) {

               String name = permission.getName();
               boolean status = permissionRing.isPermissionGranted(permission);

               addPermission(buffer, name, status);

              }

           }

        }

     }

//------------------------------------------------------------------------------

   private final static void addPermission (StringBuffer buffer, String name, boolean status) {

      if (name == null || "".equals(name))
         return;

      buffer.append("<permission " + 
                    "name=\"" + name + "\" " +
                    "status=\"" + (status?"granted":"denied") + "\"" + 
                    "/>\n");      
      
     }

//-============================================================================\
}