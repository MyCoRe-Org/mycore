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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import org.mycore.common.*;

/**
 * Instances of this class represent MyCoRe users.
 *
 * @see org.mycore.user.MCRUserMgr
 *
 * @see org.mycore.user.MCRUserObject
 * @see org.mycore.user.MCRUserContact
 * @see org.mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUser extends MCRUserObject
{
/** The numerical ID of the MyCoRe user unit (either user ID or group ID) */
private int numID = -1;

/** Specify whether the user ID is enabled or disabled */
private boolean idEnabled = false;

/** Specify whether the user is allowed to update the user object */
private boolean updateAllowed = false;

/** The password of the MyCoRe user */
private String passwd = "";

/** The primary group ID of the user */
private String primaryGroupID = "";

/** Object representing user address information */
private MCRUserContact userContact;

/**
 * Default constructor. It is used to create a user object with empty fields. 
 * This is useful for constructing an XML representation of a user without 
 * specialized data which is used e.g. by MCRCreateUserServlet just to get an 
 * XML-representation. The XML representation is the used by the 
 * XSLT-Stylesheet to create HTML output for the servlet. This empty user 
 * object will not be created in the persistent data store.
 */
public MCRUser()
  {
  super();
  numID = -1;
  idEnabled = false;
  updateAllowed = false;
  passwd = "";
  primaryGroupID = "";
  userContact = new MCRUserContact();
  }

/**
 * This constructor takes all attributes of this class as single variables.
 *
 * @param numID            (int) the numerical user ID
 * @param ID               the named user ID
 * @creator                the creator name
 * @creationDate           the timestamp of creation
 * @modifiedDate           the timestamp of modification
 * @param idEnabled        (boolean) specifies whether the account is disabled or enabled
 * @param updateAllowed    (boolean) specifies whether the user may update his or her data
 * @param description      description of the user
 * @param passwd           password of the user
 * @param primaryGroupID   the ID of the primary group of the user
 * @param groupIDs         a ArrayList of groups (IDs) the user belongs to
 * @param salutation       contact information
 * @param firstname        contact information
 * @param lastname         contact information
 * @param street           contact information
 * @param city             contact information
 * @param postalcode       contact information
 * @param country          contact information
 * @param state            contact information
 * @param institution      contact information
 * @param faculty          contact information
 * @param department       contact information
 * @param institute        contact information
 * @param telephone        telephone number
 * @param fax              fax number
 * @param email            email address
 * @param cellphone        number of cellular phone, if available
 */
public MCRUser(int numID, String ID, String creator, Timestamp creationDate,
  Timestamp modifiedDate, boolean idEnabled, boolean updateAllowed, 
  String description, String passwd, String primaryGroupID, ArrayList groupIDs,
  String salutation, String firstname, String lastname, String street, 
  String city, String postalcode, String country, String state, 
  String institution, String faculty, String department, String institute, 
  String telephone, String fax, String email, String cellphone) 
  throws MCRException, Exception
  {
  // The following data will never be changed by update
  super.ID      = trim(ID,id_len);
  this.numID    = numID;
  super.creator = trim(creator,id_len);
  // check if the creation timestamp is provided. If not, use current timestamp
  if (creationDate == null) {
    super.creationDate = new Timestamp(new GregorianCalendar().getTime()
      .getTime()); }
  else {
    super.creationDate = creationDate; }
  if (modifiedDate == null) {
    super.modifiedDate = new Timestamp(new GregorianCalendar().getTime()
      .getTime()); }
  else {
    super.modifiedDate = modifiedDate; }
  this.idEnabled = idEnabled;
  this.updateAllowed = updateAllowed;
  super.description = trim(description,description_len);
  this.passwd = trim(passwd,password_len);
  this.primaryGroupID = trim(primaryGroupID,id_len);
  super.groupIDs = groupIDs;
  userContact = new MCRUserContact(salutation,firstname,lastname,
    street, city,postalcode,country,state,institution,faculty,department,
    institute,telephone,fax,email,cellphone);
  }

/**
 * This constructor create the data of this class from an JDOM Element.
 *
 * @param the JDOM Element
 **/
