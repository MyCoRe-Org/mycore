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
 * This interface defines a permission.
 *
 * <P>
 * A permission is simply a name which corresponds to
 * a method call, a method call with specific arguments or 
 * a group of method calls on a guarded object, e.g
 * the permission "set" could stand for the right to call
 * <code>setXXX</code>-methods with arbitrary arguments for a given class.
 *
 * <P>
 * Permission objects should be immutable, so that they can be shared.
 * In general permissions are defined as static constants in a class.
 * For a given application there should exist exactly one instance 
 * of a permission for a class.
 *
 * <P>
 * In addition to the name a permission may contain a description, 
 * which can be used in help texts and ACL editors.
 * 
 * @author   Benno Süselbeck
 * @version  1.0.0, 01.11.2003
 **/


public interface MCRPermission {
///============================================================================/

  /**
   * Returns the name of the permission.
   *
   * @return string representing the name of the permission.
   */

  public String getName ();

   
//-----------------------------------------------------------------------------

  /**
   * Returns a description of the permission.
   *
   * @return string representing the description of the permission.
   */

   public String getDescription ();

//-============================================================================\
}