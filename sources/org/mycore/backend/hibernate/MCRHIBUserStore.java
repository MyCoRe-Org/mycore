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

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.mycore.backend.hibernate.tables.MCRGROUPADMINS;
import org.mycore.backend.hibernate.tables.MCRGROUPADMINSPK;
import org.mycore.backend.hibernate.tables.MCRGROUPMEMBERS;
import org.mycore.backend.hibernate.tables.MCRGROUPS;
import org.mycore.backend.hibernate.tables.MCRUSERS;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserStore;

/**
 * This class implements the interface MCRUserStore - flushmode is configured to
 * COMMIT !
 * 
 * @author Matthias Kramm
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore implements MCRUserStore {
    static Logger logger = Logger.getLogger(MCRHIBUserStore.class.getName());

    private static final MCRUSERS ADMIN_USER = new MCRUSERS();

    private static final MCRGROUPS ADMIN_GROUP = new MCRGROUPS();

    /**
     * The constructor reads the names of the SQL tables which hold the user
     * information data from mycore.properties.
     */
    public MCRHIBUserStore() {
        MCRConfiguration config = MCRConfiguration.instance();
        ADMIN_USER.setUid(config.getString("MCR.Users.Superuser.UserName"));
        ADMIN_GROUP.setGid(config.getString("MCR.Users.Superuser.GroupName"));
    }

    /**
     * This method creates a MyCoRe user object in the persistent datastore.
     * 
     * @param newUser
     *            the new user object to be stored
     */
    public void createUser(MCRUser newUser) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        MCRUSERS user = new MCRUSERS();
        String idEnabled = (newUser.isEnabled()) ? "true" : "false";
        String updateAllowed = (newUser.isUpdateAllowed()) ? "true" : "false";

        user.setNumid(newUser.getNumID());
        user.setUid(newUser.getID());
        user.setCreator(newUser.getCreator());
        user.setCreationdate(newUser.getCreationDate());
        user.setModifieddate(newUser.getModifiedDate());
        user.setDescription(newUser.getDescription());
        user.setPasswd(newUser.getPassword());
        user.setEnabled(idEnabled);
        user.setUpd(updateAllowed);
        user.setSalutation(newUser.getUserContact().getSalutation());
        user.setFirstname(newUser.getUserContact().getFirstName());
        user.setLastname(newUser.getUserContact().getLastName());
        user.setStreet(newUser.getUserContact().getStreet());
        user.setCity(newUser.getUserContact().getCity());
        user.setPostalcode(newUser.getUserContact().getPostalCode());
        user.setCountry(newUser.getUserContact().getCountry());
        user.setState(newUser.getUserContact().getState());
        user.setInstitution(newUser.getUserContact().getInstitution());
        user.setFaculty(newUser.getUserContact().getFaculty());
        user.setDepartment(newUser.getUserContact().getDepartment());
        user.setInstitute(newUser.getUserContact().getInstitute());
        user.setTelephone(newUser.getUserContact().getTelephone());
        user.setFax(newUser.getUserContact().getFax());
        user.setEmail(newUser.getUserContact().getEmail());
        user.setCellphone(newUser.getUserContact().getCellphone());
        MCRGROUPS group = (MCRGROUPS) session.get(MCRGROUPS.class, newUser.getPrimaryGroupID());
        user.setPrimgroup(group);

        // insert values
        session.save(user);
        session.flush();
    }

    /**
     * This method deletes a MyCoRe user object from the persistent datastore.
     * 
     * @param delUserID
     *            a String representing the MyCoRe user object which is to be
     *            deleted
     */
    public void deleteUser(String delUserID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRGROUPMEMBERS where USERID='" + delUserID + "'").list();

        for (int i = 0; i < l.size(); i++) {
            MCRGROUPMEMBERS members = (MCRGROUPMEMBERS) l.get(i);
            session.delete(members);
        }
        l = session.createQuery("from MCRUSERS where UID='" + delUserID + "'").list();
        if (l.size() == 1) {
            MCRUSERS user = (MCRUSERS) l.get(0);
            session.delete(user);
        } else {
            logger.warn("There is no user '" + delUserID + "'");
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
    public boolean existsUser(String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRUSERS where UID = '" + userID + "'").list();

        if (l.size() > 0) {
            return true;
        } else {
            return false;
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
    public boolean existsUser(int numID, String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List l = null;

        l = session.createQuery("from MCRUSERS where NUMID = " + numID + " or UID = '" + userID + "'").list();

        if (l.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method retrieves a MyCoRe user object from the persistent datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            retrieved
     * @return the requested user object
     */
    public MCRUser retrieveUser(String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        MCRUser retuser = null;
        MCRUSERS user = (MCRUSERS) session.get(MCRUSERS.class, userID);

        if (user == null) {
            return null;
        } else {
            int numID = user.getNumid();
            String creator = user.getCreator();
            Timestamp created = user.getCreationdate();
            Timestamp modified = user.getModifieddate();
            String description = user.getDescription();
            String passwd = user.getPasswd();
            String idEnabled = user.getEnabled();
            String updateAllowed = user.getUpd();
            String salutation = user.getSalutation();
            String firstname = user.getFirstname();
            String lastname = user.getLastname();
            String street = user.getStreet();
            String city = user.getCity();
            String postalcode = user.getPostalcode();
            String country = user.getCountry();
            String state = user.getState();
            String institution = user.getInstitution();
            String faculty = user.getFaculty();
            String department = user.getDepartment();
            String institute = user.getInstitute();
            String telephone = user.getTelephone();
            String fax = user.getFax();
            String email = user.getEmail();
            String cellphone = user.getCellphone();
            String primaryGroupID = user.getPrimgroup().getGid();

            // Now lookup the groups this user is a member of
            ArrayList<String> groups = new ArrayList<String>();
            List l = session.createQuery("from MCRGROUPMEMBERS where USERID = '" + userID + "'").list();

            MCRGROUPMEMBERS group;

            for (int i = 0; i < l.size(); i++) {
                group = (MCRGROUPMEMBERS) l.get(i);
                groups.add(group.getGid().getGid());
            }

            // set some boolean values
            boolean id_enabled = (idEnabled.equals("true")) ? true : false;
            boolean update_allowed = (updateAllowed.equals("true")) ? true : false;

            // We create the user object
            try {
                retuser = new MCRUser(numID, userID, creator, created, modified, id_enabled, update_allowed, description, passwd, primaryGroupID, groups,
                        salutation, firstname, lastname, street, city, postalcode, country, state, institution, faculty, department, institute, telephone, fax,
                        email, cellphone);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return retuser;
    }

    /**
     * This method updates a MyCoRe user object in the persistent datastore.
     * 
     * @param updUser
     *            the user to be updated
     */
    public void updateUser(MCRUser updUser) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        MCRUSERS user = (MCRUSERS) session.get(MCRUSERS.class, updUser.getID());

        if (user != null) {
            String isEnabled = (updUser.isEnabled()) ? "true" : "false";
            String updateAllowed = (updUser.isUpdateAllowed()) ? "true" : "false";
            user.setNumid(updUser.getNumID());
            user.setUid(updUser.getID());
            user.setCreator(updUser.getCreator());
            user.setCreationdate(updUser.getCreationDate());
            user.setModifieddate(updUser.getModifiedDate());
            user.setDescription(updUser.getDescription());
            user.setPasswd(updUser.getPassword());
            user.setEnabled(isEnabled);
            user.setUpd(updateAllowed);
            user.setSalutation(updUser.getUserContact().getSalutation());
            user.setFirstname(updUser.getUserContact().getFirstName());
            user.setLastname(updUser.getUserContact().getLastName());
            user.setStreet(updUser.getUserContact().getStreet());
            user.setCity(updUser.getUserContact().getCity());
            user.setPostalcode(updUser.getUserContact().getPostalCode());
            user.setCountry(updUser.getUserContact().getCountry());
            user.setState(updUser.getUserContact().getState());
            user.setInstitution(updUser.getUserContact().getInstitution());
            user.setFaculty(updUser.getUserContact().getFaculty());
            user.setDepartment(updUser.getUserContact().getDepartment());
            user.setInstitute(updUser.getUserContact().getInstitute());
            user.setTelephone(updUser.getUserContact().getTelephone());
            user.setFax(updUser.getUserContact().getFax());
            user.setEmail(updUser.getUserContact().getEmail());
            user.setCellphone(updUser.getUserContact().getCellphone());
            user.setPrimgroup((MCRGROUPS) session.get(MCRGROUPS.class, updUser.getPrimaryGroupID()));
            session.save(user);
        } else {
            throw new MCRException("User " + updUser.getID() + " not found.");
        }
    }

    /**
     * This method gets all user IDs and returns them as a ArrayList of strings.
     * 
     * @return ArrayList of strings including the user IDs of the system
     */
    @SuppressWarnings("unchecked")
    public List<String> getAllUserIDs() throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List<String> l = session.createQuery("SELECT uid from MCRUSERS").list();
        return l;
    }

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public int getMaxUserNumID() throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        return ((Number) session.createQuery("select max(numid) from MCRUSERS").uniqueResult()).intValue();
    }

    /**
     * This method creates a MyCoRe group object in the persistent datastore.
     * 
     * @param newGroup
     *            the new group object to be stored
     */
    public void createGroup(MCRGroup newGroup) throws MCRException {
        MCRGROUPS group = new MCRGROUPS();
        Session session = MCRHIBConnection.instance().getSession();
        // insert values
        group.setGid(newGroup.getID());
        group.setCreator(newGroup.getCreator());
        group.setCreationdate(newGroup.getCreationDate());
        group.setModifieddate(newGroup.getModifiedDate());
        group.setDescription(newGroup.getDescription());
        session.save(group);
        session.flush();

        final ArrayList memberUserIDs = newGroup.getMemberUserIDs();
        // now update the member lookup table Groupmembers
        for (int i = 0; i < memberUserIDs.size(); i++) {
            MCRGROUPMEMBERS member = new MCRGROUPMEMBERS();
            member.setGid(group);
            member.setUserid((MCRUSERS) session.get(MCRUSERS.class, memberUserIDs.get(i).toString()));
            session.save(member);
            session.flush();
        }

        // Groupadmins
        for (int i = 0; i < newGroup.getAdminUserIDs().size(); i++) {
            MCRGROUPADMINS admin = new MCRGROUPADMINS();
            admin.setGid(group);
            admin.setUserid((MCRUSERS) session.get(MCRUSERS.class, newGroup.getAdminUserIDs().get(i).toString()));
            admin.setGroupid(ADMIN_GROUP);
            session.saveOrUpdate(admin);
            session.flush();
            session.evict(ADMIN_GROUP);
        }
        for (int i = 0; i < newGroup.getAdminGroupIDs().size(); i++) {
            MCRGROUPADMINS admin = new MCRGROUPADMINS();
            admin.setGid(group);
            admin.setUserid(ADMIN_USER);
            final String adminGroupID = newGroup.getAdminGroupIDs().get(i).toString();
            admin.setGroupid((MCRGROUPS) session.get(MCRGROUPS.class, adminGroupID));
            if (session.get(MCRGROUPADMINS.class, admin.getKey()) == null) {
                session.saveOrUpdate(admin);
                session.flush();
                session.evict(ADMIN_USER);
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
    public boolean existsGroup(String groupID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        return ((Number) session.createCriteria(MCRGROUPS.class).setProjection(Projections.rowCount()).add(Restrictions.eq("gid", groupID)).uniqueResult())
                .intValue() > 0;
    }

    /**
     * This method deletes a MyCoRe group object in the persistent datastore.
     * 
     * @param delGroupID
     *            a String representing the MyCoRe group object which is to be
     *            deleted
     */
    public void deleteGroup(String delGroupID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        session.delete(session.get(MCRGROUPS.class, delGroupID));
    }

    /**
     * This method gets all group IDs and returns them as a ArrayList of
     * strings.
     * 
     * @return ArrayList of strings including the group IDs of the system
     */
    @SuppressWarnings("unchecked")
    public List<String> getAllGroupIDs() throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List<String> l = session.createQuery("SELECT gid from MCRGROUPS").list();
        return l;
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
    @SuppressWarnings("unchecked")
    public List<String> getGroupIDsWithAdminUser(String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(MCRGROUPADMINS.class).setProjection(Projections.property("gid")).add(
                Restrictions.eq("userid", session.get(MCRUSERS.class, userID)));
        return c.list();
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
    @SuppressWarnings("unchecked")
    public List<String> getUserIDsWithPrimaryGroup(String groupID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List<String> l = session.createQuery("SELECT uid from MCRUSERS where PRIMGROUP = '" + groupID + "'").list();
        return l;
    }

    /**
     * This method updates a MyCoRe group object in the persistent datastore.
     * 
     * @param group
     *            the group to be updated
     */
    public void updateGroup(MCRGroup group) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        MCRGROUPS dbgroup = (MCRGROUPS) session.get(MCRGROUPS.class, group.getID());

        if (dbgroup != null) {
            dbgroup.setDescription(group.getDescription());
            dbgroup.setModifieddate(group.getModifiedDate());
            session.update(dbgroup);
        }

        // prepare groupadmin arraylist
        ArrayList<String> oldAdminUserIDs = new ArrayList<String>();
        ArrayList<String> oldAdminGroupIDs = new ArrayList<String>();
        ArrayList newAdminGroupIDs = group.getAdminGroupIDs();
        ArrayList newAdminUserIDs = group.getAdminUserIDs();
        List l = session.createQuery("from MCRGROUPADMINS where GID='" + group.getID() + "'").list();

        for (int i = 0; i < l.size(); i++) {
            MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);

            if ((grpadmins.getUserid() != null) && !grpadmins.getUserid().equals("")) {
                oldAdminUserIDs.add(grpadmins.getUserid().getUid());
            }

            if ((grpadmins.getUserid() != null) && !grpadmins.getGroupid().equals("")) {
                oldAdminGroupIDs.add(grpadmins.getGroupid().getGid());
            }
        }

        // prepare groupmember arraylist
        ArrayList<String> oldUserIDs = new ArrayList<String>();
        ArrayList newUserIDs = group.getMemberUserIDs();
        l = session.createQuery("from MCRGROUPMEMBERS where GID='" + group.getID() + "'").list();

        for (int i = 0; i < l.size(); i++) {
            MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);

            if ((grpmembers.getUserid() != null) && !grpmembers.getUserid().equals("")) {
                oldUserIDs.add(grpmembers.getUserid().getUid());
            }
        }

        // insert
        for (int i = 0; i < newAdminUserIDs.size(); i++) {
            if (!oldAdminUserIDs.contains(newAdminUserIDs.get(i))) {
                MCRGROUPADMINSPK pk = new MCRGROUPADMINSPK();
                pk.setGid(dbgroup);
                pk.setGroupid(ADMIN_GROUP);
                MCRUSERS user = new MCRUSERS();
                user.setUid((String) newAdminUserIDs.get(i));
                pk.setUserid(user);
                if (session.get(MCRGROUPADMINS.class, pk) == null) {
                    MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
                    grpadmins.setKey(pk);
                    session.save(grpadmins);
                    session.evict(ADMIN_GROUP);
                    session.evict(user);
                }
            }
        }

        for (int i = 0; i < newAdminGroupIDs.size(); i++) {
            if (!oldAdminGroupIDs.contains(newAdminGroupIDs.get(i))) {
                MCRGROUPADMINSPK pk = new MCRGROUPADMINSPK();
                pk.setGid(dbgroup);
                MCRGROUPS adminGroup = new MCRGROUPS();
                adminGroup.setGid((String) newAdminGroupIDs.get(i));
                pk.setGroupid(adminGroup);
                pk.setUserid(ADMIN_USER);
                if (session.get(MCRGROUPADMINS.class, pk) == null) {
                    MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
                    grpadmins.setKey(pk);
                    session.save(grpadmins);
                    session.evict(ADMIN_USER);
                    session.evict(adminGroup);
                }
            }
        }

        // We search for the recently removed admins and remove them from
        // the table
        for (int i = 0; i < oldAdminUserIDs.size(); i++) {
            if (!newAdminUserIDs.contains(oldAdminUserIDs.get(i))) {
                int deletedEntities = session.createQuery(
                        "delete MCRGROUPADMINS " + "where GID = '" + group.getID() + "'" + " and USERID = '" + (String) oldAdminUserIDs.get(i) + "'")
                        .executeUpdate();
                logger.info(deletedEntities + " groupadmin-entries deleted");
            }
        }

        for (int i = 0; i < oldAdminGroupIDs.size(); i++) {
            if (!newAdminGroupIDs.contains(oldAdminGroupIDs.get(i))) {
                int deletedEntities = session.createQuery(
                        "delete MCRGROUPADMINS " + "where GID = '" + group.getID() + "'" + " and GROUPID = '" + (String) oldAdminGroupIDs.get(i) + "'")
                        .executeUpdate();
                logger.info(deletedEntities + " groupadmin-entries deleted");
            }
        }

        // We search for the new members and insert them into the lookup
        // table
        for (int i = 0; i < newUserIDs.size(); i++) {
            if (!oldUserIDs.contains(newUserIDs.get(i))) {
                MCRGROUPMEMBERS grpmembers = new MCRGROUPMEMBERS();
                grpmembers.setGid(dbgroup);
                MCRUSERS user = new MCRUSERS();
                user.setUid(newUserIDs.get(i).toString());
                grpmembers.setUserid(user);
                session.save(grpmembers);
                session.evict(user);
            }
        }

        // We search for the users which have been removed from this group
        // and delete the entries from the member lookup table
        for (int i = 0; i < oldUserIDs.size(); i++) {
            if (!newUserIDs.contains(oldUserIDs.get(i))) {
                int deletedEntities = session.createQuery(
                        "delete MCRGROUPMEMBERS where GID = '" + group.getID() + "' " + "and USERID ='" + (String) oldUserIDs.get(i) + "'").executeUpdate();
                logger.info(deletedEntities + " groupmember-entries deleted");
            }
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
    public MCRGroup retrieveGroup(String groupID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        MCRGROUPS groups = (MCRGROUPS) session.get(MCRGROUPS.class, groupID);
        if (groups == null) {
            throw new MCRException("retrieveGroup: There is no group with ID = " + groupID);
        } else {

            String creator = groups.getCreator();
            Timestamp created = groups.getCreationdate();
            Timestamp modified = groups.getModifieddate();
            String description = groups.getDescription();

            // Now lookup the lists of admin users, admin groups, users
            // (members) and privileges
            List l = session.createQuery("from MCRGROUPADMINS where GID = '" + groupID + "'").list();

            ArrayList<String> admUserIDs = new ArrayList<String>();
            ArrayList<String> admGroupIDs = new ArrayList<String>();

            if (l.size() > 0) {
                for (int i = 0; i < l.size(); i++) {
                    MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);

                    if (grpadmins.getUserid() != null && !grpadmins.getUserid().equals("")) {
                        admUserIDs.add(grpadmins.getUserid().getUid());
                    }

                    if (grpadmins.getGroupid() != null && !grpadmins.getGroupid().equals("")) {
                        admGroupIDs.add(grpadmins.getGroupid().getGid());
                    }
                }
            }

            l = session.createQuery("from MCRGROUPMEMBERS where GID = '" + groupID + "'").list();

            ArrayList<String> mbrUserIDs = new ArrayList<String>();

            Set<String> users = new HashSet<String>();

            if (l.size() > 0) {
                for (int i = 0; i < l.size(); i++) {
                    MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);

                    if (grpmembers.getUserid() != null && !grpmembers.getUserid().equals("")) {
                        users.add(grpmembers.getUserid().getUid());
                    }
                }
            }

            // Add all users with groupID as primary group
            l = session.createCriteria(MCRUSERS.class).setProjection(Projections.property("uid")).add(Restrictions.eq("primgroup", groups)).list();
            for (Object uid : l) {
                users.add(uid.toString());
            }
            mbrUserIDs.addAll(users);

            // We create the group object
            try {
                return new MCRGroup(groupID, creator, created, modified, description, admUserIDs, admGroupIDs, mbrUserIDs);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MCRException("Cannot create group object.", e);
            }
        }
    }

    public void createUserTables() {
        System.out.println("Create all user tables for hibernate User Store");
    }
}