public MCRUser(org.jdom.Element elm)
  {
  this();
  if (!elm.getName().equals("user")) { return; }
  super.ID = trim((String)elm.getAttributeValue("ID"),id_len);
  String numIDtmp = trim((String)elm.getAttributeValue("numID"));
  try {
    this.numID = Integer.parseInt(numIDtmp); }
  catch (Exception e) {
    this.numID = -1; }
  this.idEnabled = 
    (elm.getAttributeValue("id_enabled").equals("true")) ? true : false;
  this.updateAllowed = 
    (elm.getAttributeValue("update_allowed").equals("true")) ? true : false;
  this.creator = trim(elm.getChildTextTrim("user.creator"),id_len);
  this.passwd = trim(elm.getChildTextTrim("user.password"),password_len);
  String tmp = elm.getChildTextTrim("user.creation_date");
  if (tmp != null) {
    try {
      super.creationDate = Timestamp.valueOf(tmp); }
    catch (Exception e) { }
    }
  tmp = elm.getChildTextTrim("user.last_modified");
  if (tmp != null) {
    try {
      super.modifiedDate = Timestamp.valueOf(tmp); }
    catch (Exception e) { }
    }
  this.description = trim(elm.getChildTextTrim("user.description"),
    description_len);
  this.primaryGroupID = trim(elm.getChildTextTrim("user.primary_group"),
    id_len);
  org.jdom.Element userContactElement = elm.getChild("user.contact");
  org.jdom.Element userGroupElement = elm.getChild("user.groups");
  if (userGroupElement != null) {
    List groupIDList = userGroupElement.getChildren();
    for (int j=0; j<groupIDList.size(); j++) {
      org.jdom.Element groupID = (org.jdom.Element)groupIDList.get(j);
      if (!((String)groupID.getTextTrim()).equals("")) {
        groupIDs.add((String)groupID.getTextTrim()); }
      }
    }
  }

/**
 * @return
 *   This method returns the address object of the user
 */
public MCRUserContact getUserContact()
  { return userContact; }

/**
 * @return  This method returns the numerical ID of the user object.
 */
public int getNumID()
  { return numID; }

  /**
   * @return
   *   This method returns the password of the user.
   */
  public String getPassword()
  { return passwd; }

  /**
   * @return
   *   This method returns the ID of the primary group of the user.
   */
  public final String getPrimaryGroupID()
  { return primaryGroupID; }

  /**
   * This method returns a normalized ArrayList of all privileges of this user.
   * 
   * @return a privilege list of this user
   **/
  public final ArrayList getPrivileges()
    {
    ArrayList ar = new ArrayList();
    try {
      // Privileges of the primary group
      MCRGroup g = MCRUserMgr.instance().retrieveGroup(primaryGroupID);
      ar.addAll(g.getAllPrivileges());
      for (int i=0;i<groupIDs.size();i++) {
        g = MCRUserMgr.instance().retrieveGroup((String)groupIDs.get(i));
        ar.addAll(g.getAllPrivileges());
        }
      }
    catch(MCRException ex) {}
    ArrayList n = new ArrayList();
    for (int i=0;i<ar.size();i++) {
      boolean test = false;
      String name = ((String)ar.get(i));
      for (int j=0;j<n.size();j++) {
        if (name.equals((String)n.get(j))) { test = true; }
        }
      if (!test) { n.add(ar.get(i)); }
      }
    for (int i=0;i<n.size();i++) { logger.debug("Privileg = "+(String)ar.get(i)); }
    return n;
    }

  /**
   * This method checks if the user has a specific privilege. To do this all groups of
   * the user are checked if the given privilege is in their list of privileges.
   *
   * @return
   *   returns true if the given privilege is in one of the list of privileges
   *   of one of the groups
   */
  public boolean hasPrivilege(String privilege) 
    { return getPrivileges().contains(privilege); }

  /**
   * @return
   *   This method returns true if the user is enabled and may login.
   */
  public boolean isEnabled()
  { return idEnabled; }

  /**
   * @return
   *   This method returns true if the user may update his or her data.
   */
  public boolean isUpdateAllowed()
  { return updateAllowed; }

  /**
   * This method checks if all required fields have been provided. In a later
   * stage of the software development a User Policy object will be asked, which
   * fields exactly are the required fields. This will be configurable.
   *
   * @return
   *   returns true if all required fields have been provided
   */
  public boolean isValid() 
  {
    ArrayList requiredUserAttributes = 
      MCRUserPolicy.instance().getRequiredUserAttributes();
    boolean test = true;

    if (requiredUserAttributes.contains("userID"))
      test = test && super.ID.length() > 0;
    if (requiredUserAttributes.contains("numID"))
      test = test && this.numID >= 0;
    if (requiredUserAttributes.contains("creator"))
      test = test && super.ID.length() > 0;
    if (requiredUserAttributes.contains("password"))
      test = test && this.passwd.length() > 0;
    if (requiredUserAttributes.contains("primary_group"))
      test = test && this.primaryGroupID.length() >0;

    return test;
  }

