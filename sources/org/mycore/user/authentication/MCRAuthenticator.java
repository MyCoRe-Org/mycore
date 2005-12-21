/**
 * $RCSfile$
 * $Revision$ $Date$
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
package org.mycore.user.authentication;

/**
 * Authenticates a userID by checking a given password for that userID.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public interface MCRAuthenticator
{
  /** 
   * Initializes this instance. Each MCRAuthenticator instance
   * must have a unique ID that determines the class and the configuration
   * properties of that instance. All configuration properties of this instance
   * have the common prefix MCR.UserAuthenticator.[ID].* 
   * 
   * @param ID the unique ID of this MCRAuthenticator instance
   */  
  public void init(String ID);

  /**
   * Authenticates a user by checking a given password for that user.
   * The mechanism used for checking this depends on the implementation
   * that is used, for example LDAP, IMAP, SLNP, JAAS, ...
   * 
   * @param username the username
   * @param password the password
   * @return true, if the password is correct, false otherwise
   */
  public boolean authenticate(String username, String password);
}
