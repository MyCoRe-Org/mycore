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
    MCRUserContact userContact = newUser.getContactInfo();
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      connection.getJDBCConnection().setAutoCommit(false);
      String insert = "INSERT INTO " + DB2UsersTable
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

      statement.setInt       ( 1, newUser.getNumID()          );
      statement.setString    ( 2, newUser.getID()             );
      statement.setString    ( 3, newUser.getCreator()        );
      statement.setTimestamp ( 4, newUser.getCreationDate()   );
      statement.setTimestamp ( 5, newUser.getModifiedDate()   );
      statement.setString    ( 6, newUser.getDescription()    );
      statement.setString    ( 7, newUser.getPassword()       );
      statement.setString    ( 8, idEnabled                   );
      statement.setString    ( 9, updateAllowed               );
      statement.setString    (10, userContact.getSalutation() );
      statement.setString    (11, userContact.getFirstName()  );
      statement.setString    (12, userContact.getLastName()   );
      statement.setString    (13, userContact.getStreet()     );
      statement.setString    (14, userContact.getCity()       );
      statement.setString    (15, userContact.getPostalCode() );
      statement.setString    (16, userContact.getCountry()    );
      statement.setString    (17, userContact.getInstitution());
      statement.setString    (18, userContact.getFaculty()    );
      statement.setString    (19, userContact.getDepartment() );
      statement.setString    (20, userContact.getInstitute()  );
      statement.setString    (21, userContact.getTelephone()  );
      statement.setString    (22, userContact.getFax()        );
      statement.setString    (23, userContact.getEmail()      );
      statement.setString    (24, userContact.getCellphone()  );
      statement.setString    (25, newUser.getPrimaryGroupID() );

      statement.execute();
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

      statement.setString    ( 1, newGroup.getID()           );
      statement.setString    ( 2, newGroup.getCreator()      );
      statement.setTimestamp ( 3, newGroup.getCreationDate() );
      statement.setTimestamp ( 4, newGroup.getModifiedDate() );
      statement.setString    ( 5, newGroup.getDescription()  );

      statement.execute();
      statement.close();

      // now update the member lookup table
      insert = "INSERT INTO " + DB2GroupMembersTable + "(GID, USERID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getMemberUserIDs().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getMemberUserIDs().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }

      statement.close();
      insert = "INSERT INTO " + DB2GroupMembersTable + "(GID, GROUPID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getMemberGroupIDs().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getMemberGroupIDs().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }

      statement.close();

      // now update the group admins table.
      insert = "INSERT INTO " + DB2GroupAdminsTable + "(GID, USERID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getAdminUserIDs().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getAdminUserIDs().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }

      statement.close();
      insert = "INSERT INTO " + DB2GroupAdminsTable + "(GID, GROUPID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroup.getAdminGroupIDs().size(); i++) {
        statement.setString ( 1, newGroup.getID() );
        statement.setString ( 2, (String)newGroup.getAdminGroupIDs().elementAt(i) );
        statement.execute();
        statement.clearParameters();
      }
      statement.close();

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
    String sql = "DELETE FROM " + DB2UsersTable + " WHERE UID = '" + delUserID + "'";
    MCRSQLConnection.justDoUpdate(sql);
  }

  /**
   * This method deletes a MyCoRe group object in the persistent datastore.
   * @param delGroupID   a String representing the MyCoRe group object which is to be deleted
   */
  public synchronized void deleteGroup(String delGroupID) throws Exception
  {
    // We need to update the group table, the admin-lookup table, the member-lookup Table and
    // the privilege lookup table (removing all the references to this group object).

    String sql = "DELETE FROM " + DB2GroupsTable + " WHERE GID = '" + delGroupID + "'";
    MCRSQLConnection.justDoUpdate(sql);

    sql = "DELETE FROM " + DB2GroupAdminsTable + " WHERE GID = '" + delGroupID + "'";
    MCRSQLConnection.justDoUpdate(sql);

    sql = "DELETE FROM " + DB2GroupMembersTable + " WHERE GID = '" + delGroupID + "'";
    MCRSQLConnection.justDoUpdate(sql);

    sql = "DELETE FROM " + DB2PrivsLookupTable + " WHERE GID = '" + delGroupID + "'";
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
   * This method tests if a MyCoRe user object is available in the persistent datastore.
   * The numerical userID is taken into account, too.
   *
   * @param numID         (int) numerical userID of the MyCoRe user object
   * @param userID        a String representing the MyCoRe user object which is to be looked for
   */
  public synchronized boolean existsUser(int numID, String userID) throws Exception
  {
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
    ResultSet rs = null;

    try {
      String select = "SELECT * FROM " + DB2UsersTable + " WHERE NUMID = " + numID + " OR UID = '" + userID + "'";
      Statement statement = connection.getJDBCConnection().createStatement();
      rs = statement.executeQuery(select);

      if (rs.next())
        return true;
      else return false;
    }

    finally {
      rs.close();
      connection.release();
    }
  }

  /**
   * This method tests if a MyCoRe privilege object is available in the persistent datastore.
   * @param privName  a String representing the MyCoRe privilege object which is to be looked for
   */
  public synchronized boolean existsPrivilege(String privName) throws Exception
  {
    return MCRSQLConnection.justCheckExists(new MCRSQLStatement(DB2PrivilegesTable)
      .setCondition("NAME", privName)
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
   * This method gets all group IDs where a given user ID can manage the group (i.e. is
   * in the administrator user IDs list) as a vector of strings.
   *
   * @param userID   a String representing the administrative user
   * @return         Vector of strings including the group IDs of the system which
   *                 have userID in their administrators list
   */
  public synchronized Vector getGroupIDsWithAdminUser(String userID) throws Exception
  {
    String select = "SELECT GID FROM " + DB2GroupAdminsTable + " WHERE USERID='" + userID + "'";
    return getSelectResult(select);
  }

  /**
   * This method gets all user IDs with a given primary group and returns them as a
   * Vector of strings.
   *
   * @param groupID  a String representing a primary Group
   * @return         Vector of strings including the user IDs of the system which
   *                 have groupID as primary group
   */
  public synchronized Vector getUserIDsWithPrimaryGroup(String groupID) throws Exception
  {
    String select = "SELECT UID FROM " + DB2UsersTable + " WHERE PRIMGROUP='" + groupID + "'";
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
        String msg = "MCRUserStoreDB2.retrieveUser(): There is no user with ID = " + userID;
        throw new MCRException(msg);
      }

      int numID             = rs.getInt       ( 1);
      String creator        = rs.getString    ( 3);
      Timestamp created     = rs.getTimestamp ( 4);
      Timestamp modified    = rs.getTimestamp ( 5);
      String description    = rs.getString    ( 6);
      String passwd         = rs.getString    ( 7);
      String idEnabled      = rs.getString    ( 8);
      String updateAllowed  = rs.getString    ( 9);
      String salutation     = rs.getString    (10);
      String firstname      = rs.getString    (11);
      String lastname       = rs.getString    (12);
      String street         = rs.getString    (13);
      String city           = rs.getString    (14);
      String postalcode     = rs.getString    (15);
      String country        = rs.getString    (16);
      String institution    = rs.getString    (17);
      String faculty        = rs.getString    (18);
      String department     = rs.getString    (19);
      String institute      = rs.getString    (20);
      String telephone      = rs.getString    (21);
      String fax            = rs.getString    (22);
      String email          = rs.getString    (23);
      String cellphone      = rs.getString    (24);
      String primaryGroupID = rs.getString    (25);

      rs.close();

      // Now lookup the groups this user is a member of
      select = "SELECT GID FROM " + DB2GroupMembersTable + " WHERE USERID = '" + userID + "'";
      Vector groups = getSelectResult(select);

      // set some boolean values
      boolean id_enabled = (idEnabled.equals("true")) ? true : false;
      boolean update_allowed = (updateAllowed.equals("true")) ? true : false;

      // We create the user object
      MCRUser user = new MCRUser(numID, userID, id_enabled, update_allowed, creator, created,
        modified, description, passwd, salutation, firstname, lastname, street, city, postalcode,
        country, institution, faculty, department, institute, telephone, fax, email, cellphone,
        primaryGroupID, groups, "");

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
        String msg = "MCRUserStoreDB2.retrieveGroup(): There is no group with ID = " + groupID;
        throw new MCRException(msg);
      }

      String creator     = rs.getString    ( 2);
      Timestamp created  = rs.getTimestamp ( 3);
      Timestamp modified = rs.getTimestamp ( 4);
      String description = rs.getString    ( 5);

      rs.close();

      // Now lookup the lists of admin users, admin groups, users (members)
      // and privileges

      select = "SELECT USERID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + groupID + "' AND USERID IS NOT NULL";
      Vector admUserIDs = getSelectResult(select);

      select = "SELECT GROUPID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + groupID + "' AND GROUPID IS NOT NULL";
      Vector admGroupIDs = getSelectResult(select);

      select = "SELECT USERID FROM " + DB2GroupMembersTable
             + " WHERE GID = '" + groupID + "' AND USERID IS NOT NULL";
      Vector mbrUserIDs = getSelectResult(select);

      select = "SELECT GROUPID FROM " + DB2GroupMembersTable
             + " WHERE GID = '" + groupID + "' AND GROUPID IS NOT NULL";
      Vector mbrGroupIDs = getSelectResult(select);

      select = "SELECT GID FROM " + DB2GroupMembersTable
             + " WHERE GROUPID = '" + groupID + "'";
      Vector groupIDs = getSelectResult(select);

      select = "SELECT NAME FROM " + DB2PrivsLookupTable
             + " WHERE GID = '" + groupID + "'";
      Vector privs = getSelectResult(select);

      // We create the group object
      MCRGroup group = new MCRGroup(groupID, creator, created, modified, description,
        admUserIDs, admGroupIDs, mbrUserIDs, mbrGroupIDs, groupIDs, privs, "");

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
    MCRUserContact userContact = updUser.getContactInfo();
    MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

    try
    {
      String idEnabled = (updUser.isEnabled()) ? "true" : "false";
      String updateAllowed = (updUser.isUpdateAllowed()) ? "true" : "false";

      connection.getJDBCConnection().setAutoCommit(false);
      String update = "UPDATE " + DB2UsersTable
                    + " SET CREATOR = ? ,CREATIONDATE = ? ,MODIFIEDDATE = ? ,DESCRIPTION = ? "
                    + " ,PASSWD = ? ,ENABLED = ? ,UPD = ? ,SALUTATION = ? ,FIRSTNAME = ? "
                    + " ,LASTNAME = ? ,STREET = ? ,CITY = ? ,POSTALCODE = ? ,COUNTRY = ? "
                    + " ,INSTITUTION = ? ,FACULTY = ? ,DEPARTMENT = ? ,INSTITUTE = ? "
                    + " ,TELEPHONE = ? ,FAX = ? ,EMAIL = ? ,CELLPHONE = ? ,PRIMGROUP = ? "
                    + " WHERE UID = ?";

      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);

      statement.setString    ( 1, updUser.getCreator()        );
      statement.setTimestamp ( 2, updUser.getCreationDate()   );
      statement.setTimestamp ( 3, updUser.getModifiedDate()   );
      statement.setString    ( 4, updUser.getDescription()    );
      statement.setString    ( 5, updUser.getPassword()       );
      statement.setString    ( 6, idEnabled                   );
      statement.setString    ( 7, updateAllowed               );
      statement.setString    ( 8, userContact.getSalutation() );
      statement.setString    ( 9, userContact.getFirstName()  );
      statement.setString    (10, userContact.getLastName()   );
      statement.setString    (11, userContact.getStreet()     );
      statement.setString    (12, userContact.getCity()       );
      statement.setString    (13, userContact.getPostalCode() );
      statement.setString    (14, userContact.getCountry()    );
      statement.setString    (15, userContact.getInstitution());
      statement.setString    (16, userContact.getFaculty()    );
      statement.setString    (17, userContact.getDepartment() );
      statement.setString    (18, userContact.getInstitute()  );
      statement.setString    (19, userContact.getTelephone()  );
      statement.setString    (20, userContact.getFax()        );
      statement.setString    (21, userContact.getEmail()      );
      statement.setString    (22, userContact.getCellphone()  );
      statement.setString    (23, updUser.getPrimaryGroupID() );
      statement.setString    (24, updUser.getID()             );

      statement.execute();
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
   * This method updates a MyCoRe privilege set object in the persistent datastore.
   * At the moment we only insert *new* privileges, we do not overwrite existent
   * privileges and we do not delete any privilege.
   *
   * @param privilegeSet the privilege set object to be updated
   */
  public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) throws Exception
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
        if (!existsPrivilege(thePrivilege.getName())) {
          statement.setString ( 1, (String)thePrivilege.getName() );
          statement.setString ( 2, (String)thePrivilege.getDescription() );
          statement.execute();
          statement.clearParameters();
        }
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
                    + " SET CREATOR = ? ,CREATIONDATE = ? ,MODIFIEDDATE = ? , DESCRIPTION = ? "
                    + " WHERE GID = ?";

      PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);

      statement.setString    ( 1, group.getCreator()      );
      statement.setTimestamp ( 2, group.getCreationDate() );
      statement.setTimestamp ( 3, group.getModifiedDate() );
      statement.setString    ( 4, group.getDescription()  );
      statement.setString    ( 5, group.getID()           );

      statement.execute();
      statement.close();

      // Now we update the group admins table. First the admin users and thereafter the admin
      // groups. But first we collect information about which admins have been added or removed
      // from the list. In order to do so we compare the lists of admins before and after the update.

      String select = "SELECT USERID FROM " + DB2GroupAdminsTable
                    + " WHERE GID = '" + group.getID() + "' AND USERID IS NOT NULL";
      Vector oldAdminUserIDs = getSelectResult(select);
      Vector newAdminUserIDs = group.getAdminUserIDs();

      select = "SELECT GROUPID FROM " + DB2GroupAdminsTable
             + " WHERE GID = '" + group.getID() + "' AND GROUPID IS NOT NULL";
      Vector oldAdminGroupIDs = getSelectResult(select);
      Vector newAdminGroupIDs = group.getAdminGroupIDs();

      // We search for the newly added admins and insert them into the table
      String insert = "INSERT INTO " + DB2GroupAdminsTable + "(GID, USERID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newAdminUserIDs.size(); i++)
      {
        if (!oldAdminUserIDs.contains((String)newAdminUserIDs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newAdminUserIDs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }

      statement.close();
      insert = "INSERT INTO " + DB2GroupAdminsTable + "(GID, GROUPID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newAdminGroupIDs.size(); i++)
      {
        if (!oldAdminGroupIDs.contains((String)newAdminGroupIDs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newAdminGroupIDs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();

      // We search for the recently removed admins and remove them from the table
      for (int i=0; i<oldAdminUserIDs.size(); i++)
      {
        if (!newAdminUserIDs.contains((String)oldAdminUserIDs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupAdminsTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND USERID = '" + (String)oldAdminUserIDs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      for (int i=0; i<oldAdminGroupIDs.size(); i++)
      {
        if (!newAdminGroupIDs.contains((String)oldAdminGroupIDs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupAdminsTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND GROUPID = '" + (String)oldAdminGroupIDs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      // Now we update the membership lookup table. First we collect information about
      // which users have been added or removed. Therefore we compare the list of users
      // this group has as members before and after the update.

      select = "SELECT USERID FROM " + DB2GroupMembersTable
             + " WHERE GID = '" + group.getID() + "' AND USERID IS NOT NULL";
      Vector oldUserIDs = getSelectResult(select);
      Vector newUserIDs = group.getMemberUserIDs();

      // We search for the new members and insert them into the lookup table
      insert = "INSERT INTO " + DB2GroupMembersTable + "(GID, USERID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newUserIDs.size(); i++)
      {
        if (!oldUserIDs.contains((String)newUserIDs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newUserIDs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();

      // We search for the users which have been removed from this group and delete the
      // entries from the member lookup table

      for (int i=0; i<oldUserIDs.size(); i++)
      {
        if (!newUserIDs.contains((String)oldUserIDs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupMembersTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND USERID = '" + (String)oldUserIDs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      // Now we collect information about which groups have been added or removed.
      // Therefore we compare the list of groups this group has as members before and
      // after the update.

      select = "SELECT GROUPID FROM " + DB2GroupMembersTable
             + " WHERE GID = '" + group.getID() + "' AND GROUPID IS NOT NULL";
      Vector oldGroupIDs = getSelectResult(select);
      Vector newGroupIDs = group.getMemberGroupIDs();

      // We search for the new members and insert them into the lookup table
      insert = "INSERT INTO " + DB2GroupMembersTable + "(GID, GROUPID) VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newGroupIDs.size(); i++)
      {
        if (!oldGroupIDs.contains((String)newGroupIDs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newGroupIDs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();

      // We search for the groups which have been removed from this group and delete the
      // entries from the member lookup table

      for (int i=0; i<oldGroupIDs.size(); i++)
      {
        if (!newGroupIDs.contains((String)oldGroupIDs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2GroupMembersTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND GROUPID = '" + (String)oldGroupIDs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

      // Now we collect information about which privileges have been added or removed.
      // Therefor we compare the list of privileges this group has before and after
      // the update.

      select = "SELECT NAME FROM " + DB2PrivsLookupTable
             + " WHERE GID = '" + group.getID() + "'";
      Vector oldPrivs = getSelectResult(select);
      Vector newPrivs = group.getPrivileges();

      // We search for new privileges and insert them into the lookup table
      insert = "INSERT INTO " + DB2PrivsLookupTable + " VALUES (?,?)";
      statement = connection.getJDBCConnection().prepareStatement(insert);

      for (int i=0; i<newPrivs.size(); i++) {
        if (!oldPrivs.contains((String)newPrivs.elementAt(i))) {
          statement.setString ( 1, group.getID() );
          statement.setString ( 2, (String)newPrivs.elementAt(i) );
          statement.execute();
          statement.clearParameters();
        }
      }
      statement.close();

      // We search for the privileges which have been removed from this group and delete the
      // entries from the privilege lookup table

      for (int i=0; i<oldPrivs.size(); i++)
      {
        if (!newPrivs.contains((String)oldPrivs.elementAt(i))) {
          String sql = "DELETE FROM " + DB2PrivsLookupTable
                     + " WHERE GID = '" + group.getID()
                     + "' AND NAME = '" + (String)oldPrivs.elementAt(i) + "'";
          MCRSQLConnection.justDoUpdate(sql);
        }
      }

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
   * This method checks whether all the user management tables exists in
   * the DB2 database.
   */
  private boolean tablesExist()
  {
    int number = MCRSQLConnection.justCountRows(
      "SYSCAT.TABLES WHERE TABNAME = '" + DB2UsersTable +
      "' OR TABNAME = '" + DB2GroupsTable +
      "' OR TABNAME = '" + DB2GroupAdminsTable +
      "' OR TABNAME = '" + DB2GroupMembersTable +
      "' OR TABNAME = '" + DB2PrivilegesTable +
      "' OR TABNAME = '" + DB2PrivsLookupTable + "'");
    return (number == 6);
/*
    MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
    try
    {
      // At the moment this is a workaround. Since MySQL does not provide certain
      // SQL system information like SYSCAT.TABLES we have to produce an SQL error
      // in order to find out if the table exists or not. Therefore we simply produce
      // an SQLException if the table does not yet exist.

      String select = "SELECT COUNT(*) FROM " + DB2UsersTable;
      Statement statement = c.getJDBCConnection().createStatement();
      ResultSet rs = statement.executeQuery(select);
      rs.close();

      select = "SELECT COUNT(*) FROM " + DB2GroupsTable;
      statement = c.getJDBCConnection().createStatement();
      rs = statement.executeQuery(select);
      rs.close();

      select = "SELECT COUNT(*) FROM " + DB2GroupAdminsTable;
      statement = c.getJDBCConnection().createStatement();
      rs = statement.executeQuery(select);
      rs.close();

      select = "SELECT COUNT(*) FROM " + DB2GroupMembersTable;
      statement = c.getJDBCConnection().createStatement();
      rs = statement.executeQuery(select);
      rs.close();

      select = "SELECT COUNT(*) FROM " + DB2PrivilegesTable;
      statement = c.getJDBCConnection().createStatement();
      rs = statement.executeQuery(select);
      rs.close();

      return true;  // ok, obviously the tables exists (there is no SQLException)

    }

    catch (SQLException e) { return false; } // the table does not exist!
    finally { c.release(); }
*/
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
       .addColumn("NUMID INTEGER NOT NULL")
       .addColumn("UID VARCHAR(20) NOT NULL")
       .addColumn("CREATOR VARCHAR(20) NOT NULL")
       .addColumn("CREATIONDATE TIMESTAMP")
       .addColumn("MODIFIEDDATE TIMESTAMP")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("PASSWD VARCHAR(20) NOT NULL")
       .addColumn("ENABLED VARCHAR(8) NOT NULL")
       .addColumn("UPD VARCHAR(8) NOT NULL")
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
       .addColumn("UNIQUE (NUMID)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2GroupsTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("CREATOR VARCHAR(20) NOT NULL")
       .addColumn("CREATIONDATE TIMESTAMP")
       .addColumn("MODIFIEDDATE TIMESTAMP")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("PRIMARY KEY(GID)")
       .toCreateTableStatement());

       c.doUpdate (new MCRSQLStatement(DB2GroupAdminsTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("USERID VARCHAR(20)")
       .addColumn("GROUPID VARCHAR(20)")
       //.addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID)")
       //.addColumn("FOREIGN KEY (USERID) REFERENCES "+ DB2UsersTable +" (UID)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2GroupMembersTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("USERID VARCHAR(20)")
       .addColumn("GROUPID VARCHAR(20)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2PrivilegesTable)
       .addColumn("NAME VARCHAR(64) NOT NULL")
       .addColumn("DESCRIPTION VARCHAR(200)")
       .addColumn("PRIMARY KEY(NAME)")
       .toCreateTableStatement());

      c.doUpdate (new MCRSQLStatement(DB2PrivsLookupTable)
       .addColumn("GID VARCHAR(20) NOT NULL")
       .addColumn("NAME VARCHAR(32) NOT NULL")
       .addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID)")
       .addColumn("FOREIGN KEY (NAME) REFERENCES "+ DB2PrivilegesTable +" (NAME)")
       //.addColumn("FOREIGN KEY (GID) REFERENCES "+ DB2GroupsTable +" (GID) ON DELETE CASCADE")
       //.addColumn("FOREIGN KEY (NAME) REFERENCES "+ DB2PrivilegesTable +" (NAME) ON DELETE CASCADE")
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
