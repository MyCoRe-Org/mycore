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

package org.mycore.frontend.cli;

import java.io.*;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.*;
import org.mycore.user.*;

/**
 * This class provides a set of commands for the org.mycore.user management which
 * can be used by the command line interface.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRUserCommands
{
  private static Logger logger =
    Logger.getLogger(MCRUserCommands.class.getName());

 /**
  * Initialize common data.
  **/
  private static void init()
    {
    MCRConfiguration config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    }

  /**
   * This method checks the data consistency of the user management and should be
   * called after a system crash or after importing data from files, respectively.
   */
  public static void checkConsistency() throws Exception
  { MCRUserMgr.instance().checkConsistency(); }

  /**
   * This method creates user, group or privilege data from a file or from files in a
   * directory. In contrast to the "import" mode (see importFromFile()) the group
   * information is taken into account when creating users, i.e. the corresponding
   * groups will be notified that they have a new user.
   *
   * @param filename
   *   name of a file or directory containing user, group or privilege data in XML files
   */
  public static void createFromFile(String filename) throws Exception
  { loadFromFile(filename, "create"); }

  /**
   * This method invokes MCRUserMgr.deleteGroup() and permanently removes
   * a group from the system.
   *
   * @param groupID the ID of the group which will be deleted
   */
  public static void deleteGroup(String groupID) throws Exception
  {
    init();
    MCRUserMgr.instance().deleteGroup(groupID);
    logger.info("Group ID " + groupID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.deleteUser() and permanently removes
   * a user from the system.
   *
   * @param userID the ID of the user which will be deleted
   */
  public static void deleteUser(String userID) throws Exception
  {
    init();
    MCRUserMgr.instance().deleteUser(userID);
    logger.info("User ID " + userID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a vector
   * of all users stored in the persistent datastore.
   */
  public static void listAllUsers() throws Exception
  {
    init();
    Vector users = new Vector(MCRUserMgr.instance().getAllUserIDs());
    logger.info("");
    for (int i=0; i<users.size(); i++)
      logger.info(users.elementAt(i));
  }

  /**
   * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a vector
   * of all groups stored in the persistent datastore.
   */
  public static void listAllGroups() throws Exception
  {
    init();
    Vector groups = new Vector(MCRUserMgr.instance().getAllGroupIDs());
    logger.info("");
    for (int i=0; i<groups.size(); i++)
      logger.info(groups.elementAt(i));
  }

  /**
   * This method invokes MCRPrivilegeSet.getPrivileges() and retrieves a vector
   * of all privileges stored in the persistent datastore.
   */
  public static void listAllPrivileges() throws Exception
  {
    init();
    Vector privs = new Vector(MCRPrivilegeSet.instance().getPrivileges());
    logger.info("");
    for (int i=0; i<privs.size(); i++) {
      MCRPrivilege currentPriv = (MCRPrivilege)privs.elementAt(i);
      logger.info(currentPriv.getName());
      logger.info("    "+currentPriv.getDescription());
    }
  }

  /**
   * Import data from a file or directory. Importing data from files is different from creating
   * user or group objects from data files (see createFromFile()): If a user or group is "imported"
   * the group information in the xml representation is not taken into account (since these data are
   * redundant). The import mode is used when a recreation of a whole set of user management
   * information is necessary, e.g. for a migration of data to another system.
   *
   * @param filename
   *   name of a file or directory containing user, group or privilege data in XML files
   */
  public static void importFromFile(String filename) throws Exception
  { loadFromFile(filename, "import"); }

  /**
   * This command takes a file name as a parameter, retrieves all groups from MCRUserMgr as JDOM
   * document and saves this to the given file.
   *
   * @param filename Name of the file the groups will be saved to
   */
  public static void saveAllGroupsToFile(String filename) throws Exception
  {
    Document jdomDoc = MCRUserMgr.instance().getAllGroups();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
  }

  /**
   * This command takes a file name as a parameter, retrieves all privileges from MCRPrivilegeSet
   * as JDOM document and saves this to the given file.
   *
   * @param filename Name of the file the privileges will be saved to
   */
  public static void saveAllPrivilegesToFile(String filename) throws Exception
  {
    Document jdomDoc = MCRPrivilegeSet.instance().toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
  }

  /**
   * This command takes a file name as a parameter, retrieves all users from MCRUserMgr as JDOM
   * document and saves this to the given file.
   *
   * @param filename Name of the file the users will be saved to
   */
  public static void saveAllUsersToFile(String filename) throws Exception
  {
    Document jdomDoc = MCRUserMgr.instance().getAllUsers();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
  }

  /**
   * This command takes a groupID and file name as a parameter, retrieves the group from
   * MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param groupID  ID of the group to be saved
   * @param filename Name of the file the groups will be saved to
   */
  public static void saveGroupToFile(String groupID, String filename) throws Exception
  {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
    Document jdomDoc = group.toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
  }

  /**
   * This command takes a userID and file name as a parameter, retrieves the user from
   * MCRUserMgr as JDOM document and saves this to the given file.
   *
   * @param userID   ID of the user to be saved
   * @param filename Name of the file the user will be saved to
   */
  public static void saveUserToFile(String userID, String filename) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    Document jdomDoc = user.toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
  }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to change the password.
   *
   * @param userID the ID of the user for which the password will be set
   */
  public static void setPassword(String userID, String password) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
    user.setPassword(password);
  }

  /** This method sets the user management component to read only mode */
  public static void setLock() throws Exception
  {
    init();
    MCRUserMgr.instance().setLock(true);
    logger.info("Write access to the user component persistent database now is denied.");
  }

  /**
   * This method invokes MCRUserMgr.retrieveGroup() and then works with the
   * retrieved group object to get an XML-Representation.
   *
   * @param groupID the ID of the group for which the XML-representation is needed
   */
  public static void showGroup(String groupID) throws Exception
  {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID, true);
    Document jdomDoc = group.toJDOMDocument();
    showAsXML(jdomDoc);
  }

  /**
   * This method invokes MCRUserMgr.retrieveUser() and then works with the
   * retrieved user object to get an XML-Representation.
   *
   * @param userID the ID of the user for which the XML-representation is needed
   */
  public static void showUser(String userID) throws Exception
  {
    MCRUser user = MCRUserMgr.instance().retrieveUser(userID, true);
    Document jdomDoc = user.toJDOMDocument();
    showAsXML(jdomDoc);
  }

  /** This method sets the user management component to read/write access mode */
  public static void unLock() throws Exception
  {
    init();
    MCRUserMgr.instance().setLock(false);
    logger.info("Write access to the user component persistent database now is allowed.");
  }

  /**
   * update data from a file. This method calls loadFromFile().
   *
   * @param filename
   *   name of a file or directory containing user or group information in XML files
   */
  public static void updateFromFile(String filename) throws Exception
  { loadFromFile(filename, "update"); }

  /**
   * This method takes a filename or a directory as a parameter, determines whether the
   * given file is a directory or data file. If it is a directory, loadFromXMLFile()
   * is called for all files in the directory. If it is a file, loadFromXMLFile() is
   * called directly for this file.
   *
   * @param filename
   *   name of a file or directory containing user, group or privilege data in XML files
   * @param todo
   *   String value determining what to to (either "import", "create" or "update")
   */
  private static void loadFromFile(String filename, String todo) throws Exception
  {
    init();
    String SLASH = new String((System.getProperties()).getProperty("file.separator"));
    int fnLength;  // Length of the file name
    File inFile = new File(filename);

    logger.info("Creating|importing|updating user/group/privilege data "
                      + " from file|directory: "+filename);

    if (inFile.isDirectory())
    {
      String [] fileList = inFile.list();
      if (fileList.length == 0)
        logger.info("The given directory is empty!");

      int xmlFileCounter = 0;
      for (int i=0; i<fileList.length; i++)
      {
        fnLength = fileList[i].length();
        if (fileList[i].substring(fnLength-4, fnLength).equals(".xml")) {
          xmlFileCounter++;
          String uri = filename+SLASH+fileList[i];
          loadFromXMLFile(uri, todo);
        }
      }
      if (xmlFileCounter == 0)
        logger.info("The given directory contains no .xml file!");

      return;  // All .xml-files in the given directory are read.
    }  // No, the given parameter is *not* a directory...

    if (inFile.isFile() && filename.substring(filename.length()-4, filename.length()).equals(".xml"))
      loadFromXMLFile(filename, todo);
    else logger.info("File not valid or not !");
  }

  /**
   * This private method is invoked by loadFromFile(). It takes a filename (String) as a parameter
   * and expects that this file is an XML file. The second parameter (String) determines if the user objects are
   * "imported", "created" or "updated". The difference is the handling of the group information when creating
   * resp. importing or updating users. This method parses the file and passes the jdom documents to other
   * methods, depending on the type of the object to be created, i.e. users, groups or privileges.
   *
   * @param filename    name of the XML-file
   * @param todo        String value "import", "create" or "update"
   */
  private static void loadFromXMLFile(String filename, String todo) throws Exception
  {
    init();
    logger.info("Reading file : "+filename+"\n");
    Document jdomDoc = null;
    File inFile = new File(filename);

    try {
      org.jdom.input.SAXBuilder b = new org.jdom.input.SAXBuilder(false);
      jdomDoc = b.build(inFile);
      Element jdomRootElement = jdomDoc.getRootElement();
      String type = (String)jdomRootElement.getAttributeValue("type").trim();

      if (type.equals("user"))
        loadUserFromXMLFile(jdomRootElement, todo);
      else if (type.equals("group"))
        loadGroupFromXMLFile(jdomRootElement, todo);
      else if (type.equals("privilege"))
        loadPrivilegeFromXMLFile(jdomRootElement);
      else
        logger.info("MCRUserCommands: unknown object type!");
    }
    catch (Exception e) {
      logger.info("Exception: "+e.getMessage());
    }
  }

  /**
   * This private method is invoked by loadFromXMLFile(). It takes a jdom element as a parameter. This
   * element contains the information to import, create or update one or more users. The second parameter
   * (String) determines if the user objects are "imported", "created" or "updated". The difference is the
   * handling of the group information. If a user is "imported" the group information in the document
   * is not taken into account (since typically groups are imported at the same time and the information
   * is redundant).
   *
   * @param rootElement  the root element of the xml document read in in loadFromXMLFile()
   * @param todo         String value "import", "create" or "update"
   */
  private static void loadUserFromXMLFile(Element rootElement, String todo) throws Exception
  {
    init();
    List userList  = rootElement.getChildren();
    int iNumUsers  = userList.size();
    logger.info("Number of users to create resp. update: "+iNumUsers);

    for (int i=0; i<iNumUsers; i++) {
      Element userElement = (Element)userList.get(i);
      Element userContactElement = userElement.getChild("user.contact");
      Element userGroupElement = userElement.getChild("user.groups");
      List groupIDList = userGroupElement.getChildren();
      Vector groups = null;  // this is for the "import" case

      if ((todo.equals("create")) || (todo.equals("update"))) {
        groups = new Vector();
        for (int j=0; j<groupIDList.size(); j++) {
          Element groupID = (Element)groupIDList.get(j);
          if (!((String)groupID.getTextTrim()).equals(""))
            groups.add((String)groupID.getTextTrim());
        }
      }

      boolean idEnabled = (userElement.getAttributeValue("id_enabled").equals("true")) ? true : false;
      boolean updateAllowed = (userElement.getAttributeValue("update_allowed").equals("true")) ? true : false;

      String salutation = (userContactElement.getChildTextTrim("contact.salutation") != null)
        ? userContactElement.getChildTextTrim("contact.salutation") : "";
      String firstname = (userContactElement.getChildTextTrim("contact.firstname") != null)
        ? userContactElement.getChildTextTrim("contact.firstname") : "";
      String lastname = (userContactElement.getChildTextTrim("contact.lastname") != null)
        ? userContactElement.getChildTextTrim("contact.lastname") : "";
      String street = (userContactElement.getChildTextTrim("contact.street") != null)
        ? userContactElement.getChildTextTrim("contact.street") : "";
      String city = (userContactElement.getChildTextTrim("contact.city") != null)
        ? userContactElement.getChildTextTrim("contact.city") : "";
      String postalcode = (userContactElement.getChildTextTrim("contact.postalcode") != null)
        ? userContactElement.getChildTextTrim("contact.postalcode") : "";
      String country = (userContactElement.getChildTextTrim("contact.country") != null)
        ? userContactElement.getChildTextTrim("contact.country") : "";
      String institution = (userContactElement.getChildTextTrim("contact.institution") != null)
        ? userContactElement.getChildTextTrim("contact.institution") : "";
      String faculty = (userContactElement.getChildTextTrim("contact.faculty") != null)
        ? userContactElement.getChildTextTrim("contact.faculty") : "";
      String department = (userContactElement.getChildTextTrim("contact.department") != null)
        ? userContactElement.getChildTextTrim("contact.department") : "";
      String institute = (userContactElement.getChildTextTrim("contact.institute") != null)
        ? userContactElement.getChildTextTrim("contact.institute") : "";
      String telephone = (userContactElement.getChildTextTrim("contact.telephone") != null)
        ? userContactElement.getChildTextTrim("contact.telephone") : "";
      String fax = (userContactElement.getChildTextTrim("contact.fax") != null)
        ? userContactElement.getChildTextTrim("contact.fax") : "";
      String email = (userContactElement.getChildTextTrim("contact.email") != null)
        ? userContactElement.getChildTextTrim("contact.email") : "";
      String cellphone = (userContactElement.getChildTextTrim("contact.cellphone") != null)
        ? userContactElement.getChildTextTrim("contact.cellphone") : "";

      if ((todo.equals("import")) || (todo.equals("create")))
      {
        Timestamp creationDate = null;
        String date = (String)userElement.getChildTextTrim("user.creation_date");
        if (date.length() > 0)
          creationDate = Timestamp.valueOf(date);
        else creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());

        Timestamp modifiedDate = null;
        date = (String)userElement.getChildTextTrim("user.last_modified");
        if (date.length() > 0)
          modifiedDate = Timestamp.valueOf(date);
        else modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());

        MCRUser newUser = new MCRUser(
          Integer.parseInt((String)userElement.getAttributeValue("numID")),
          userElement.getAttributeValue("ID"),
          idEnabled, updateAllowed,
          userElement.getChildTextTrim("user.creator"),
          creationDate, modifiedDate,
          userElement.getChildTextTrim("user.description"),
          userElement.getChildTextTrim("user.password"),
          salutation, firstname, lastname, street, city, postalcode, country,
          institution, faculty, department, institute, telephone, fax, email, cellphone,
          userElement.getChildTextTrim("user.primary_group"),
          groups, "create");
      }
      else { // the "update" case
        String userID = (String)userElement.getAttributeValue("ID");
        MCRUser updUser = MCRUserMgr.instance().retrieveUser(userID, true);
        updUser.update(
          idEnabled, updateAllowed,
          userElement.getChildTextTrim("user.description"),
          userElement.getChildTextTrim("user.password"),
          salutation, firstname, lastname, street, city, postalcode, country,
          institution, faculty, department, institute, telephone, fax,
          email, cellphone,
          userElement.getChildTextTrim("user.primary_group"),
          groups, "update");
      }
    } // end for
    logger.info("All users created resp. updated.");
  }

  /**
   * This private method is invoked by loadFromXMLFile(). It takes a jdom element as a parameter. This
   * element contains the information to import, create or update one or more groups. The second parameter
   * (String) determines if the group objects are "imported", "created" or "updated". The difference is the
   * handling of the group information. If a group is "imported" the information determining in which other
   * group the current group is a member of is not taken into account since this is redundant information
   * (the other groups know which members they have).
   *
   * @param rootElement  the root element of the xml document read in in loadFromXMLFile()
   * @param todo         String value "import", "create" or "update"
   */
  private static void loadGroupFromXMLFile(Element rootElement, String todo) throws Exception
  {
    init();
    boolean create = false;
    List groupList = rootElement.getChildren();
    int iNumGroups = groupList.size();
    logger.info("Number of groups to create resp. update: "+iNumGroups);

    for (int i=0; i<iNumGroups; i++) { // Loop over all groups in the xml file
      Vector groups  = null;
      Vector users   = null;
      Vector memberGroups = null;
      Vector adminUsers = null;
      Vector adminGroups = null;
      Vector privileges = null;

      Element groupElement   = (Element)groupList.get(i);
      Element adminsElement  = groupElement.getChild("group.admins");
      Element membersElement = groupElement.getChild("group.members");
      Element groupsElement  = groupElement.getChild("group.groups");
      Element privsElement   = groupElement.getChild("group.privileges");

      List admUserIDList = adminsElement.getChildren("admins.userID");
      if (admUserIDList.size() > 0) {
        adminUsers = new Vector();
        for (int j=0; j<admUserIDList.size(); j++) {
          Element admUserIDElement = (Element)admUserIDList.get(j);
          if (!((String)admUserIDElement.getTextTrim()).equals(""))
            adminUsers.add((String)admUserIDElement.getTextTrim());
        }
      }

      List admGroupIDList = adminsElement.getChildren("admins.groupID");
      if (admGroupIDList.size() > 0) {
        adminGroups = new Vector();
        for (int j=0; j<admGroupIDList.size(); j++) {
          Element admGroupIDElement = (Element)admGroupIDList.get(j);
          if (!((String)admGroupIDElement.getTextTrim()).equals(""))
            adminGroups.add((String)admGroupIDElement.getTextTrim());
        }
      }

      List userIDList = membersElement.getChildren("members.userID");
      if (userIDList.size() > 0) {
        users = new Vector();
        for (int j=0; j<userIDList.size(); j++) {
          Element userIDElement = (Element)userIDList.get(j);
          if (!((String)userIDElement.getTextTrim()).equals(""))
            users.add((String)userIDElement.getTextTrim());
        }
      }

      List memberGroupIDList = membersElement.getChildren("members.groupID");
      if (memberGroupIDList.size() > 0) {
        memberGroups = new Vector();
        for (int j=0; j<memberGroupIDList.size(); j++) {
          Element memberGroupID = (Element)memberGroupIDList.get(j);
          if (!((String)memberGroupID.getTextTrim()).equals(""))
            memberGroups.add((String)memberGroupID.getTextTrim());
        }
      }

      List privsList = privsElement.getChildren("privileges.privilege");
      if (privsList.size() > 0) {
        privileges = new Vector();
        for (int j=0; j<privsList.size(); j++) {
          Element privilege = (Element)privsList.get(j);
          if (!((String)privilege.getTextTrim()).equals(""))
            privileges.add((String)privilege.getTextTrim());
        }
      }

      List groupIDList = groupsElement.getChildren();
      if (todo.equals("import")) {
        groups = null;
      }
      else if ((todo.equals("create")) || (todo.equals("update"))) {
        groups = new Vector();
        for (int j=0; j<groupIDList.size(); j++) {
          Element groupID = (Element)groupIDList.get(j);
          if (!((String)groupID.getTextTrim()).equals(""))
            groups.add((String)groupID.getTextTrim());
        }
      }

      if (todo.equals("create"))
        create = true;
      else create = false;

      if ((todo.equals("import")) || (todo.equals("create")))
      {
        Timestamp creationDate = null;
        String date = (String)groupElement.getChildTextTrim("group.creation_date");
        if (date.length() > 0)
          creationDate = Timestamp.valueOf(date);
        else creationDate = new Timestamp(new GregorianCalendar().getTime().getTime());

        Timestamp modifiedDate = null;
        date = (String)groupElement.getChildTextTrim("group.last_modified");
        if (date.length() > 0)
          modifiedDate = Timestamp.valueOf(date);
        else modifiedDate = new Timestamp(new GregorianCalendar().getTime().getTime());

        MCRGroup newGroup = new MCRGroup(
          groupElement.getAttributeValue("ID"),
          groupElement.getChildTextTrim("group.creator"),
          creationDate, modifiedDate, groupElement.getChildTextTrim("group.description"),
          adminUsers, adminGroups, users, memberGroups, groups, privileges, create, "create");
      }
      else { // the "update" case
        String groupID = (String)groupElement.getAttributeValue("ID");
        MCRGroup updGroup = MCRUserMgr.instance().retrieveGroup(groupID, true);
        updGroup.update(
          groupElement.getChildTextTrim("group.description"),
          adminUsers, adminGroups, users, memberGroups, groups, privileges, "update");
      }
    }
    logger.info("All groups created resp. updated.");
  }

  /**
   * This private method is invoked by loadFromXMLFile(). It takes a jdom element as a parameter. This
   * element contains the information to import or create one or more privileges.
   *
   * @param rootElement  the root element of the xml document read in in loadFromXMLFile()
   */
  private static void loadPrivilegeFromXMLFile(Element rootElement) throws Exception
  {
    init();
    List privList = rootElement.getChildren();
    int iNumPrivs = privList.size();
    Vector privileges = new Vector();
    logger.info("Number of privileges to create resp. update: "+iNumPrivs);

    for (int i=0; i<iNumPrivs; i++) {
      Element privElement = (Element)privList.get(i);

      MCRPrivilege priv = new MCRPrivilege(
        (String)privElement.getAttributeValue("name"),
        privElement.getChildTextTrim("privilege.description"));

      privileges.add(priv);
    }
    MCRPrivilegeSet.instance().loadPrivileges(privileges);
    logger.info("All privileges created resp. updated.");
  }

  /**
   * This method just prints a pretty XML output to System.out.
   * @param jdomDoc  the JDOM XML document to be printed
   */
  private static final void showAsXML(Document jdomDoc)
  {
    XMLOutputter outputter = new XMLOutputter("  ", true);
    try {
      outputter.output(jdomDoc, System.out);
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }

  /**
   * This method just saves a JDOM document to a file
   * @param jdomDoc  the JDOM XML document to be printed
   * @param outFile  a FileWriter object for the output
   */
  private static final void saveToXMLFile(Document jdomDoc, FileWriter outFile)
  {
    XMLOutputter outputter = new XMLOutputter("  ", true);
    try {
      outputter.output(jdomDoc, outFile);
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }
}

