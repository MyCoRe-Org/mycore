/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.user2.MCRGroup;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserContact;
import org.mycore.user2.MCRUserStore;

/**
 * This class implements the interface MCRUserStore and uses SQL tables for
 * persistent storage of MyCoRe user, group and privileges information,
 * respectively.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRSQLUserStore2 implements MCRUserStore {
    /** the logger */
    static Logger logger = Logger.getLogger(MCRSQLUserStore2.class.getName());

    /** name of the sql table containing user information */
    private String SQLUsersTable;

    /** name of the sql table containing group information */
    private String SQLGroupsTable;

    /** name of the sql table containing user and group membership information */
    private String SQLGroupMembersTable;

    /** name of the sql table containing group admin information */
    private String SQLGroupAdminsTable;

    /**
     * The constructor reads the names of the SQL tables which hold the user
     * information data from mycore.properties. The existence of the tables is
     * checked. If the tables do not yet exist they will be created.
     */
    public MCRSQLUserStore2() {
        // set configuration
        MCRConfiguration config = MCRConfiguration.instance();
        SQLUsersTable = config.getString("MCR.users_store_sql_table_users", "MCRUSERS");
        SQLGroupsTable = config.getString("MCR.users_store_sql_table_groups", "MCRGROUPS");
        SQLGroupMembersTable = config.getString("MCR.users_store_sql_table_group_members", "MCRGROUPMEMBERS");
        SQLGroupAdminsTable = config.getString("MCR.users_store_sql_table_group_admins", "MCRGROUPADMINS");

        // create tables
        if (!MCRSQLConnection.doesTableExist(SQLUsersTable)) {
            logger.info("Create table " + SQLUsersTable);
            createSQLUsersTable();
            logger.info("Done.");
        }

        if (!MCRSQLConnection.doesTableExist(SQLGroupsTable)) {
            logger.info("Create table " + SQLGroupsTable);
            createSQLGroupsTable();
            logger.info("Done.");
        }

        if (!MCRSQLConnection.doesTableExist(SQLGroupMembersTable)) {
            logger.info("Create table " + SQLGroupMembersTable);
            createSQLGroupMembersTable();
            logger.info("Done.");
        }

        if (!MCRSQLConnection.doesTableExist(SQLGroupAdminsTable)) {
            logger.info("Create table " + SQLGroupAdminsTable);
            createSQLGroupAdminsTable();
            logger.info("Done.");
        }

    }

    /**
     * This method creates the table named SQLUsersTable.
     */
    private final void createSQLUsersTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLUsersTable).addColumn("NUMID INTEGER NOT NULL").addColumn("UID VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("CREATOR VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("CREATIONDATE TIMESTAMP").addColumn("MODIFIEDDATE TIMESTAMP").addColumn(
                    "DESCRIPTION VARCHAR(" + Integer.toString(MCRUser.description_len) + ")").addColumn("PASSWD VARCHAR(" + Integer.toString(MCRUser.password_len) + ") NOT NULL").addColumn("ENABLED VARCHAR(8) NOT NULL").addColumn("UPD VARCHAR(8) NOT NULL").addColumn("SALUTATION VARCHAR(" + Integer.toString(MCRUserContact.salutation_len) + ")").addColumn(
                    "FIRSTNAME VARCHAR(" + Integer.toString(MCRUserContact.firstname_len) + ")").addColumn("LASTNAME VARCHAR(" + Integer.toString(MCRUserContact.lastname_len) + ")").addColumn("STREET VARCHAR(" + Integer.toString(MCRUserContact.street_len) + ")").addColumn("CITY VARCHAR(" + Integer.toString(MCRUserContact.city_len) + ")").addColumn(
                    "POSTALCODE VARCHAR(" + Integer.toString(MCRUserContact.postalcode_len) + ")").addColumn("COUNTRY VARCHAR(" + Integer.toString(MCRUserContact.country_len) + ")").addColumn("STATE VARCHAR(" + Integer.toString(MCRUserContact.state_len) + ")").addColumn("INSTITUTION VARCHAR(" + Integer.toString(MCRUserContact.institution_len) + ")").addColumn(
                    "FACULTY VARCHAR(" + Integer.toString(MCRUserContact.faculty_len) + ")").addColumn("DEPARTMENT VARCHAR(" + Integer.toString(MCRUserContact.department_len) + ")").addColumn("INSTITUTE VARCHAR(" + Integer.toString(MCRUserContact.institute_len) + ")").addColumn("TELEPHONE VARCHAR(" + Integer.toString(MCRUserContact.telephone_len) + ")").addColumn(
                    "FAX VARCHAR(" + Integer.toString(MCRUserContact.fax_len) + ")").addColumn("EMAIL VARCHAR(" + Integer.toString(MCRUserContact.email_len) + ")").addColumn("CELLPHONE VARCHAR(" + Integer.toString(MCRUserContact.cellphone_len) + ")").addColumn("PRIMGROUP VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("PRIMARY KEY(UID)").addColumn("UNIQUE (NUMID)")
                    .toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLUsersTable).addColumn("UID").toIndexStatement());
        } finally {
            c.release();
        }
    }

    /**
     * This method creates the table named SQLGroupsTable.
     */
    private final void createSQLGroupsTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLGroupsTable).addColumn("GID VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("CREATOR VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("CREATIONDATE TIMESTAMP").addColumn("MODIFIEDDATE TIMESTAMP").addColumn("DESCRIPTION VARCHAR(" + Integer.toString(MCRUser.description_len) + ")")
                    .addColumn("PRIMARY KEY(GID)").toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLGroupsTable).addColumn("GID").toIndexStatement());
        } finally {
            c.release();
        }
    }

    /**
     * This method creates the table named SQLGroupAdminsTable
     */
    private final void createSQLGroupAdminsTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLGroupAdminsTable).addColumn("GID VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("USERID VARCHAR(" + Integer.toString(MCRUser.id_len) + ")").addColumn("GROUPID VARCHAR(" + Integer.toString(MCRUser.id_len) + ")").toCreateTableStatement());
        } finally {
            c.release();
        }
    }

    /**
     * This method creates the table named SQLGroupMembersTable.
     */
    private final void createSQLGroupMembersTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLGroupMembersTable).addColumn("GID VARCHAR(" + Integer.toString(MCRUser.id_len) + ") NOT NULL").addColumn("USERID VARCHAR(" + Integer.toString(MCRUser.id_len) + ")").toCreateTableStatement());
        } finally {
            c.release();
        }
    }

    /**
     * This method creates a MyCoRe user object in the persistent datastore.
     * 
     * @param newUser
     *            the new user object to be stored
     */
    public synchronized void createUser(MCRUser newUser) throws MCRException {
        String idEnabled = (newUser.isEnabled()) ? "true" : "false";
        String updateAllowed = (newUser.isUpdateAllowed()) ? "true" : "false";
        MCRUserContact userContact = newUser.getUserContact();
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            connection.getJDBCConnection().setAutoCommit(false);

            String insert = "INSERT INTO " + SQLUsersTable + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

            statement.setInt(1, newUser.getNumID());
            statement.setString(2, newUser.getID());
            statement.setString(3, newUser.getCreator());
            statement.setTimestamp(4, newUser.getCreationDate());
            statement.setTimestamp(5, newUser.getModifiedDate());
            statement.setString(6, newUser.getDescription());
            statement.setString(7, newUser.getPassword());
            statement.setString(8, idEnabled);
            statement.setString(9, updateAllowed);
            statement.setString(10, userContact.getSalutation());
            statement.setString(11, userContact.getFirstName());
            statement.setString(12, userContact.getLastName());
            statement.setString(13, userContact.getStreet());
            statement.setString(14, userContact.getCity());
            statement.setString(15, userContact.getPostalCode());
            statement.setString(16, userContact.getCountry());
            statement.setString(17, userContact.getState());
            statement.setString(18, userContact.getInstitution());
            statement.setString(19, userContact.getFaculty());
            statement.setString(20, userContact.getDepartment());
            statement.setString(21, userContact.getInstitute());
            statement.setString(22, userContact.getTelephone());
            statement.setString(23, userContact.getFax());
            statement.setString(24, userContact.getEmail());
            statement.setString(25, userContact.getCellphone());
            statement.setString(26, newUser.getPrimaryGroupID());

            statement.execute();
            statement.close();

            connection.getJDBCConnection().commit();
            connection.getJDBCConnection().setAutoCommit(true);
        } catch (Exception ex) {
            try {
                connection.getJDBCConnection().rollback();
            } catch (SQLException ignored) {
            }

            logger.error("Error in Userstore", ex);
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This method creates a MyCoRe group object in the persistent datastore.
     * 
     * @param newGroup
     *            the new group object to be stored
     */
    public synchronized void createGroup(MCRGroup newGroup) throws MCRException {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            connection.getJDBCConnection().setAutoCommit(false);

            String insert = "INSERT INTO " + SQLGroupsTable + " VALUES (?,?,?,?,?)";
            PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);

            statement.setString(1, newGroup.getID());
            statement.setString(2, newGroup.getCreator());
            statement.setTimestamp(3, newGroup.getCreationDate());
            statement.setTimestamp(4, newGroup.getModifiedDate());
            statement.setString(5, newGroup.getDescription());

            statement.execute();
            statement.close();

            // now update the member lookup table
            insert = "INSERT INTO " + SQLGroupMembersTable + "(GID, USERID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newGroup.getMemberUserIDs().size(); i++) {
                statement.setString(1, newGroup.getID());
                statement.setString(2, (String) newGroup.getMemberUserIDs().get(i));
                statement.execute();
                statement.clearParameters();
            }

            statement.close();

            // now update the group admins table.
            insert = "INSERT INTO " + SQLGroupAdminsTable + "(GID, USERID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newGroup.getAdminUserIDs().size(); i++) {
                statement.setString(1, newGroup.getID());
                statement.setString(2, (String) newGroup.getAdminUserIDs().get(i));
                statement.execute();
                statement.clearParameters();
            }

            statement.close();
            insert = "INSERT INTO " + SQLGroupAdminsTable + "(GID, GROUPID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newGroup.getAdminGroupIDs().size(); i++) {
                statement.setString(1, newGroup.getID());
                statement.setString(2, (String) newGroup.getAdminGroupIDs().get(i));
                statement.execute();
                statement.clearParameters();
            }

            statement.close();

            connection.getJDBCConnection().commit();
            connection.getJDBCConnection().setAutoCommit(true);
        } catch (Exception ex) {
            try {
                connection.getJDBCConnection().rollback();
            } catch (SQLException ignored) {
            }

            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This method deletes a MyCoRe user object from the persistent datastore.
     * 
     * @param delUserID
     *            a String representing the MyCoRe user object which is to be
     *            deleted
     */
    public synchronized void deleteUser(String delUserID) throws MCRException {
        try {
            String sql = "DELETE FROM " + SQLUsersTable + " WHERE UID = '" + delUserID + "'";
            MCRSQLConnection.justDoUpdate(sql);
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        }
    }

    /**
     * This method deletes a MyCoRe group object in the persistent datastore.
     * 
     * @param delGroupID
     *            a String representing the MyCoRe group object which is to be
     *            deleted
     */
    public synchronized void deleteGroup(String delGroupID) throws MCRException {
        try {
            // We need to update the group table, the admin-lookup table, the
            // member-lookup Table and
            // the privilege lookup table (removing all the references to this
            // group object).
            String sql = "DELETE FROM " + SQLGroupsTable + " WHERE GID = '" + delGroupID + "'";
            MCRSQLConnection.justDoUpdate(sql);

            sql = "DELETE FROM " + SQLGroupAdminsTable + " WHERE GID = '" + delGroupID + "'";
            MCRSQLConnection.justDoUpdate(sql);

            sql = "DELETE FROM " + SQLGroupMembersTable + " WHERE GID = '" + delGroupID + "'";
            MCRSQLConnection.justDoUpdate(sql);

            MCRSQLConnection.justDoUpdate(sql);
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        }
    }

    /**
     * This method tests if a MyCoRe user object is available in the persistent
     * datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            looked for
     */
    public synchronized boolean existsUser(String userID) throws MCRException {
        try {
            return MCRSQLConnection.justCheckExists(new MCRSQLStatement(SQLUsersTable).setCondition("UID", userID).toRowSelector());
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        }
    }

    /**
     * This method tests if a MyCoRe user object is available in the persistent
     * datastore. The numerical userID is taken into account, too.
     * 
     * @param numID
     *            (int) numerical userID of the MyCoRe user object
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            looked for
     */
    public synchronized boolean existsUser(int numID, String userID) throws MCRException {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
        ResultSet rs = null;

        try {
            String select = "SELECT * FROM " + SQLUsersTable + " WHERE NUMID = " + numID + " OR UID = '" + userID + "'";
            Statement statement = connection.getJDBCConnection().createStatement();
            rs = statement.executeQuery(select);

            return rs.next();
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            try {
                rs.close();
                connection.release();
            } catch (Exception ex) {
                throw new MCRException("Error in UserStore.", ex);
            }
        }
    }

    /**
     * This method tests if a MyCoRe group object is available in the persistent
     * datastore.
     * 
     * @param groupID
     *            a String representing the MyCoRe group object which is to be
     *            looked for
     */
    public synchronized boolean existsGroup(String groupID) throws MCRException {
        try {
            return MCRSQLConnection.justCheckExists(new MCRSQLStatement(SQLGroupsTable).setCondition("GID", groupID).toRowSelector());
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        }
    }

    /**
     * This method gets all user IDs and returns them as a ArrayList of strings.
     * 
     * @return ArrayList of strings including the user IDs of the system
     */
    public synchronized ArrayList getAllUserIDs() throws MCRException {
        String select = "SELECT UID FROM " + SQLUsersTable;

        return getSelectResult(select);
    }

    /**
     * This method gets all group IDs and returns them as a ArrayList of
     * strings.
     * 
     * @return ArrayList of strings including the group IDs of the system
     */
    public synchronized ArrayList getAllGroupIDs() throws MCRException {
        String select = "SELECT GID FROM " + SQLGroupsTable;

        return getSelectResult(select);
    }

    /**
     * This method gets all group IDs where a given user ID can manage the group
     * (i.e. is in the administrator user IDs list) as a ArrayList of strings.
     * 
     * @param userID
     *            a String representing the administrative user
     * @return ArrayList of strings including the group IDs of the system which
     *         have userID in their administrators list
     */
    public synchronized ArrayList getGroupIDsWithAdminUser(String userID) throws MCRException {
        String select = "SELECT GID FROM " + SQLGroupAdminsTable + " WHERE USERID='" + userID + "'";

        return getSelectResult(select);
    }

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public synchronized int getMaxUserNumID() throws MCRException {
        String select = "SELECT MAX(NUMID) FROM " + SQLUsersTable;
        String nr = MCRSQLConnection.justGetSingleValue(select);
        int iMax = Integer.parseInt(nr);

        return iMax;
    }

    /**
     * This method gets all user IDs with a given primary group and returns them
     * as a ArrayList of strings.
     * 
     * @param groupID
     *            a String representing a primary Group
     * @return ArrayList of strings including the user IDs of the system which
     *         have groupID as primary group
     */
    public synchronized ArrayList getUserIDsWithPrimaryGroup(String groupID) throws MCRException {
        String select = "SELECT UID FROM " + SQLUsersTable + " WHERE PRIMGROUP='" + groupID + "'";

        return getSelectResult(select);
    }

    /**
     * This method retrieves a MyCoRe user object from the persistent datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            retrieved
     * @return the requested user object
     */
    public synchronized MCRUser retrieveUser(String userID) throws MCRException {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            String select = "SELECT * FROM " + SQLUsersTable + " WHERE UID = '" + userID + "'";
            Statement statement = connection.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (!rs.next()) {
                String msg = "MCRSQLUserStore.retrieveUser(): There is no user with ID = " + userID;
                throw new MCRException(msg);
            }

            int numID = rs.getInt(1);
            String creator = rs.getString(3);
            Timestamp created = rs.getTimestamp(4);
            Timestamp modified = rs.getTimestamp(5);
            String description = rs.getString(6);
            String passwd = rs.getString(7);
            String idEnabled = rs.getString(8);
            String updateAllowed = rs.getString(9);
            String salutation = rs.getString(10);
            String firstname = rs.getString(11);
            String lastname = rs.getString(12);
            String street = rs.getString(13);
            String city = rs.getString(14);
            String postalcode = rs.getString(15);
            String country = rs.getString(16);
            String state = rs.getString(17);
            String institution = rs.getString(18);
            String faculty = rs.getString(19);
            String department = rs.getString(20);
            String institute = rs.getString(21);
            String telephone = rs.getString(22);
            String fax = rs.getString(23);
            String email = rs.getString(24);
            String cellphone = rs.getString(25);
            String primaryGroupID = rs.getString(26);

            rs.close();

            // Now lookup the groups this user is a member of
            select = "SELECT GID FROM " + SQLGroupMembersTable + " WHERE USERID = '" + userID + "'";

            ArrayList groups = getSelectResult(select);

            // set some boolean values
            boolean id_enabled = (idEnabled.equals("true")) ? true : false;
            boolean update_allowed = (updateAllowed.equals("true")) ? true : false;

            // We create the user object
            MCRUser user = new MCRUser(numID, userID, creator, created, modified, id_enabled, update_allowed, description, passwd, primaryGroupID, groups, salutation, firstname, lastname, street, city, postalcode, country, state, institution, faculty, department, institute, telephone, fax, email, cellphone);

            rs.close();

            return user;
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This method retrieves a MyCoRe group object from the persistent
     * datastore.
     * 
     * @param groupID
     *            a String representing the MyCoRe group object which is to be
     *            retrieved
     * @return the requested group object
     */
    public synchronized MCRGroup retrieveGroup(String groupID) throws MCRException {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            String select = "SELECT * FROM " + SQLGroupsTable + " WHERE GID = '" + groupID + "'";
            Statement statement = connection.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (!rs.next()) {
                String msg = "MCRSQLUserStore.retrieveGroup(): There is no group with ID = " + groupID;
                throw new MCRException(msg);
            }

            String creator = rs.getString(2);
            Timestamp created = rs.getTimestamp(3);
            Timestamp modified = rs.getTimestamp(4);
            String description = rs.getString(5);

            rs.close();

            // Now lookup the lists of admin users, admin groups, users
            // (members)
            // and privileges
            select = "SELECT USERID FROM " + SQLGroupAdminsTable + " WHERE GID = '" + groupID + "' AND USERID IS NOT NULL AND USERID != ''";

            ArrayList admUserIDs = getSelectResult(select);

            select = "SELECT GROUPID FROM " + SQLGroupAdminsTable + " WHERE GID = '" + groupID + "' AND GROUPID IS NOT NULL AND GROUPID != ''";

            ArrayList admGroupIDs = getSelectResult(select);

            select = "SELECT USERID FROM " + SQLGroupMembersTable + " WHERE GID = '" + groupID + "' AND USERID IS NOT NULL AND USERID != ''";

            ArrayList mbrUserIDs = getSelectResult(select);

            // We create the group object
            MCRGroup group = new MCRGroup(groupID, creator, created, modified, description, admUserIDs, admGroupIDs, mbrUserIDs);

            return group;
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This method updates a MyCoRe user object in the persistent datastore.
     * 
     * @param updUser
     *            the user to be updated
     */
    public synchronized void updateUser(MCRUser updUser) throws MCRException {
        MCRUserContact userContact = updUser.getUserContact();
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            String idEnabled = (updUser.isEnabled()) ? "true" : "false";
            String updateAllowed = (updUser.isUpdateAllowed()) ? "true" : "false";

            connection.getJDBCConnection().setAutoCommit(false);

            String update = "UPDATE " + SQLUsersTable + " SET CREATOR = ? ,CREATIONDATE = ? ,MODIFIEDDATE = ? ,DESCRIPTION = ? " + " ,PASSWD = ? ,ENABLED = ? ,UPD = ? ,SALUTATION = ? ,FIRSTNAME = ? " + " ,LASTNAME = ? ,STREET = ? ,CITY = ? ,POSTALCODE = ? ,COUNTRY = ? ,STATE = ? " + " ,INSTITUTION = ? ,FACULTY = ? ,DEPARTMENT = ? ,INSTITUTE = ? "
                    + " ,TELEPHONE = ? ,FAX = ? ,EMAIL = ? ,CELLPHONE = ? ,PRIMGROUP = ? " + " WHERE UID = ?";

            PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);

            statement.setString(1, updUser.getCreator());
            statement.setTimestamp(2, updUser.getCreationDate());
            statement.setTimestamp(3, updUser.getModifiedDate());
            statement.setString(4, updUser.getDescription());
            statement.setString(5, updUser.getPassword());
            statement.setString(6, idEnabled);
            statement.setString(7, updateAllowed);
            statement.setString(8, userContact.getSalutation());
            statement.setString(9, userContact.getFirstName());
            statement.setString(10, userContact.getLastName());
            statement.setString(11, userContact.getStreet());
            statement.setString(12, userContact.getCity());
            statement.setString(13, userContact.getPostalCode());
            statement.setString(14, userContact.getCountry());
            statement.setString(15, userContact.getState());
            statement.setString(16, userContact.getInstitution());
            statement.setString(17, userContact.getFaculty());
            statement.setString(18, userContact.getDepartment());
            statement.setString(19, userContact.getInstitute());
            statement.setString(20, userContact.getTelephone());
            statement.setString(21, userContact.getFax());
            statement.setString(22, userContact.getEmail());
            statement.setString(23, userContact.getCellphone());
            statement.setString(24, updUser.getPrimaryGroupID());
            statement.setString(25, updUser.getID());

            statement.execute();
            statement.close();

            connection.getJDBCConnection().commit();
            connection.getJDBCConnection().setAutoCommit(true);
        } catch (Exception ex) {
            try {
                connection.getJDBCConnection().rollback();
            } catch (SQLException ignored) {
            }

            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This method updates a MyCoRe group object in the persistent datastore.
     * 
     * @param group
     *            the group to be updated
     */
    public synchronized void updateGroup(MCRGroup group) throws MCRException {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            connection.getJDBCConnection().setAutoCommit(true); // workaround

            // for problem
            // with creating
            // of users
            String update = "UPDATE " + SQLGroupsTable + " SET CREATOR = ? ,CREATIONDATE = ? ,MODIFIEDDATE = ? , DESCRIPTION = ? " + " WHERE GID = ?";

            PreparedStatement statement = connection.getJDBCConnection().prepareStatement(update);
            statement.setString(1, group.getCreator());
            statement.setTimestamp(2, group.getCreationDate());
            statement.setTimestamp(3, group.getModifiedDate());
            statement.setString(4, group.getDescription());
            statement.setString(5, group.getID());

            statement.execute();
            statement.close();

            // Now we update the group admins table. First the admin users and
            // thereafter the admin
            // groups. But first we collect information about which admins have
            // been added or removed
            // from the list. In order to do so we compare the lists of admins
            // before and after the update.
            String select = "SELECT USERID FROM " + SQLGroupAdminsTable + " WHERE GID = '" + group.getID() + "' AND USERID IS NOT NULL";
            ArrayList oldAdminUserIDs = getSelectResult(select);
            ArrayList newAdminUserIDs = group.getAdminUserIDs();

            select = "SELECT GROUPID FROM " + SQLGroupAdminsTable + " WHERE GID = '" + group.getID() + "' AND GROUPID IS NOT NULL";

            ArrayList oldAdminGroupIDs = getSelectResult(select);
            ArrayList newAdminGroupIDs = group.getAdminGroupIDs();

            // We search for the newly added admins and insert them into the
            // table
            String insert = "INSERT INTO " + SQLGroupAdminsTable + "(GID, USERID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newAdminUserIDs.size(); i++) {
                if (!oldAdminUserIDs.contains(newAdminUserIDs.get(i))) {
                    statement.setString(1, group.getID());
                    statement.setString(2, (String) newAdminUserIDs.get(i));
                    statement.execute();
                    statement.clearParameters();
                }
            }

            statement.close();
            insert = "INSERT INTO " + SQLGroupAdminsTable + "(GID, GROUPID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newAdminGroupIDs.size(); i++) {
                if (!oldAdminGroupIDs.contains(newAdminGroupIDs.get(i))) {
                    statement.setString(1, group.getID());
                    statement.setString(2, (String) newAdminGroupIDs.get(i));
                    statement.execute();
                    statement.clearParameters();
                }
            }

            statement.close();

            // We search for the recently removed admins and remove them from
            // the table
            for (int i = 0; i < oldAdminUserIDs.size(); i++) {
                if (!newAdminUserIDs.contains(oldAdminUserIDs.get(i))) {
                    String sql = "DELETE FROM " + SQLGroupAdminsTable + " WHERE GID = '" + group.getID() + "' AND USERID = '" + (String) oldAdminUserIDs.get(i) + "'";
                    MCRSQLConnection.justDoUpdate(sql);
                }
            }

            for (int i = 0; i < oldAdminGroupIDs.size(); i++) {
                if (!newAdminGroupIDs.contains(oldAdminGroupIDs.get(i))) {
                    String sql = "DELETE FROM " + SQLGroupAdminsTable + " WHERE GID = '" + group.getID() + "' AND GROUPID = '" + (String) oldAdminGroupIDs.get(i) + "'";
                    MCRSQLConnection.justDoUpdate(sql);
                }
            }

            // Now we update the membership lookup table. First we collect
            // information about
            // which users have been added or removed. Therefore we compare the
            // list of users
            // this group has as members before and after the update.
            select = "SELECT USERID FROM " + SQLGroupMembersTable + " WHERE GID = '" + group.getID() + "' AND USERID IS NOT NULL";

            ArrayList oldUserIDs = getSelectResult(select);
            ArrayList newUserIDs = group.getMemberUserIDs();

            // We search for the new members and insert them into the lookup
            // table
            insert = "INSERT INTO " + SQLGroupMembersTable + "(GID, USERID) VALUES (?,?)";
            statement = connection.getJDBCConnection().prepareStatement(insert);

            for (int i = 0; i < newUserIDs.size(); i++) {
                if (!oldUserIDs.contains(newUserIDs.get(i))) {
                    statement.setString(1, group.getID());
                    statement.setString(2, (String) newUserIDs.get(i));
                    statement.execute();
                    statement.clearParameters();
                }
            }

            statement.close();

            // We search for the users which have been removed from this group
            // and delete the
            // entries from the member lookup table
            for (int i = 0; i < oldUserIDs.size(); i++) {
                if (!newUserIDs.contains(oldUserIDs.get(i))) {
                    String sql = "DELETE FROM " + SQLGroupMembersTable + " WHERE GID = '" + group.getID() + "' AND USERID = '" + (String) oldUserIDs.get(i) + "'";
                    MCRSQLConnection.justDoUpdate(sql);
                }
            }
             // connection.getJDBCConnection().commit();
            // connection.getJDBCConnection().setAutoCommit(true);
        } catch (Exception ex) {
            // try{ connection.getJDBCConnection().rollback(); }
            // catch(SQLException ignored){}
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }

    /**
     * This private method is a helper method and is called by many of the
     * public methods of this class. It takes a SELECT statement (which must be
     * provided as a parameter) and works this out on the database. This method
     * is only applicable in the case that only one ArrayList of strings is
     * requested as the result of the SELECT statement.
     * 
     * @param select
     *            String, SELECT statement to be carried out on the database
     * @return ArrayList of strings - the result of the SELECT statement
     */
    private ArrayList getSelectResult(String select) throws MCRException {
        ArrayList vec = new ArrayList();
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            Statement statement = connection.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            while (rs.next())
                vec.add(rs.getString(1));

            rs.close();

            return vec;
        } catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            connection.release();
        }
    }
}
