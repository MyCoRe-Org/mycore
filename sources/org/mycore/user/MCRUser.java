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
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.ListIterator;
import org.jdom.Document;
import org.jdom.Element;
import mycore.common.*;

/**
 * Instances of this class represent MyCoRe users.
 *
 * @see mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
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
   * Default constructor. It is used to create a user object with empty fields. This is
   * useful for constructing an XML representation of a user without specialized data
   * which is used e.g. by MCRCreateUserServlet just to get an XML-representation. The
   * XML representation is the used by the XSLT-Stylesheet to create HTML output for
   * the servlet. This empty user object will not be created in the persistent data store.
   */
  public MCRUser() throws Exception
  {
    this(-1, "", false, false, "", null, null, "", "", "", "", "", "", "", "", "", "",
         "", "", "", "", "", "", "", "", null, "");
  }

  /**
   * This constructor takes all attributes of this class as single variables.
   *
   * @param numID            (int) the numerical user ID
   * @param ID               the named user ID
   * @param idEnabled        (boolean) specifies whether the account is disabled or enabled
   * @param updateAllowed    (boolean) specifies whether the user may update his or her data
   * @param creator          the user ID who created this user
   * @param creationDate     timestamp of the creation of this user, if null the current date will be used
   * @param modifiedDate     timestamp of the last modification of this user
   * @param description      description of the user
   * @param passwd           password of the user
   * @param salutation       how to address the user, e.g. Mr. or Prof. Dr. ...
   * @param firstname        the first name (or given name) of the user
   * @param lastname         the last name (or surname) of the user
   * @param street           contact information
   * @param city             contact information
   * @param postalcode       contact information
   * @param country          contact information
   * @param institution      contact information
   * @param faculty          contact information
   * @param department       contact information
   * @param institute        contact information
   * @param telephone        telephone number
   * @param fax              fax number
   * @param email            email address
   * @param cellphone        number of cellular phone, if available
   * @param primaryGroupID   the ID of the primary group of the user
   * @param groupIDs         a Vector of groups (IDs) the user belongs to
   * @param todoInMgr        String value which specifies whether the MCRUserMgr must be notified
   *                         about the construction of this object. Values may be "" (no action)
   *                         and "create" (create new user).
   */
  public MCRUser(int numID, String ID, boolean idEnabled, boolean updateAllowed, String creator,
                 Timestamp creationDate, Timestamp modifiedDate, String description, String passwd,
                 String salutation, String firstname, String lastname, String street, String city,
                 String postalcode, String country, String institution, String faculty,
                 String department, String institute, String telephone, String fax, String email,
                 String cellphone, String primaryGroupID, Vector groupIDs,
                 String todoInMgr) throws MCRException, Exception
  {
    // The following data will never be changed, thus it must not be passed to the following
    // update method...

    super.ID      = trim(ID);
    super.creator = trim(creator);
    this.numID    = numID;

    // check if the creation timestamp is provided. If not, use current date and time
    if (creationDate == null)
      super.creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());
    else super.creationDate = creationDate;

    // use the update method to populate the remaining attributes
    update(idEnabled, updateAllowed, description, passwd, salutation, firstname, lastname,
           street, city, postalcode, country, institution, faculty, department, institute,
           telephone, fax, email, cellphone, primaryGroupID, groupIDs, "");

    // The timestamp of the last changes has been set to the current date and time in update().
    // If this is not wanted (i.e. if the parameter lastChanges is not null), it will be reset here:

    if (modifiedDate != null)
      super.modifiedDate = modifiedDate;

    // Eventually notify the User Manager and create a new user object in the datastore,
    // but only if this object is valid, i.e. all required fields are provided

    if (todoInMgr.trim().equals("create")) {
      if (isValid())
        MCRUserMgr.instance().createUser(this);
      else throw new MCRException("User object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * @return
   *   This method returns the address object of the user
   */
  public MCRUserContact getContactInfo()
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
  public String getPrimaryGroupID()
  { return primaryGroupID; }

  /**
   * This method checks if the user has a specific privilege. To do this all groups of
   * the user are checked if the given privilege is in their list of privileges.
   *
   * @return
   *   returns true if the given privilege is in one of the list of privileges
   *   of one of the groups
   */
  public boolean hasPrivilege(String privilege) throws Exception
  {
    ListIterator groupIter = groupIDs.listIterator();
    while (groupIter.hasNext()) {
      MCRGroup currentGroup = MCRUserMgr.instance().retrieveGroup((String)groupIter.next());
      if (currentGroup.hasPrivilege(privilege))
        return true;
    }
    return false;
  }

  /**
   * @return
   *   This method returns true if the user is member of the administrators group.
   */
  public boolean isAdmin()
  { return (groupIDs.contains("administrators")) ? true : false; }

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
  public boolean isValid() throws Exception
  {
    Vector requiredUserAttributes = MCRUserPolicy.instance().getRequiredUserAttributes();
    boolean test = true;

    if (requiredUserAttributes.contains("userID"))
      test = test && super.ID.length() > 0;
    if (requiredUserAttributes.contains("numID"))
      test = test && this.numID >= 0;
    if (requiredUserAttributes.contains("password"))
      test = test && this.passwd.length() > 0;
    if (requiredUserAttributes.contains("creator"))
      test = test && super.creator.length() > 0;
    if (requiredUserAttributes.contains("description"))
      test = test && super.description.length() > 0;
    if (requiredUserAttributes.contains("salutation"))
      test = test && userContact.getSalutation().length() > 0;
    if (requiredUserAttributes.contains("firstname"))
      test = test && userContact.getFirstName().length() > 0;
    if (requiredUserAttributes.contains("lastname"))
      test = test && userContact.getLastName().length() > 0;
    if (requiredUserAttributes.contains("street"))
      test = test && userContact.getStreet().length() > 0;
    if (requiredUserAttributes.contains("city"))
      test = test && userContact.getCity().length() > 0;
    if (requiredUserAttributes.contains("postalcode"))
      test = test && userContact.getPostalCode().length() > 0;
    if (requiredUserAttributes.contains("country"))
      test = test && userContact.getCountry().length() > 0;
    if (requiredUserAttributes.contains("institution"))
      test = test && userContact.getInstitution().length() > 0;
    if (requiredUserAttributes.contains("faculty"))
      test = test && userContact.getFaculty().length() > 0;
    if (requiredUserAttributes.contains("department"))
      test = test && userContact.getDepartment().length() > 0;
    if (requiredUserAttributes.contains("institute"))
      test = test && userContact.getInstitute().length() > 0;
    if (requiredUserAttributes.contains("telephone"))
      test = test && userContact.getTelephone().length() > 0;
    if (requiredUserAttributes.contains("fax"))
      test = test && userContact.getFax().length() > 0;
    if (requiredUserAttributes.contains("email"))
      test = test && userContact.getEmail().length() > 0;
    if (requiredUserAttributes.contains("cellphone"))
      test = test && userContact.getCellphone().length() > 0;
    if (requiredUserAttributes.contains("primary_group"))
      test = test && this.primaryGroupID.length() >0;

    return test;
  }

  /**
   * This method sets the password of the user.
   *
   * @param newPassword   The new password of the user
   */
  public boolean setPassword(String newPassword) throws Exception
  {
    // We do not allow empty passwords. Later we might check if the password is
    // conform with a password policy.

    if (newPassword.length() != 0) {
      passwd = newPassword;
      super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());
      MCRUserMgr.instance().updateUser(this);
      return true;
    }
    else return false;
  }

  /**
   * Updates some of the attributes of the user. Some attributes (ID, creator, creationDate)
   * cannot be updated. The date of the last changes will be updated automatically in this
   * method.
   *
   * @param idEnabled        (boolean) specifies whether the account is disabled or enabled
   * @param updateAllowed    (boolean) specifies whether the user may update his or her data
   * @param description      description of the user
   * @param passwd           password of the user
   * @param salutation       how to address the user, e.g. Mr. or Prof. Dr. ...
   * @param firstname        the first name (or given name) of the user
   * @param lastname         the last name (or surname) of the user
   * @param street           contact information
   * @param city             contact information
   * @param postalcode       contact information
   * @param country          contact information
   * @param institution      contact information
   * @param faculty          contact information
   * @param department       contact information
   * @param institute        contact information
   * @param telephone        telephone number
   * @param fax              fax number
   * @param email            email address
   * @param cellphone        number of cellular phone, if available
   * @param primaryGroupID   the ID of the primary group of the user
   * @param groupIDs         a Vector of groups (IDs) the user belongs to
   * @param todoInMgr        String value which specifies whether the MCRUserMgr must be notified.
   */
  public void update(boolean idEnabled, boolean updateAllowed, String description, String passwd,
                     String salutation, String firstname, String lastname, String street, String city,
                     String postalcode, String country, String institution, String faculty,
                     String department, String institute, String telephone, String fax, String email,
                     String cellphone, String primaryGroupID, Vector groupIDs,
                     String todoInMgr) throws MCRException, Exception
  {
    super.description = trim(description);
    this.idEnabled = idEnabled;
    this.updateAllowed = updateAllowed;
    this.passwd = trim(passwd);
    this.primaryGroupID = trim(primaryGroupID);

    // the groupID vector might be null due to the default constructor
    if (groupIDs != null)
      super.groupIDs = groupIDs;
    else super.groupIDs = new Vector();

    // check if the primary group is in the groups list. If not, put it into the list.
    if ((primaryGroupID.length() > 0) && (!super.groupIDs.contains(primaryGroupID)))
      super.groupIDs.add(primaryGroupID);

    // update the date of the last changes
    super.modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());

    // create the MCRuserContact object
    userContact = new MCRUserContact(salutation, firstname, lastname, street, city, postalcode,
                                     country, institution, faculty, department, institute,
                                     telephone, fax, email, cellphone);

    // Eventually notify the User Manager and update the user object in the datastore,
    // but only if this object is valid.
    if (todoInMgr.trim().equals("update")) {
      if (isValid())
        MCRUserMgr.instance().updateUser(this);
      else throw new MCRException("User object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * @return
   *   This method returns the user or group object as a JDOM document.
   */
  public Document toJDOMDocument() throws Exception
  {
    Element root = new Element("mcr_userobject");
    root.setAttribute("type", "user");
    root.addContent(this.toJDOMElement());
    Document jdomDoc = new Document(root);
    return jdomDoc;
  }

  /**
   * @return
   *   This method returns the user or group object as a JDOM element. This is needed if
   *   one wants to get a representation of several user objects in one xml document.
   */
  public Element toJDOMElement() throws Exception
  {
    Element user = new Element("user")
      .setAttribute("numID", Integer.toString(numID))
      .setAttribute("ID", ID)
      .setAttribute("id_enabled", (idEnabled) ? "true" : "false")
      .setAttribute("update_allowed", (updateAllowed) ? "true" : "false");

    Element Passwd       = new Element("user.password").setText(passwd);
    Element Creator      = new Element("user.creator").setText(super.creator);
    Element CreationDate = new Element("user.creation_date").setText(super.creationDate.toString());
    Element ModifiedDate = new Element("user.last_modified").setText(super.modifiedDate.toString());
    Element Description  = new Element("user.description").setText(super.description);
    Element Primarygroup = new Element("user.primary_group").setText(primaryGroupID);
    Element Groups       = new Element("user.groups");


    // Loop over all group IDs
    for (int i=0; i<groupIDs.size(); i++) {
      Element groupID = new Element("groups.groupID").setText((String)groupIDs.elementAt(i));
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
}
