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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.user.*;

/**
 * This class provides a set of commands for the org.mycore.user management which
 * can be used by the command line interface.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserCommands
{
private static Logger logger =
  Logger.getLogger(MCRUserCommands.class.getName());
private static MCRConfiguration config = null;

/**
 * Initialize common data.
 **/
private static void init()
  {
  config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties());
  }

/**
 * The method initialized the user and group system an creat a
 * superunser with the values of the mycore.properties. As 'super'
 * default, if no properties was find, mcradmin with password mycore was 
 * used.
 *
 * @param session the MCRSession object
 **/
public static void initSuperuser(MCRSession session) throws MCRException
  {
  init();
  String suser = config.getString("MCR.users_superuser_username","mcradmin");
  String spasswd = config.getString("MCR.users_superuser_userpasswd","mycore");
  String sgroup = config.getString("MCR.users_superuser_groupname","mcrgroup");
  String guser = config.getString("MCR.users_guestuser_username","gast");
  String gpasswd = config.getString("MCR.users_guestuser_userpasswd","gast");
  String ggroup = config.getString("MCR.users_guestuser_groupname","mcrgast");

  try {
    MCRUser testu = MCRUserMgr.instance().retrieveUser(suser);
    MCRGroup testg = MCRUserMgr.instance().retrieveGroup(sgroup);
    logger.info("The superuser already exist!");
    return;
    }
  catch (Exception e) { }

  try {
    MCRPrivilegeSet p = MCRPrivilegeSet.instance();
    ArrayList privList = new ArrayList();
    privList.add(new MCRPrivilege("create user",
      "Users with this privilege may create new users."));
    privList.add(new MCRPrivilege("create group",
      "Users with this privilege may create new groups."));
    privList.add(new MCRPrivilege("delete user",
      "Users with this privilege may delete other users."));
    privList.add(new MCRPrivilege("delete group",
      "Users with this privilege may delete groups."));
    privList.add(new MCRPrivilege("modify user",
      "Users with this privilege may modify data of other users."));
    privList.add(new MCRPrivilege("modify group",
      "Users with this privilege may modify data of groups."));
    privList.add(new MCRPrivilege("user administrator",
      "Users with this privilege has administrator rights in the system."));
    privList.add(new MCRPrivilege("list all users",
      "Users with this privilege may list the users of the system."));
    privList.add(new MCRPrivilege("create object in datastore",
      "Users with this privilege may create objects (documents etc.)."));
    p.loadPrivileges(privList);
    }
  catch (Exception e) {
    throw new MCRException("Can't create the privileges.",e); }
  logger.info("The privillege set is installed.");

  // the superuser group
  try {
    ArrayList admUserIDs = new ArrayList();
    admUserIDs.add(suser);
    ArrayList admGroupIDs = new ArrayList();
    ArrayList mbrUserIDs = new ArrayList();
    mbrUserIDs.add(suser);
    ArrayList mbrGroupIDs = new ArrayList();
    ArrayList groupIDs = new ArrayList();
    ArrayList privileges= new ArrayList();
    privileges.add("create user");
    privileges.add("delete user");
    privileges.add("modify user");
    privileges.add("create group");
    privileges.add("delete group");
    privileges.add("modify group");
    privileges.add("list all users");
    privileges.add("user administrator");
    privileges.add("create object in datastore");
    MCRGroup g = new MCRGroup(sgroup,suser,null,null,"The superuser group",
      admUserIDs,admGroupIDs,mbrUserIDs,mbrGroupIDs,groupIDs,privileges);
    MCRUserMgr.instance().initializeGroup(g,suser);
    }
  catch (Exception e) {
    throw new MCRException("Can't create the superuser group.",e); }
  logger.info("The group "+sgroup+" is installed.");

  // the guest group
  try {
    ArrayList admUserIDs = new ArrayList();
    admUserIDs.add(suser);
    ArrayList admGroupIDs = new ArrayList();
    admGroupIDs.add(sgroup);
    ArrayList mbrUserIDs = new ArrayList();
    mbrUserIDs.add(guser);
    mbrUserIDs.add(suser);
    ArrayList mbrGroupIDs = new ArrayList();
    mbrGroupIDs.add(sgroup);
    ArrayList groupIDs = new ArrayList();
    ArrayList privileges= new ArrayList();
    MCRGroup g = new MCRGroup(ggroup,suser,null,null,"The guest group",
      admUserIDs,admGroupIDs,mbrUserIDs,mbrGroupIDs,groupIDs,privileges);
    MCRUserMgr.instance().initializeGroup(g,suser);
    }
  catch (Exception e) {
    throw new MCRException("Can't create the superuser group.",e); }
  logger.info("The group "+ggroup+" is installed.");

  // the superuser
  try {
    ArrayList groupIDs = new ArrayList();
    groupIDs.add(sgroup);
    groupIDs.add(ggroup);
    MCRUser u = new MCRUser(1,suser,suser,null,null,true,false,"Superuser",
      spasswd,sgroup,groupIDs,null,null,null,null,null,null,null,null,
      null,null,null,null,null,null,null,null);
    MCRUserMgr.instance().initializeUser(u,suser);
    }
  catch (Exception e) {
    throw new MCRException("Can't create the superuser.",e); }
  logger.info("The user "+suser+" with password "+spasswd+" is installed.");

  // the guest
  try {
    ArrayList groupIDs = new ArrayList();
    groupIDs.add(ggroup);
    MCRUser u = new MCRUser(2,guser,suser,null,null,true,true,"guest",
      gpasswd,ggroup,groupIDs,null,null,null,null,null,null,null,null,
      null,null,null,null,null,null,null,null);
    MCRUserMgr.instance().initializeUser(u,suser);
    }
  catch (Exception e) {
    throw new MCRException("Can't create the guest.",e); }
  logger.info("The user "+guser+" with password "+gpasswd+" is installed.");

  // check all
  session.setCurrentUserID(suser);
  MCRUserMgr.instance().checkConsistency(session);
  logger.info("");
  }

