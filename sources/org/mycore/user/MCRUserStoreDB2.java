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
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRUsageException;
import mycore.sql.*;

/**
 * This class implements the interface MCRUserStore and uses DB2 tables for
 * persistent storage of MyCoRe user, group and privileges information, respectively.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserStoreDB2 implements MCRUserStore
{
  /** name of the db2 table containing user information */
  private String DB2UsersTable;

  /** name of the db2 table containing group information */
  private String DB2GroupsTable;

  /** name of the db2 table containing user and group membership information */
  private String DB2GroupMembersTable;

  /** name of the db2 table containing group admin information */
  private String DB2GroupAdminsTable;

  /** name of the db2 table containing privilege information */
  private String DB2PrivilegesTable;

  /** name of the db2 table containing group-privilege information */
  private String DB2PrivsLookupTable;

  /**
   * The constructor reads the names of the DB2 tables which hold the user information
   * data from mycore.properties. The existence of the tables is checked. If the tables
   * do not yet exist they will be created.
   */
  public MCRUserStoreDB2()
  {
    DB2UsersTable        = MCRConfiguration.instance().getString("MCR.users_store_db2_table_users");
    DB2GroupsTable       = MCRConfiguration.instance().getString("MCR.users_store_db2_table_groups");
    DB2GroupMembersTable = MCRConfiguration.instance().getString("MCR.users_store_db2_table_group_members");
    DB2GroupAdminsTable  = MCRConfiguration.instance().getString("MCR.users_store_db2_table_group_admins");
    DB2PrivilegesTable   = MCRConfiguration.instance().getString("MCR.users_store_db2_table_privileges");
    DB2PrivsLookupTable  = MCRConfiguration.instance().getString("MCR.users_store_db2_table_privs_lookup");

    if (!tablesExist()) createTables();
  }

  /**
   * This method creates a MyCoRe user object in the persistent datastore.
   * @param newUser      the new user object to be stored
   */
  public synchronized void createUser(MCRUser newUser) throws Exception
  {
    String idEnabled = (newUser.isEnabled()) ? "true" : "false";
    String updateAllowed = (newUser.isUpdateAllowed()) ? "true" : "false";
    Vector groups = newUser.getGroups();
    MCRUserAddress userAddress = newUser.getAddressObject();
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String insert = "INSERT INTO " + DB2UsersTable
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

      statement.setString    ( 1, newUser.getID()              );
      statement.setString    ( 2, newUser.getPassword()        );
      statement.setString    ( 3, idEnabled                    );
      statement.setString    ( 4, updateAllowed                );
      statement.setString    ( 5, newUser.getCreator()         );
      statement.setTimestamp ( 6, newUser.getCreationDate()    );
      statement.setTimestamp ( 7, newUser.getLastChangesDate() );
      statement.setString    ( 8, newUser.getDescription()     );
      statement.setString    ( 9, userAddress.getSalutation()  );
      statement.setString    (10, userAddress.getFirstName()   );
      statement.setString    (11, userAddress.getLastName()    );
      statement.setString    (12, userAddress.getStreet()      );
      statement.setString    (13, userAddress.getCity()        );
      statement.setString    (14, userAddress.getPostalCode()  );
      statement.setString    (15, userAddress.getCountry()     );
      statement.setString    (16, userAddress.getInstitution() );
      statement.setString    (17, userAddress.getFaculty()     );
      statement.setString    (18, userAddress.getDepartment()  );
      statement.setString    (19, userAddress.getInstitute()   );
      statement.setString    (20, userAddress.getTelephone()   );
      statement.setString    (21, userAddress.getFax()         );
      statement.setString    (22, userAddress.getEmail()       );
      statement.setString    (23, userAddress.getCellphone()   );
      statement.setString    (24, newUser.getPrimaryGroup()    );

      statement.execute();
      statement.close();

      // now update the member lookup table
      insert = "INSERT INTO " + DB2GroupMembersTable + " VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newUser.getGroups().size(); i++) {
        statement.setString ( 1, newUser.getID() );
        statement.setString ( 2, (String)newUser.getGroups().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }
      statement.close();

      connection.getJDBCConnection().commit();
      connection.getJDBCConnection().setAutoCommit(true);
    }

    catch(Exception ex)
    {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw ex;
    }

    finally
    { connection.release(); }
  }

  /**
   * This method creates a MyCoRe group object in the persistent datastore.
   * @param newGroup     the new group object to be stored
   */
  public synchronized void createGroup(MCRGroup newGroup) throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String insert = "INSERT INTO " + DB2GroupsTable
                    + " VALUES (?,?,?,?,?)";
      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

      statement.setString    ( 1, newGroup.getID()              );
      statement.setString    ( 2, newGroup.getCreator()         );
      statement.setTimestamp ( 3, newGroup.getCreationDate()    );
      statement.setTimestamp ( 4, newGroup.getLastChangesDate() );
      statement.setString    ( 5, newGroup.getDescription()     );

      statement.execute();
      statement.close();

      // Now we update the group admins table. First the admin users and
      // thereafter the admin groups

      insert = "INSERT INTO " + DB2GroupAdminsTable + " VALUES (?,?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getAdminUsers().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getAdminUsers().elementAt(i) );
        statement.setString ( 3, "user" );
        statement.execute();
        statement.clearParameters();
      }

      for (int i=0; i<newGroup.getAdminGroups().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getAdminGroups().elementAt(i) );
        statement.setString ( 3, "group" );
        statement.execute();
        statement.clearParameters();
      }
      statement.close();

      // We do not need to update the membership lookup table. This is done
      // while creating/updating a user.

      // Now update the privileges lookup table
      insert = "INSERT INTO " + DB2PrivsLookupTable + " VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getPrivileges().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getPrivileges().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }
      statement.close();

      connection.getJDBCConnection().commit();
      connection.getJDBCConnection().setAutoCommit(true);
    }

    catch(Exception ex)
    {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw ex;
    }

    finally
    { connection.release(); }
  }

  /**
   * This method creates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object
   */
  public synchronized void createPrivilegeSet(MCRPrivilegeSet privilegeSet)
                           throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
    MCRPrivilege thePrivilege;
    Vector privileges = privilegeSet.getPrivileges();

    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String insert = "INSERT INTO " + DB2PrivilegesTable + " VALUES (?,?)";
      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<privileges.size(); i++) {
        thePrivilege = (MCRPrivilege)privileges.elementAt(i);
        statement.setString ( 1, (String)thePrivilege.getName() );
        statement.setString ( 2, (String)thePrivilege.getDescription() );
        statement.execute();
        statement.clearParameters();
      }
      statement.close();

      connection.getJDBCConnection().commit();
      connection.getJDBCConnection().setAutoCommit(true);
    }

    catch(Exception ex)
    {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw ex;
    }

    finally
    { connection.release(); }
  }

  /**
   * This method deletes a MyCoRe user object from the persistent datastore.
   * @param delUserID    a String representing the MyCoRe user object which is to be deleted
   */
  public synchronized void deleteUser(String delUserID) throws Exception
  {
    // We need not to care about updating the Member-Lookup Table (removing all the
    // references to this user object). This will be done automatically by DB2. See
    // the create table statements for that.

    String sql = "DELETE FROM " + DB2UsersTable + " WHERE UID = '" + delUserID + "'";
    MCRSQLConnection.justDoUpdate(sql);
  }

  /**
   * This method deletes a MyCoRe group object in the persistent datastore.
   * @param delGroupID   a String representing the MyCoRe group object which is to be deleted
   */
  public synchronized void deleteGroup(String delGroupID) throws Exception
  {
    // We need not to care about updating the Member-Lookup Table (removing all the
    // references to this group object). This will be done automatically by DB2. See
    // the create table statements for that.

    String sql = "DELETE FROM " + DB2GroupsTable + " WHERE GID = '" + delGroupID + "'";
    MCRSQLConnection.justDoUpdate(sql);
  }

  /**
   * This method tests if a MyCoRe user object is available in the persistent datastore.
   * @param userID        a String representing the MyCoRe user object which is to be looked for
   */
  public synchronized boolean existsUser(String userID) throws Exception
  {
    return MCRSQLConnection.justCheckExists(new MCRSQLStatement(DB2UsersTable)
      .setCondition("UID", userID)
      .toRowSelector());
  }

  /**
   * This method tests if a MyCoRe privilege set object is available in the persistent datastore.
   */
  public boolean existsPrivilegeSet() throws Exception
  {
    String select = "SELECT COUNT(NAME) FROM " + DB2PrivilegesTable;
    String nr = MCRSQLConnection.justGetSingleValue(select);
    int iNumber = Integer.parseInt(nr);
    return (iNumber == 0) ? false : true;
  }

  /**
   * This method tests if a MyCoRe group object is available in the persistent datastore.
   * @param groupID       a String representing the MyCoRe group object which is to be looked for
   */
  public synchronized boolean existsGroup(String groupID) throws Exception
  {
    return MCRSQLConnection.justCheckExists(new MCRSQLStatement(DB2GroupsTable)
      .setCondition("GID", groupID)
      .toRowSelector());
  }

  /**
   * This method gets all user IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the user IDs of the system
   */
  public synchronized Vector getAllUserIDs() throws Exception
  {
    String select = "SELECT UID FROM " + DB2UsersTable;
    return getSelectResult(select);
  }

  /**
   * This method gets all group IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the group IDs of the system
   */
  public synchronized Vector getAllGroupIDs() throws Exception
  {
    String select = "SELECT GID FROM " + DB2GroupsTable;
    return getSelectResult(select);
  }

  /**
   * This method retrieves a MyCoRe user object from the persistent datastore.
   * @param userID  a String representing the MyCoRe user object which is to be retrieved
   * @return        the requested user object
   */
  public synchronized MCRUser retrieveUser(String userID) throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
    try
    {
      String select = "SELECT * FROM " + DB2UsersTable + " WHERE UID = '" + userID + "'";
      Statement statement = connection.getJDBCConnection().createStatement();
      ResultSet rs = statement.executeQuery(select);

      if(!rs.next())
      {
        String msg = "There is no user with ID = " + userID;
        throw new MCRException(msg);
      }

      String passwd        = rs.getString    ( 2);
      String idEnabled     = rs.getString    ( 3);
      String updateAllowed = rs.getString    ( 4);
      String creator       = rs.getString    ( 5);
      Timestamp created    = rs.getTimestamp ( 6);
      Timestamp modified   = rs.getTimestamp ( 7);
      String description   = rs.getString    ( 8);
      String salutation    = rs.getString    ( 9);
      String firstname     = rs.getString    (10);
      String lastname      = rs.getString    (11);
      String street        = rs.getString    (12);
      String city          = rs.getString    (13);
      String postalcode    = rs.getString    (14);
      String country       = rs.getString    (15);
      String institution   = rs.getString    (16);
      String faculty       = rs.getString    (17);
      String department    = rs.getString    (18);
      String institute     = rs.getString    (19);
      String telephone     = rs.getString    (20);
      String fax           = rs.getString    (21);
      String email         = rs.getString    (22);
      String cellphone     = rs.getString    (23);
      String primaryGroup  = rs.getString    (24);

      rs.close();

      // Now lookup the groups this user is a member of
      select = "SELECT GID FROM " + DB2GroupMembersTable + " WHERE UID = '" + userID + "'";
      Vector groups = getSelectResult(select);

      // We create the user object
      MCRUser user = new MCRUser(userID, passwd, idEnabled, updateAllowed, creator,
        created, modified, description, salutation, firstname, lastname, street,
        city, postalcode, country, institution, faculty, department, institute,
        telephone, fax, email, cellphone, primaryGroup, groups, "");

      rs.close();
      return user;
    }

    finally{ connection.release(); }
  }

  /**
   * This method retrieves a MyCoRe group object from the persistent datastore.
   * @param groupID  a String representing the MyCoRe group object which is to be retrieved
   * @return         the requested group object
   */
  public synchronized MCRGroup retrieveGroup(String groupID) throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
    try
    {
      String select = "SELECT * FROM " + DB2GroupsTable + " WHERE GID = '" + groupID + "'";
      Statement statement = connection.getJDBCConnection().createStatement();
      ResultSet rs = statement.executeQuery(select);

      if(!rs.next())
      {
        String msg = "There is no group with ID = " + groupID;
        throw new MCRException(msg);
      }

      String creator     = rs.getString    ( 2);
      Timestamp created  = rs.getTimestamp ( 3);
      Timestamp modified = rs.getTimestamp ( 4);
      String description = rs.getString    ( 5);

      rs.close();

      // Now lookup the lists of admin users, admin groups, users (members)
      // and privileges

      select = "SELECT ADMINID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + groupID + "' AND TYPE = 'user'";
      Vector adminUsers = getSelectResult(select);

      select = "SELECT ADMINID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + groupID + "' AND TYPE = 'group'";
      Vector adminGroups = getSelectResult(select);

      select = "SELECT UID FROM " + DB2GroupMembersTable
             + " WHERE GID = '" + groupID + "'";
      Vector users = getSelectResult(select);

      select = "SELECT NAME FROM " + DB2PrivsLookupTable
             + " WHERE GID = '" + groupID + "'";
      Vector privs = getSelectResult(select);

      // We create the group object
      MCRGroup group = new MCRGroup(groupID, creator, created, modified, description,
        adminUsers, adminGroups, users, privs, "");

      return group;
    }
    finally{ connection.release(); }
  }

  /**
   * This method retrieves a MyCoRe privilege set from the persistent datastore.
   * @return  the Vector of known privileges of the system
   */
  public Vector retrievePrivilegeSet() throws Exception
  {
    Vector privileges = new Vector();
    MCRPrivilege thePrivilege;
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      String select = "SELECT * FROM " + DB2PrivilegesTable;
      Statement statement = connection.getJDBCConnection().createStatement();
      ResultSet rs = statement.executeQuery(select);

      while(rs.next()) {
        thePrivilege = new MCRPrivilege(rs.getString(1), rs.getString(2));
        privileges.add(thePrivilege);
      }
      rs.close();
      return privileges;
    }
    finally{ connection.release(); }
  }

  /**
   * This method updates a MyCoRe user object in the persistent datastore.
   * @param updUser    the user to be updated
   */
  public synchronized void updateUser(MCRUser updUser) throws Exception
  {
    String idEnabled = (updUser.isEnabled()) ? "true" : "false";
    String updateAllowed = (updUser.isUpdateAllowed()) ? "true" : "false";
    MCRUserAddress userAddress = updUser.getAddressObject();
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String update = "UPDATE " + DB2UsersTable
                    + " SET PASSWD = ? ,ENABLED = ? ,UPDATE = ? ,CREATOR = ? "
                    + " ,CREATIONDATE = ? ,LASTCHANGES = ? ,DESCRIPTION = ? "
                    + " ,SALUTATION = ? ,FIRSTNAME = ? ,LASTNAME = ? ,STREET = ? "
                    + " ,CITY = ? ,POSTALCODE = ? ,COUNTRY = ? ,INSTITUTION = ? "
                    + " ,FACULTY = ? ,DEPARTMENT = ? ,INSTITUTE = ? ,TELEPHONE = ? "
                    + " ,FAX = ? ,EMAIL = ? ,CELLPHONE = ? ,PRIMGROUP = ? "
                    + " WHERE UID = ?";

      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);

      statement.setString    ( 1, updUser.getPassword()        );
      statement.setString    ( 2, idEnabled                    );
      statement.setString    ( 3, updateAllowed                );
      statement.setString    ( 4, updUser.getCreator()         );
      statement.setTimestamp ( 5, updUser.getCreationDate()    );
      statement.setTimestamp ( 6, updUser.getLastChangesDate() );
      statement.setString    ( 7, updUser.getDescription()     );
      statement.setString    ( 8, userAddress.getSalutation()  );
      statement.setString    ( 9, userAddress.getFirstName()   );
      statement.setString    (10, userAddress.getLastName()    );
      statement.setString    (11, userAddress.getStreet()      );
      statement.setString    (12, userAddress.getCity()        );
      statement.setString    (13, userAddress.getPostalCode()  );
      statement.setString    (14, userAddress.getCountry()     );
      statement.setString    (15, userAddress.getInstitution() );
      statement.setString    (16, userAddress.getFaculty()     );
      statement.setString    (17, userAddress.getDepartment()  );
      statement.setString    (18, userAddress.getInstitute()   );
      statement.setString    (19, userAddress.getTelephone()   );
      statement.setString    (20, userAddress.getFax()         );
      statement.setString    (21, userAddress.getEmail()       );
      statement.setString    (22, userAddress.getCellphone()   );
      statement.setString    (23, updUser.getPrimaryGroup()    );
      statement.setString    (24, updUser.getID()              );

      statement.execute();
      statement.close();

      // Now we update the member lookup table. First we collect information about
      // which groups have been added or removed. Therefore we compare the list of
      // groups this user is a member of before and after the update.

      String select = "SELECT GID FROM " + DB2GroupMembersTable
                    + " WHERE UID = '" + updUser.getID() + "'";
      Vector oldGroups = getSelectResult(select);
      Vector newGroups = updUser.getGroups();

      // We search for the groups where this user is a new member and insert
      // the new user into the member lookup table

      String insert = "INSERT INTO " + DB2GroupMembersTable + " VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);
      for (int i=0; i<newGroups.size(); i++)
      {
        if (!oldGroups.contains((String)newGroups.elementAt(i))) {
          statement.setString ( 1, updUser.getID() );
          statement.setString ( 2, (String)newGroups.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();
      connection.getJDBCConnection().commit();
      connection.getJDBCConnection().setAutoCommit(true);

      // We search for the groups where this user has been removed from and
      // delete the entries from the member lookup table

      for (int i=0; i<oldGroups.size(); i++)
      {
        if (!newGroups.contains((String)oldGroups.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupMembersTable
                     + " WHERE UID = '" + updUser.getID()
                     + "' AND GID = '" + (String)oldGroups.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }
    }

    catch(Exception ex)
    {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw ex;
    }

    finally
    { connection.release(); }
  }

  /**
   * This method updates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object to be updated
   */
  public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) throws Exception
  {
    System.out.println("*** MCRUserStoreDB2.updatePrivilegeSet() is not yet implemented! ***");
  }

  /**
   * This method updates a MyCoRe group object in the persistent datastore.
   * @param group      the group to be updated
   */
  public synchronized void updateGroup(MCRGroup group) throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String update = "UPDATE " + DB2GroupsTable
                    + " SET CREATOR = ? ,CREATIONDATE = ? "
                    + " ,LASTCHANGES = ? ,DESCRIPTION = ? "
                    + " WHERE GID = ?";

      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);

      statement.setString    ( 1, group.getCreator()         );
      statement.setTimestamp ( 2, group.getCreationDate()    );
      statement.setTimestamp ( 3, group.getLastChangesDate() );
      statement.setString    ( 4, group.getDescription()     );
      statement.setString    ( 5, group.getID()              );

      statement.execute();
      statement.close();

      // Now we update the group admins table. First the admin users and
      // thereafter the admin groups. But first we collect information about
      // which admins have been added or removed from the list. In order to do
      // so we compare the lists of admins before and after the update.

      String select = "SELECT ADMINID FROM " + DB2GroupAdminsTable
                    + " WHERE GID = '" + group.getID() + "' AND TYPE = 'user'";
      Vector oldAdminUsers = getSelectResult(select);
      Vector newAdminUsers = group.getAdminUsers();

      select = "SELECT ADMINID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + group.getID() + "' AND TYPE = 'group'";
      Vector oldAdminGroups = getSelectResult(select);
      Vector newAdminGroups = group.getAdminGroups();

      // We search for the newly added admins and insert them into the table
      String insert = "INSERT INTO " + DB2GroupAdminsTable + " VALUES (?,?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newAdminUsers.size(); i++)
      {
        if (!oldAdminUsers.contains((String)newAdminUsers.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newAdminUsers.elementAt(i) );
          statement.setString ( 3, "user" );
          statement.execute();
          statement.clearParameters();
        }
      }

      for (int i=0; i<newAdminGroups.size(); i++)
      {
        if (!oldAdminGroups.contains((String)newAdminGroups.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newAdminGroups.elementAt(i) );
          statement.setString ( 3, "group" );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();

      // We do not need to update the membership lookup table. This is done
      // while creating/updating a user.

      // Now we update the privileges lookup table. First we collect information about
      // which privileges have been added or removed. Therefore we compare the list of
      // privileges this group has before and after the update.

      select = "SELECT NAME FROM " + DB2PrivsLookupTable
             + " WHERE GID = '" + group.getID() + "'";
      Vector oldPrivs = getSelectResult(select);
      Vector newPrivs = group.getPrivileges();

      // We search for the newly added privileges and insert them into the table
      insert = "INSERT INTO " + DB2PrivsLookupTable + " VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);
      for (int i=0; i<newPrivs.size(); i++)
      {
        if (!oldPrivs.contains((String)newPrivs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newPrivs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();
      connection.getJDBCConnection().commit();
      connection.getJDBCConnection().setAutoCommit(true);

      // We search for the recently removed admins and remove them from the table
      for (int i=0; i<oldAdminUsers.size(); i++)
      {
        if (!newAdminUsers.contains((String)oldAdminUsers.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupAdminsTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND ADMINID = '" + (String)oldAdminUsers.elementAt(i)
                     + "' AND TYPE = 'user'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      for (int i=0; i<oldAdminGroups.size(); i++)
      {
        if (!newAdminGroups.contains((String)oldAdminGroups.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupAdminsTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND ADMINID = '" + (String)oldAdminGroups.elementAt(i)
                     + "' AND TYPE = 'group'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      // We search for the recently removed privileges and remove them from the table
      for (int i=0; i<oldPrivs.size(); i++)
      {
        if (!newPrivs.contains((String)oldPrivs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2PrivsLookupTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND NAME = '" + (String)oldPrivs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }
    }

    catch(Exception ex)
    {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw ex;
    }

    finally
    { connection.release(); }
  }

  /**
   * This method checks whether all the user management tables exists in
   * the DB2 database.
   */
  private boolean tablesExist()
  {
    int number = MCRSQLConnection.justCountRows(
      "SYSCAT.TABLES WHERE TABNAME = '" + DB2UsersTable +
      "' OR TABNAME = '" + DB2GroupsTable +
      "' OR TABNAME = '" + DB2GroupMembersTable +
      "' OR TABNAME = '" + DB2GroupAdminsTable +
      "' OR TABNAME = '" + DB2PrivilegesTable +
      "' OR TABNAME = '" + DB2PrivsLookupTable + "'");
    return (number == 6);
  }

  /**
   * This method creates all necessary DB2 tables for the user management.
   */
  private void createTables()
  {
    MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      c.doUpdate (new MCRSQLStatement(DB2UsersTable)
       .addColumn("UID VARCHAR(20) NOT NULL")
       .addColumn("PASSWD VARCHAR(20) NOT NULL")
       .addColumn("ENABLED VARCHAR(8) NOT NULL")
       .addColumn("UPDATE VARCHAR(8) NOT NULL")
       .addColumn("CREATOR VARCHAR(20) NOT NULL")
       .addColumn("CREATIONDATE TIMESTAMP")
       .addColumn("LASTCHANGES TIMESTAMP")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("SALUTATION VARCHAR(25) NOT NULL")
       .addColumn("FIRSTNAME VARCHAR(25) NOT NULL")
       .addColumn("LASTNAME VARCHAR(25) NOT NULL")
       .addColumn("STREET VARCHAR(40)")
       .addColumn("CITY VARCHAR(25)")
       .addColumn("POSTALCODE VARCHAR(16)")
       .addColumn("COUNTRY VARCHAR(25)")
       .addColumn("INSTITUTION VARCHAR(64)")
       .addColumn("FACULTY VARCHAR(64)")
       .addColumn("DEPARTMENT VARCHAR(64)")
       .addColumn("INSTITUTE VARCHAR(64)")
       .addColumn("TELEPHONE VARCHAR(20) NOT NULL")
       .addColumn("FAX VARCHAR(20)")
       .addColumn("EMAIL VARCHAR(64) NOT NULL")
       .addColumn("CELLPHONE VARCHAR(20)")
       .addColumn("PRIMGROUP VARCHAR(20) NOT NULL")
       .addColumn("PRIMARY KEY(UID)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2GroupsTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("CREATOR VARCHAR(20) NOT NULL")
       .addColumn("CREATIONDATE TIMESTAMP")
       .addColumn("LASTCHANGES TIMESTAMP")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("PRIMARY KEY(GID)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2GroupMembersTable)
       .addColumn("UID VARCHAR(20) NOT NULL")
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("FOREIGN KEY (UID) REFERENCES "+ DB2UsersTable +" (UID) ON DELETE CASCADE")
       .addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID) ON DELETE CASCADE")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2GroupAdminsTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("ADMINID VARCHAR(20) NOT NULL")
       .addColumn("TYPE VARCHAR(6) NOT NULL")
       .addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID) ON DELETE CASCADE")
       //.addColumn("FOREIGN KEY (MEMBERID) REFERENCES "+ DB2UsersTable +" (UID) ON DELETE CASCADE")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2PrivilegesTable)
       .addColumn("NAME VARCHAR(32) NOT NULL")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("PRIMARY KEY(NAME)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2PrivsLookupTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("NAME VARCHAR(32) NOT NULL")
       .addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID) ON DELETE CASCADE")
       .addColumn("FOREIGN KEY (NAME) REFERENCES "+ DB2PrivilegesTable +" (NAME) ON DELETE CASCADE")
       .toCreateTableStatement());
    }
    finally {c.release();}
  }

  /**
   * This private method is a helper method and is called by many of the public methods
   * of this class. It takes a SELECT statement (which must be provided as a parameter)
   * and works this out on the database. This method is only applicable in the case that
   * only one vector of strings is requested as the result of the SELECT statement.
   *
   * @param select  String, SELECT statement to be carried out on the database
   * @return        Vector of strings - the result of the SELECT statement
   */
  private Vector getSelectResult(String select) throws Exception
  {
    Vector vec = new Vector();
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      Statement statement = connection.getJDBCConnection().createStatement();
      ResultSet rs = statement.executeQuery(select);

      while(rs.next())
        vec.add(rs.getString(1));

      rs.close();
      return vec;
    }
    finally{ connection.release(); }
  }
}
