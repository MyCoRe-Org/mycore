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

package org.mycore.common;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
import org.mycore.user.*;

/**
 * Instances of this class collect information kept during a session like the currently
 * active user, the preferred language etc.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSession implements Cloneable
{
  /** the logger */
  private static Logger logger = Logger.getLogger(MCRSession.class.getName());

  /** The user ID of the session */
  private String userID = null;

  /** The language for this session */
  private String language = null;

  /**
   * The constructor of a MCRSession. As default the user ID is set to the value
   * of the property variable named 'MCR.users_guestuser_username'.
   **/
  public MCRSession()
  { reset(); }

  /**
   * Implement a deep copy of instances of this class
   * @return MCRSession deep copy
   */
  public final Object clone()
  {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {       // This should never happen
      throw new InternalError(e.toString());
    }
  }

  /** returns the current user ID */
  public final String getCurrentUserID()
  { return userID.trim(); }

  /** returns the current user object */
  public final MCRUser getCurrentUser() throws MCRException
  {
    try {
      if (userID != null)
        return MCRUserMgr.instance().retrieveUser(userID.trim());
      else return null;
    }
    catch(Exception ex) {
      throw new MCRException("Error in MCRSession!", ex);
    }
  }

  /** sets the current user ID */
  public final void setCurrentUserID(String userID)
  { this.userID = userID; }

  /** returns the current language */
  public final String getCurrentLanguage()
  { return language.trim(); }

  /** sets the current language */
  public final void setCurrentLanguage(String language)
  { this.language = language; }

  /** Write data to the logger for debugging purposes */
  public final void debug()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    logger.debug("UserID             = "+userID);
    logger.debug("language           = "+language);
  }

  /** Resets the session to the default values */
  public final void reset()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    userID = config.getString("MCR.users_guestuser_username","gast");
    language = "DE";
  }
}