/**
 * This method checks the data consistency of the user management and should be
 * called after a system crash or after importing data from files, respectively.
 *
 * @param session the MCRSession object
 **/
public static void checkConsistency(MCRSession session) throws Exception
  { MCRUserMgr.instance().checkConsistency(session); }

/**
 * This method invokes MCRUserMgr.deleteGroup() and permanently removes
 * a group from the system.
 *
 * @param session the MCRSession object
 * @param groupID the ID of the group which will be deleted
 **/
public static void deleteGroup(MCRSession session, String groupID) throws Exception
  {
  init();
  MCRUserMgr.instance().deleteGroup(session, groupID);
  logger.info("Group ID " + groupID + " deleted!");
  }

/**
 * This method invokes MCRUserMgr.deleteUser() and permanently removes
 * a user from the system.
 *
 * @param session the MCRSession object
 * @param userID the ID of the user which will be deleted
 **/
public static void deleteUser(MCRSession session, String userID) throws Exception
  {
  init();
  MCRUserMgr.instance().deleteUser(session,userID);
  logger.info("User ID " + userID + " deleted!");
  }

/**
 * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
 * of all users stored in the persistent datastore.
 *
 * @param session the MCRSession object
 **/
public static void listAllUsers(MCRSession session) throws Exception
  {
  init();
  ArrayList users = MCRUserMgr.instance().getAllUserIDs(session);
  logger.info("");
  for (int i=0; i<users.size(); i++) { logger.info((String)users.get(i)); }
  }

/**
 * This method invokes MCRUserMgr.getAllGroupIDs() and retrieves a ArrayList
 * of all groups stored in the persistent datastore.
 *
 * @param session the MCRSession object
 **/
public static void listAllGroups(MCRSession session) throws Exception
  {
  init();
  ArrayList groups = MCRUserMgr.instance().getAllGroupIDs(session);
  logger.info("");
  for (int i=0; i<groups.size(); i++) { logger.info((String)groups.get(i)); }
  }

/**
 * This method invokes MCRPrivilegeSet.getPrivileges() and retrieves a ArrayList
 * of all privileges stored in the persistent datastore.
 **/
