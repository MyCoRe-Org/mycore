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

package org.mycore.user;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This is the abstract super class of MCRUser and MCRGroup
 *
 * @see org.mycore.user.MCRUser
 * @see org.mycore.user.MCRGroup
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
abstract class MCRUserObject
{
  protected static Logger logger = Logger.getLogger(MCRUserObject.class.getName());
  protected static MCRConfiguration config = null;

  /** The maximum length of user and group names */
  public static final int id_len = 20;

  /** The maximum length of the password */
  public static final int password_len = 20;

  /** The maximum length of the decription */
  public static final int description_len = 200;

  /** The maximum length of the privilege */
  public static final int privilege_len = 100;

  /** The ID of the MyCoRe user unit (either user ID or group ID) */
  protected String ID = "";

  /** Specifies the user responsible for the creation of this user unit */
  protected String creator = "";

  /** The date of creation of the user object in the MyCoRe system */
  protected Timestamp creationDate = null;

  /** The date of the last modification of this user object */
  protected Timestamp modifiedDate = null;

  /** Description of the user unit */
  protected String description = "";

  /** A list of groups (IDs) where this object is a member of */
  protected ArrayList groupIDs = null;

  /**
   * The constructor for an empty object. Only the logger was set.
   **/
  public MCRUserObject()
  {
    ID = "";
    creator = "";
    setCreationDate();
    setModifiedDate();
    description = "";
    groupIDs = new ArrayList();
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
  }

  /**
   * This method sets the creationDate to the time the method is called.
   **/
  public final void setCreationDate()
  { creationDate = new Timestamp(new GregorianCalendar().getTime().getTime()); }

  /**
   * This method sets the modifiedDate to the time the method is called.
   **/
  public final void setModifiedDate()
  { modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime()); }

  /**
   * This method sets the creator value.
   *
   * @param creator the creator of a user or group
   **/
  public final void setCreator(String creator)
  { this.creator = trim(creator); }

  /**
   * @return
   *   This method returns the creation date (timestamp) of the user object.
   */
  public final Timestamp getCreationDate()
  { return creationDate; }

  /**
   * @return
   *   This method returns the time of the last modifications (timestamp) of the user object.
   */
  public final Timestamp getModifiedDate()
  { return modifiedDate; }

  /**
   * @return
   *   This method returns the user ID of the creator of this user object.
   */
  public final String getCreator()
  { return creator; }

  /**
   * @return
   *   This method returns the description of the user object.
   */
  public final String getDescription()
  { return description; }

  /**
   * @return
   *   This method returns the list of groups as a ArrayList of strings. These are the
   *   groups where the object itself is a member of.
   */
  public final ArrayList getGroupIDs()
  { return groupIDs; }

  /**
   * This method adds a group to the groups list of the group. This is the list of
   * group IDs where this group is a member of, not the list of groups this group
   * has as members!
   *
   * @param groupID   ID of the group added to the group
   */
  public void addGroupID(String groupID) throws MCRException
  { if (!groupIDs.contains(groupID)) { groupIDs.add(groupID); } }

  /**
   * This method clears the ArrayList containing the administrative groups.
   */
  protected final void cleanGroupID()
  { groupIDs.clear(); }

  /**
   * This method removes a group from the groups list of the group. These are the
   * groups where the group itself is a member of.
   *
   * @param groupID   ID of the group removed from the group
   */
  public void removeGroupID(String groupID) throws MCRException
  { if (groupIDs.contains(groupID))  { groupIDs.remove(groupID); } }

  /**
   * This method must be implemented by a subclass and then returns the user or group
   * object as a JDOM document.
   */
  abstract public org.jdom.Document toJDOMDocument() throws MCRException;

  /**
   * This method must be implemented by a subclass and then returns the user or group
   * object as a JDOM element.
   */
  abstract public org.jdom.Element toJDOMElement() throws MCRException;

  /**
   * This helper method replaces null with an empty string and trims whitespace from
   * non-null strings.
   */
  protected final static String trim(String s)
  { return (s != null) ? s.trim() : ""; }

  /**
   * This helper method replaces null with an empty string and trims whitespace
   * from non-null strings.
   **/
  protected final static String trim(String s, int len)
  {
    String sn = (s != null) ? s.trim() : "";
    if (sn.length()>len) {
      logger.warn("The string \'"+sn+"\' is too long (max. "+Integer.toString(len)+").");
      return sn.substring(0,len);
    }
    else {
      return sn; }
  }

  /**
   * This method sends debug data to the logger (for the debug mode).
   **/
  public final void debugDefault()
  {
    logger.debug("ID                 = "+ID);
    logger.debug("creator            = "+creator);
    logger.debug("creationDate       = "+String.valueOf(creationDate));
    logger.debug("modifiedDate       = "+String.valueOf(modifiedDate));
    logger.debug("description        = "+description);
    logger.debug("groupIDs #         = "+groupIDs.size());
    for (int i = 0; i<groupIDs.size(); i++) {
      logger.debug("groupIDs           = "+((String)groupIDs.get(i))); }
  }

  /**
   * This method is only used for providing error messages in the
   * access control component and should be removed later.
   */
  public String toString()
  { return ID; }

}