/**
 * This method sets the password of the user.
 *
 * @param newPassword   The new password of the user
 */
public boolean setPassword(String newPassword)
  {
  // We do not allow empty passwords. Later we might check if the password is
  // conform with a password policy.
  if (newPassword == null) { return false; }
  if (newPassword.length() != 0) {
    passwd = trim(newPassword,password_len);
    super.modifiedDate = new Timestamp(new GregorianCalendar().getTime()
      .getTime());
    return true;
    }
  return false;
  }

/**
 * This method set the enabled value with a boolean data.
 *
 * @param flag the boolean data
 **/
public final void setEnabled(boolean flag)
  { idEnabled = flag; }

/**
 * This method update this instance with the data of the given MCRUser.
 *
 * @param newuser the data for the update.
 **/
public final void update(MCRUser newuser)
  {
  if (!updateAllowed) return;
  idEnabled = newuser.isEnabled();
  passwd = newuser.getPassword();
  primaryGroupID = newuser.getPrimaryGroupID();
  description = newuser.getDescription();
  groupIDs = newuser.getGroupIDs();
  userContact = newuser.getUserContact();
  }

/**
 * @return
 *   This method returns the user or group object as a JDOM document.
 */
public org.jdom.Document toJDOMDocument() throws MCRException
  {
  // Build the DOM
  org.jdom.Element root = new org.jdom.Element("mycoreuser");
  root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
    MCRDefaults.XLINK_URL));
  root.setAttribute("noNamespaceSchemaLocation","MCRUser.xsd",org.jdom.Namespace.getNamespace("xsi",
    MCRDefaults.XSI_URL));
  root.addContent(this.toJDOMElement());
  org.jdom.Document jdomDoc = new org.jdom.Document(root);
  return jdomDoc;
  }

/**
 * This method returns the user object as a JDOM element. This is needed if
 * one wants to get a representation of several user objects in one xml 
 * document.
 *
 * @return this user data as JDOM element
 */
public org.jdom.Element toJDOMElement() throws MCRException
  {
  org.jdom.Element user = new org.jdom.Element("user")
    .setAttribute("numID", Integer.toString(numID))
    .setAttribute("ID", ID)
    .setAttribute("id_enabled", (idEnabled) ? "true" : "false")
    .setAttribute("update_allowed", (updateAllowed) ? "true" : "false");
  org.jdom.Element Passwd = 
    new org.jdom.Element("user.password").setText(passwd);
  org.jdom.Element Creator = 
    new org.jdom.Element("user.creator").setText(super.creator);
  org.jdom.Element CreationDate = 
    new org.jdom.Element("user.creation_date")
    .setText(super.creationDate.toString());
  org.jdom.Element ModifiedDate = 
    new org.jdom.Element("user.last_modified")
    .setText(super.modifiedDate.toString());
  org.jdom.Element Description  = 
    new org.jdom.Element("user.description")
    .setText(super.description);
  org.jdom.Element Primarygroup = 
    new org.jdom.Element("user.primary_group")
    .setText(primaryGroupID);
  // Loop over all group IDs
  org.jdom.Element Groups = new org.jdom.Element("user.groups");
  for (int i=0; i<groupIDs.size(); i++) {
    org.jdom.Element groupID = new org.jdom.Element("groups.groupID")
      .setText((String)groupIDs.get(i));
    Groups.addContent(groupID);
    }
  // Aggregate user element
  user.addContent(Passwd)
      .addContent(Creator)
      .addContent(CreationDate)
      .addContent(ModifiedDate)
      .addContent(Description)
      .addContent(Primarygroup)
      .addContent(userContact.toJDOMElement())
      .addContent(Groups);
  return user;
  }

/**
 * This method put debug data to the logger (for the debug mode).
 **/
public final void debug()
  {
  debugDefault();
  logger.debug("primaryGroupID     = "+primaryGroupID); 
  userContact.debug();
  }
}
