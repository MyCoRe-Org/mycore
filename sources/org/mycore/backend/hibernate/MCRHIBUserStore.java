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

package org.mycore.backend.hibernate;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrivilege;
import org.mycore.user.MCRPrivilegeSet;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserContact;
import org.mycore.user.MCRUserStore;

import org.hibernate.*;
import org.hibernate.cfg.Configuration;

/**
 * This class implements the interface MCRUserStore
 * 
 * @author Matthias Kramm
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore implements MCRUserStore {
    static Logger logger = Logger.getLogger(MCRHIBUserStore.class.getName());

    private SessionFactory sessionFactory;

    /* not used yet */
    private String SQLUsersTable;

    /** name of the sql table containing user information */
    private String SQLGroupsTable;

    /** name of the sql table containing group information */
    private String SQLGroupMembersTable;

    /** name of the sql table containing user and group membership information */
    private String SQLGroupAdminsTable;

    /** name of the sql table containing group admin information */
    private String SQLPrivilegesTable;

    /** name of the sql table containing privilege information */
    private String SQLPrivsLookupTable;

    /** name of the sql table containing group-privilege information */

    /**
     * The constructor reads the names of the SQL tables which hold the user
     * information data from mycore.properties.
     */
    public MCRHIBUserStore() {
        // set configuration
        MCRConfiguration config = MCRConfiguration.instance();
        SQLUsersTable = config.getString("MCR.users_store_sql_table_users",
                "MCRUSERS");
        SQLGroupsTable = config.getString("MCR.users_store_sql_table_groups",
                "MCRGROUPS");
        SQLGroupMembersTable = config.getString(
                "MCR.users_store_sql_table_group_members", "MCRGROUPMEMBERS");
        SQLGroupAdminsTable = config.getString(
                "MCR.users_store_sql_table_group_admins", "MCRGROUPADMINS");
        SQLPrivilegesTable = config.getString(
                "MCR.users_store_sql_table_privileges", "MCRPRIVS");
        SQLPrivsLookupTable = config.getString(
                "MCR.users_store_sql_table_privs_lookup", "MCRPRIVSLOOKUP");
    }

    private Session getSession() {
        return sessionFactory.openSession();
    }

    /**
     * This method creates a MyCoRe user object in the persistent datastore.
     * 
     * @param newUser
     *            the new user object to be stored
     */
    public synchronized void createUser(MCRUser newUser) throws MCRException {
        MCRUserExt u;
        if (newUser instanceof MCRUserExt) {
            u = (MCRUserExt) newUser;
        } else {
            u = new MCRUserExt(newUser);
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.update(u);
        tx.commit();
        session.close();
    }

    /**
     * This method deletes a MyCoRe user object from the persistent datastore.
     * 
     * @param delUserID
     *            a String representing the MyCoRe user object which is to be
     *            deleted
     */
    public synchronized void deleteUser(String delUserID) throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.delete("MCRID = " + delUserID);
        tx.commit();
        session.close();
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
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("MCRID = " + userID).list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return true;
        else
            return false;
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
    public synchronized boolean existsUser(int numID, String userID)
            throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("MCRID = " + userID).list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return true;
        else
            return false;
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
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("MCRID = " + userID).list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no user with ID = "
                    + userID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return (MCRUser) l.get(0);
    }

    /**
     * This method updates a MyCoRe user object in the persistent datastore.
     * 
     * @param updUser
     *            the user to be updated
     */
    public synchronized void updateUser(MCRUser updUser) throws MCRException {
        createUser(updUser);
    }

    /**
     * This method gets all user IDs and returns them as a ArrayList of strings.
     * 
     * @return ArrayList of strings including the user IDs of the system
     */
    public synchronized ArrayList getAllUserIDs() throws MCRException {
        /*
         * TODO: now how are we going to load all user IDs, without filling the
         * entire memory with users ??
         */
        return null;
    }

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public synchronized int getMaxUserNumID() throws MCRException {
        List l = getAllUserIDs();
        if (l == null)
            return 0;
        else
            return l.size();
    }

    /**
     * This method creates a MyCoRe group object in the persistent datastore.
     * 
     * @param newGroup
     *            the new group object to be stored
     */
    public synchronized void createGroup(MCRGroup newGroup) throws MCRException {
        MCRGroupExt u;
        if (newGroup instanceof MCRGroupExt) {
            u = (MCRGroupExt) newGroup;
        } else {
            u = new MCRGroupExt(newGroup);
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.update(u);
        tx.commit();
        session.close();
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
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("GROUPID = " + groupID).list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return true;
        else
            return false;
    }

    /**
     * This method deletes a MyCoRe group object in the persistent datastore.
     * 
     * @param delGroupID
     *            a String representing the MyCoRe group object which is to be
     *            deleted
     */
    public synchronized void deleteGroup(String delGroupID) throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.delete("MCRID = " + delGroupID);
        tx.commit();
        session.close();
    }

    /**
     * This method gets all group IDs and returns them as a ArrayList of
     * strings.
     * 
     * @return ArrayList of strings including the group IDs of the system
     */
    public synchronized ArrayList getAllGroupIDs() throws MCRException {
        /* TODO */
        return null;
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
    public synchronized ArrayList getGroupIDsWithAdminUser(String userID)
            throws MCRException {
        /* TODO */
        return null;
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
    public synchronized ArrayList getUserIDsWithPrimaryGroup(String groupID)
            throws MCRException {
        /* TODO */
        return null;
    }

    /**
     * This method updates a MyCoRe group object in the persistent datastore.
     * 
     * @param group
     *            the group to be updated
     */
    public synchronized void updateGroup(MCRGroup group) throws MCRException {
        createGroup(group);
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
    public synchronized MCRGroup retrieveGroup(String groupID)
            throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("GROUPID = " + groupID).list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return (MCRGroup) l.get(0);
        return null;
    }

    /**
     * This method creates a MyCoRe privilege set object in the persistent
     * datastore.
     * 
     * @param privilegeSet
     *            the privilege set object
     */
    public synchronized void createPrivilegeSet(MCRPrivilegeSet privilegeSet)
            throws MCRException {
        ArrayList privileges = privilegeSet.getPrivileges();
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        for (int i = 0; i < privileges.size(); i++) {
            MCRPrivilege thePrivilege = (MCRPrivilege) privileges.get(i);
            MCRPrivilegeExt tp;
            if (thePrivilege instanceof MCRPrivilegeExt) {
                tp = (MCRPrivilegeExt) thePrivilege;
            } else {
                tp = new MCRPrivilegeExt(thePrivilege);
            }
            session.update(tp);
        }
        tx.commit();
        session.close();
    }

    /**
     * This method tests if a MyCoRe privilege object is available in the
     * persistent datastore.
     * 
     * @param privName
     *            a String representing the MyCoRe privilege object which is to
     *            be looked for
     * @return true if the privilege exist, else return false
     */
    public synchronized boolean existsPrivilege(String privName) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("NAME = " + privName).list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return true;
        else
            return false;
    }

    /**
     * This method tests if a MyCoRe privilege set object is available in the
     * persistent datastore.
     */
    public boolean existsPrivilegeSet() throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("").list();
        tx.commit();
        session.close();
        if (l.size() > 0)
            return true;
        else
            return false;
    }

    /**
     * This method retrieves a MyCoRe privilege set from the persistent
     * datastore.
     * 
     * @return the ArrayList of known privileges of the system
     */
    public ArrayList retrievePrivilegeSet() throws MCRException {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("").list();
        tx.commit();
        session.close();
        return new ArrayList(l);
    }

    /**
     * This method updates a MyCoRe privilege set object in the persistent
     * datastore. New privileges was insert in the database, existing privileges
     * can be update.
     * 
     * @param privilegeSet
     *            the privilege set object to be updated
     */
    public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) {
        createPrivilegeSet(privilegeSet);
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
        throw new IllegalStateException(
                "Hibernate backend doesn't support direct SQL queries");
    }
}