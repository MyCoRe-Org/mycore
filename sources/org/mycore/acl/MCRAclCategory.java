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

import org.mycore.user.MCRPrincipal;

import java.util.Set;
///============================================================================|

/**
 * Enumeration for certain types and categories of principals
 * used as entries in ACLs.
 *
 * This class should be reimplemented when true enumerations are available in Java.
 * 
 * @author   Benno Süselbeck
 * @version  1.0.0, 01.11.2003
 **/

public class MCRAclCategory implements MCRPrincipal {
///============================================================================/

  /**
   * IDs for generic principal. 
   * These ids must not be used for other principals.
   */
   
   public static final int OWNER_ID        = 0;
   public static final int OWNER_GROUP_ID  = 1;
   public static final int OTHER_ID        = 2;
   public static final int ANY_OTHER_ID    = 3;
   public static final int USERS_ID        = 4;
   public static final int GROUPS_ID       = 5;

//+----------------------------------------------------------------------------

  /**
   * Names for the principal.
   */
   
   public static final String OWNER_NAME        = "OWNER";
   public static final String OWNER_GROUP_NAME  = "OWNER_GROUP";
   public static final String OTHER_NAME        = "OTHER";
   public static final String ANY_OTHER_NAME    = "ANY_OTHER";
   public static final String USERS_NAME        = "USERS";
   public static final String GROUPS_NAME       = "GROUPS";

//+-----------------------------------------------------------------------------

  /**
   * The principal representing owners of an object.
   */
   
   public static final MCRAclCategory OWNER = new MCRAclCategory(OWNER_ID, OWNER_NAME);
   
   
  /**
   * The principal representing owner groups of an object.
   */
   
   public static final MCRAclCategory OWNER_GROUP = new MCRAclCategory(OWNER_GROUP_ID, OWNER_GROUP_NAME);
   
   
   /**
    * The principal representing other principals (not owner and not member of owner group).
    */
   
   public static final MCRAclCategory OTHER = new MCRAclCategory(OTHER_ID, OTHER_NAME);
   
   
   /**
    * The principal representing the category "any other" (unknown user, guest).
    */
   
   public static final MCRAclCategory ANY_OTHER = new MCRAclCategory(ANY_OTHER_ID, ANY_OTHER_NAME);
   
   
   /**
    * The principal representing the category of all individual users.
    */
   
   public static final MCRAclCategory USERS = new MCRAclCategory(USERS_ID, USERS_NAME);
   
   
   /**
    * The principal representing the category of all individual groups.
    */
   
   public static final MCRAclCategory GROUPS = new MCRAclCategory(GROUPS_ID, GROUPS_NAME);
                                                
//+------------------------------------------------------------------------------

   private String name;
   private int id;

//+-----------------------------------------------------------------------------

  /**
   * Constructor is private so that no instances other than
   * the ones in the enumeration can be created.
   *
   * @see #readResolve
   */

   private MCRAclCategory (int id, String name) {
   
      this.name = name;
      this.id = id;
        
     }
     
//>-----------------------------------------------------------------------------

  /**
   * @return the name of the category
   */
  
   public String getID () {
  
      return name;
        
     }
    
//------------------------------------------------------------------------------

  /**
   * @return string representation of principal
   */
  
   public String toString () {
  
      return getID();
        
     }
    
//------------------------------------------------------------------------------

  /**
   * The numerical id of the principal.
   *
   * Can be used in switching
   * i.e. switch(aclPrincipal.getNumID()){
   *       case MCRAclPrincipal.OWNER_ID:
   *        ....
   *       case MCRAclPrincipal.ANY_OTHER_ID:
   *
   * @return the id of the principal
   */
   
   public int getNumID () {
   
      return id;
      
     }
    
//------------------------------------------------------------------------------

  /**
   * This method is called by the serialization code before it returns an unserialized
   * object. To provide for unicity of instances, the instance that was read
   * is replaced by its static equivalent
   *
   * @return Object instance after unserializing
   */
   
   public Object readResolve () {
   
        switch (id) {
        
        case OWNER_ID:       return MCRAclCategory.OWNER;
        case OWNER_GROUP_ID: return MCRAclCategory.OWNER_GROUP;
        case OTHER_ID:       return MCRAclCategory.OTHER;
        case ANY_OTHER_ID:   return MCRAclCategory.ANY_OTHER;
        default:             throw new Error("Unknown MCRAclPrincipal value");
        
       }
       
    }
    
//-=============================================================================
}