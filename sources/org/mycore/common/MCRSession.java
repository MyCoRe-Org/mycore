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
import java.util.Map;
import java.util.HashMap;
import org.mycore.datamodel.classifications.MCRClassificationBrowserData;


/**
 * Instances of this class collect information kept during a session like the currently
 * active user, the preferred language etc.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 */
public class MCRSession implements Cloneable
{
  /** A map storing arbitrary session data **/
  private Map map = new HashMap();

  /** the logger */
  static Logger logger = Logger.getLogger(MCRSession.class.getName());

  /** The user ID of the session */
  private String userID = null;

  /** The language for this session as upper case character */
  private String language = null;

  /** The unique ID of this session */
  private String sessionID = null;

  /** A cache of MCRSession objects, used for method getSession( String ) */
  private static MCRCache sessions = new MCRCache( 1000 );

  /** -ASC- für MCRClassificationBrowser Class session daten */
  public MCRClassificationBrowserData BData = null;

  /**
   * The constructor of a MCRSession. As default the user ID is set to the value
   * of the property variable named 'MCR.users_guestuser_username'.
   **/
  public MCRSession()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    userID = config.getString("MCR.users_guestuser_username","gast");
    language = config.getString("MCR.metadata_default_lang","de");
    sessionID = buildSessionID();
    sessions.put( sessionID, this );

    logger.debug( "MCRSession created " + sessionID );
  }

  /**
   * Returns the MCRSession for the given sessionID.
   **/
  public static MCRSession getSession( String sessionID )
  { return (MCRSession)( sessions.get( sessionID ) ); }

  /**
   * Constructs a unique session ID for this session, based on
   * current time and IP address of host where the code runs.
   **/
  private static synchronized String buildSessionID()
  {
    String ip = "127.0.0.1";
    try{ ip = java.net.InetAddress.getLocalHost().getHostAddress(); }
    catch( java.net.UnknownHostException ignored ){}

    java.util.StringTokenizer st = new java.util.StringTokenizer( ip, "." );

    long sum = Integer.parseInt( st.nextToken() );
    while( st.hasMoreTokens() )
      sum = ( sum << 8 ) + Integer.parseInt( st.nextToken() );

    String address = Long.toString( sum, 36 );
    address = "000000" + address;
    String prefix = address.substring( address.length() - 6 );

    long now = System.currentTimeMillis();
    String suffix = Long.toString( now, 36 );

    return prefix + "-" + suffix;
  }

  /**
   * Returns the unique ID of this session
   **/
  public String getID()
  { return sessionID; }

  /** returns the current user ID */
  public final String getCurrentUserID()
  { return userID.trim(); }

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
    PropertyConfigurator.configure(MCRConfiguration.instance().getLoggingProperties());
    logger.debug("SessionID          = "+sessionID);
    logger.debug("UserID             = "+userID);
    logger.debug("language           = "+language);
  }

  /** Stores an object under the given key within the session **/
  public Object put( Object key, Object value )
  { return map.put( key, value ); }

  /** Returns the object that was stored in the session under the given key **/
  public Object get( Object key )
  { return map.get( key ); }

  /** 
   * a cache used to store the request parameters of the last max. 10
   * http requests to static XML files in the web application against this
   * MCRSession
   **/
  public MCRCache requestParamCache = new MCRCache( 10 );
}

