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

/**
 * Instances of this class collect information kept during a session like the login
 * user currently active.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSession
{

/** the logger */
private static Logger logger = Logger.getLogger(MCRSession.class.getName());

/** The user ID of the session */
private String userID = null;

/**
 * The constructor of a MCRSession. As default the user ID was set to the value
 * of the property variable named 'MCR.users_guestuser_username'.
 **/
public MCRSession()
  {
  MCRConfiguration config = MCRConfiguration.instance();
  userID = config.getString("MCR.users_guestuser_username","gast");
  }

/** returns the current user ID */
public final String getCurrentUserID()
  { return userID; }

/** sets the current user ID */
public final void setCurrentUserID(String userID)
  { this.userID = userID; }

/** Put the data to the logger to debug this class */
public final void debug()
  {
  MCRConfiguration config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties()); 
  logger.debug("UserID             = "+userID);
  }

}