public static void listAllPrivileges() throws MCRException
  {
  try {
    init();
    ArrayList privs = MCRPrivilegeSet.instance().getPrivileges();
    logger.info("");
    for (int i=0; i<privs.size(); i++) {
      MCRPrivilege currentPriv = (MCRPrivilege)privs.get(i);
      logger.info(currentPriv.getName());
      logger.info("    "+currentPriv.getDescription());
      }
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveAllGroupsToFile()",e); }
  }

/**
 * This command takes a file name as a parameter, retrieves all groups from 
 * MCRUserMgr as JDOM document and saves this to the given file.
 *
 * @param session the MCRSession object
 * @param filename Name of the file the groups will be saved to
 */
public static void saveAllGroupsToFile(MCRSession session, String filename) 
  throws MCRException
  {
  try {
    org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllGroups(session);
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveAllGroupsToFile()",e); }
  }

/**
 * This command takes a file name as a parameter, retrieves all privileges 
 * from MCRPrivilegeSet as JDOM document and saves this to the given file.
 *
 * @param filename Name of the file the privileges will be saved to
 */
public static void saveAllPrivilegesToFile(String filename)
  throws MCRException
  {
  try {
    org.jdom.Document jdomDoc = MCRPrivilegeSet.instance().toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveAllPrivilegesToFile()",e); }
  }

/**
 * This command takes a file name as a parameter, retrieves all users from 
 * MCRUserMgr as JDOM document and saves this to the given file.
 *
 * @param session the MCRSession object
 * @param filename Name of the file the users will be saved to
 */
public static void saveAllUsersToFile(MCRSession session, String filename) 
  throws MCRException
  {
  try {
    org.jdom.Document jdomDoc = MCRUserMgr.instance().getAllUsers(session);
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveAllUsersToFile()",e); }
  }

/**
 * This command takes a groupID and file name as a parameter, retrieves the 
 * group from MCRUserMgr as JDOM document and saves this to the given file.
 *
 * @param session the MCRSession object
 * @param groupID  ID of the group to be saved
 * @param filename Name of the file the groups will be saved to
 */
public static void saveGroupToFile(MCRSession session, String groupID, 
  String filename) throws Exception
  {
  try {
    MCRGroup group = MCRUserMgr.instance().retrieveGroup(session,groupID);
    org.jdom.Document jdomDoc = group.toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveGroupToFile()",e); }
  }

/**
 * This command takes a userID and file name as a parameter, retrieves the 
 * user from MCRUserMgr as JDOM document and saves this to the given file.
 *
 * @param session the MCRSession object
 * @param userID   ID of the user to be saved
 * @param filename Name of the file the user will be saved to
 */
public static void saveUserToFile(MCRSession session, String userID, 
  String filename) throws MCRException
  {
  try {
    MCRUser user = MCRUserMgr.instance().retrieveUser(session,userID);
    org.jdom.Document jdomDoc = user.toJDOMDocument();
    FileWriter outFile = new FileWriter(new File(filename));
    saveToXMLFile(jdomDoc, outFile);
    }
  catch (Exception e) {
    throw new MCRException("Error while command saveUserToFile()",e); }
  }

/**
 * This method invokes MCRUserMgr.retrieveUser() and then works with the
 * retrieved user object to change the password.
 *
 * @param session the MCRSession object
 * @param userID the ID of the user for which the password will be set
 */
public static void setPassword(MCRSession session,String userID, 
  String password) throws MCRException
  {
  if (password == null) return;
  init();
  MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
  user.setPassword(password);
  MCRUserMgr.instance().updateUser(session,user);
  logger.info("The new password was set.");
  }

/** 
 *This method sets the user management component to read only mode
 *
 * @param session the MCRSession object
 */
public static void setLock(MCRSession session) throws MCRException
  {
  init();
  MCRUserMgr.instance().setLock(session,true);
  logger.info("Write access to the user component persistent database now is"+
    " denied.");
  }

/** 
 * This method sets the user management component to read/write access mode
 *
 * @param session the MCRSession object
 */
public static void unLock(MCRSession session) throws MCRException
  {
  init();
  MCRUserMgr.instance().setLock(session,false);
  logger.info("Write access to the user component persistent database now is"+
    " allowed.");
  }

/**
 * This method invokes MCRUserMgr.retrieveGroup() and then works with the
 * retrieved group object to get an XML-Representation.
 *
 * @param session the MCRSession object
 * @param groupID the ID of the group for which the XML-representation is needed
 */
public static final void showGroup(MCRSession session,String groupID) 
  throws MCRException
  {
  MCRGroup group = MCRUserMgr.instance().retrieveGroup(session,groupID, true);
  org.jdom.Document jdomDoc = group.toJDOMDocument();
  showAsXML(jdomDoc);
  }

/**
 * This method invokes MCRUserMgr.retrieveUser() and then works with the
 * retrieved user object to get an XML-Representation.
 *
 * @param session the MCRSession object
 * @param userID the ID of the user for which the XML-representation is needed
 */
public static final void showUser(MCRSession session, String userID) 
  throws MCRException
  {
  MCRUser user = MCRUserMgr.instance().retrieveUser(session,userID, true);
  org.jdom.Document jdomDoc = user.toJDOMDocument();
  showAsXML(jdomDoc);
  }

/**
 * Check the file name
 * @param filename the filename of the user data input
 * @return true if the file name is okay
 **/
private static final boolean checkFilename(String filename)
  {
  init();
  if( ! filename.endsWith( ".xml" ) ) {
    logger.warn( filename + " ignored, does not end with *.xml" ); 
    return false; }
  if( ! new File( filename ).isFile() ) {
    logger.warn( filename + " ignored, is not a file." ); 
    return false;}
  return true;
  }

/**
 * This method invokes MCRUserMgr.createUser() with data from a file.
 * @param session the MCRSession object
 * @param filename the filename of the user data input
 **/
public static final void createUserFromFile(MCRSession session,String filename)
  {
  init();
  if (!checkFilename(filename)) return;
  logger.info( "Reading file " + filename + " ..." );
  try {
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    org.jdom.Document doc = bulli.build(MCRXMLHelper.parseURI(filename,false));
    org.jdom.Element rootelm = doc.getRootElement();
    if (!rootelm.getAttribute("type").getValue().equals("user")) {
      throw new MCRException("The data are not for user."); }
    List listelm = rootelm.getChildren();
    for (int i=0;i<listelm.size();i++) {
      MCRUser u = new MCRUser((org.jdom.Element)listelm.get(i));
      MCRUserMgr.instance().createUser(session,u);
      }
    }
  catch (Exception e) {
    throw new MCRException("Error while loading user data.",e); }
  }

/**
 * This method invokes MCRUserMgr.createGroup() with data from a file.
 * @param session the MCRSession object
 * @param filename the filename of the user data input
 **/
public static final void createGroupFromFile(MCRSession session,String filename)
  {
  init();
  if (!checkFilename(filename)) return;
  logger.info( "Reading file " + filename + " ..." );
  try {
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    org.jdom.Document doc = bulli.build(MCRXMLHelper.parseURI(filename,false));
    org.jdom.Element rootelm = doc.getRootElement();
    if (!rootelm.getAttribute("type").getValue().equals("group")) {
      throw new MCRException("The data are not for group."); }
    List listelm = rootelm.getChildren();
    for (int i=0;i<listelm.size();i++) {
      MCRGroup g = new MCRGroup((org.jdom.Element)listelm.get(i));
      MCRUserMgr.instance().createGroup(session,g);
      }
    }
  catch (Exception e) {
    throw new MCRException("Error while loading group data.",e); }
  }

/**
 * This method invokes MCRUserMgr.updateUser() with data from a file.
 * @param session the MCRSession object
 * @param filename the filename of the user data input
 **/
public static final void updateUserFromFile(MCRSession session,String filename)
  {
  init();
  if (!checkFilename(filename)) return;
  logger.info( "Reading file " + filename + " ..." );
  try {
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    org.jdom.Document doc = bulli.build(MCRXMLHelper.parseURI(filename,false));
    org.jdom.Element rootelm = doc.getRootElement();
    if (!rootelm.getAttribute("type").getValue().equals("user")) {
      throw new MCRException("The data are not for user."); }
    List listelm = rootelm.getChildren();
    for (int i=0;i<listelm.size();i++) {
      MCRUser u = new MCRUser((org.jdom.Element)listelm.get(i));
      MCRUserMgr.instance().updateUser(session,u);
      }
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
  }

/**
 * This method invokes MCRUserMgr.updateGroup() with data from a file.
 * @param session the MCRSession object
 * @param filename the filename of the user data input
 **/
public static final void updateGroupFromFile(MCRSession session,String filename)
  {
  init();
  if (!checkFilename(filename)) return;
  logger.info( "Reading file " + filename + " ..." );
  try {
    org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
    org.jdom.Document doc = bulli.build(MCRXMLHelper.parseURI(filename));
    org.jdom.Element rootelm = doc.getRootElement();
    if (!rootelm.getAttribute("type").getValue().equals("group")) {
      throw new MCRException("The data are not for group."); }
    List listelm = rootelm.getChildren();
    for (int i=0;i<listelm.size();i++) {
      MCRGroup g = new MCRGroup((org.jdom.Element)listelm.get(i));
      MCRUserMgr.instance().updateGroup(session,g);
      }
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
  }

/**
 * This method just prints a pretty XML output to System.out.
 * @param jdomDoc  the JDOM XML document to be printed
 **/
private static final void showAsXML(org.jdom.Document jdomDoc)
  {
  org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter("  ", true);
  try { outputter.output(jdomDoc, System.out); }
  catch (Exception e) {
    throw new MCRException("Error while show XML to file."); }
  }

/**
 * This method just saves a JDOM document to a file
 * @param jdomDoc  the JDOM XML document to be printed
 * @param outFile  a FileWriter object for the output
 */
private static final void saveToXMLFile(org.jdom.Document jdomDoc, 
  FileWriter outFile)
  {
  org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter("  ", true);
  try { outputter.output(jdomDoc, outFile); }
  catch (Exception e) { 
    throw new MCRException("Error while save XML to file."); }
  }
}

