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

import org.mycore.user.MCRUser;
import org.mycore.user.MCRGroup;
///============================================================================|

/**
 * This interface characterizes objects
 * which have an owner and an owning group.
 *
 * @author   Benno Süselbeck
 * @version  1.0.0, 01.11.2003
 **/

public interface MCROwnedObject {
///============================================================================/

  /**
   * Returns the owner of this guarded object.
   *
   * @return the owner of this object.   
   */
   
   public MCRUser getOwner ();

//------------------------------------------------------------------------------

  /**
   * Sets a new owner for this guarded object.
   *
   * @throws  MCRAccessException  if the currentUser is not allowed to set a new owner.
   */
   
   public void changeOwner (MCRUser newOwner)
                           throws MCRAccessException;
                           
//------------------------------------------------------------------------------

  /**
   * Returns the owner of this guarded object.
   *
   * @return the owner of this object.
   */
   
   public MCRGroup getOwnerGroup ();
   
  
//------------------------------------------------------------------------------

  /**
   * Sets a new owner group for this guarded object.
   *
   * @throws  MCRAccessException  if the currentUser is not allowed to set a new owner.  
   */
   
   public void changeOwnerGroup (MCRGroup newOwnerGroup)
                                throws MCRAccessException;
   

//-============================================================================\
}