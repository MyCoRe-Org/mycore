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

package mycore.user;

import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import org.w3c.dom.Document;
import mycore.xml.MCRXMLHelper;

/**
 * This is the abstract super class of MCRUser and MCRGroup
 *
 * @see mycore.user.MCRUser
 * @see mycore.user.MCRGroup
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
abstract class MCRUserUnit
{
  /** The ID of the MyCoRe user unit (either user ID or group ID) */
  protected String ID = "";

  /** Specifies the user responsible for the creation of this user unit */
  protected String creator = "";

  /** The date of creation of the user unit object in the MyCoRe system */
  protected Date creationDate = null;

  /** The date of the last changes of this user unit */
  protected Date lastChanges = null;

  /** Description of the user unit */
  protected String description = "";

  /** Specify whether the UserManager must be notified about the creation of this object */
  protected boolean bCreateInMgr = true;

  /**
   * returns the creator user ID of the user unit object
   * @return         returns the creator user ID of the user unit object
   */
  public String getCreator()
  { return creator; }

  /**
   * This helper method returns a given date (creationDate, lastChanges) as a string
   * @param date     date to be transformed into a string
   * @return         returns a given date as a string
   */
  protected String getDateAsString(Date date)
  { return DateFormat.getDateInstance().format(date); }

  /**
   * returns the ID of the user unit (user ID or group ID)
   * @return         returns the ID of the user unit
   */
  public String getID()
  { return ID; }

  /** This method returns the user unit information as a formatted string. */
  abstract public String getFormattedInfo() throws Exception;

  /** Checks if the user unit has a specific privilege */
  abstract public boolean hasPrivilege(String privilege) throws Exception;

  /**
   * returns the user or group object as a DOM document
   * @return returns the user or group object as a DOM document
   */
  public Document toDOM() throws Exception
  { return MCRXMLHelper.parseXML(this.toXML("")); }

  /** This method returns the user unit object as an xml representation. */
  abstract public String toXML(String NL) throws Exception;

  /**
   * This helper method replaces null with an empty string and trims whitespace from
   * non-null strings.
   */
  protected static String trim(String s)
  { return (s != null) ? s.trim() : ""; }

}
