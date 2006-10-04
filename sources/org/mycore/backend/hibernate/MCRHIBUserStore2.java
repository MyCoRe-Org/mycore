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
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.tables.MCRGROUPADMINS;
import org.mycore.backend.hibernate.tables.MCRGROUPMEMBERS;
import org.mycore.backend.hibernate.tables.MCRGROUPS;
import org.mycore.backend.hibernate.tables.MCRUSERS;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.user2.MCRGroup;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserStore;

/**
 * This class implements the interface MCRUserStore
 * 
 * @author Matthias Kramm
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore2 implements MCRUserStore {
    static Logger logger = Logger.getLogger(MCRHIBUserStore2.class.getName());

    /**
     * The constructor reads the names of the SQL tables which hold the user
     * information data from mycore.properties.
     */
    public MCRHIBUserStore2() {
    }

    /**
     * This method creates a MyCoRe user object in the persistent datastore.
     * 
     * @param newUser
     *            the new user object to be stored
     */
    public synchronized void createUser(MCRUser newUser) throws MCRException {
        MCRUSERS user = new MCRUSERS();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            // insert values
            String idEnabled = (newUser.isEnabled()) ? "true" : "false";
            String updateAllowed = (newUser.isUpdateAllowed()) ? "true" : "false";

            user.getKey().setNumid(newUser.getNumID());
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
            user.setPrimgroup(newUser.getPrimaryGroupID());
            session.save(user);
            tx.commit();
        } catch (MCRException e) {
            tx.rollback();
            throw new MCRException("Error while creating new user.", e);
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Hibernate error while creating new user.", e);
        } finally {
            session.close();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            List l = session.createQuery("from MCRGROUPMEMBERS where USERID='" + delUserID + "'").list();

            for (int i = 0; i < l.size(); i++) {
                MCRGROUPMEMBERS members = (MCRGROUPMEMBERS) l.get(i);
                session.delete(members);
                session.flush();
            }

            l = session.createQuery("from MCRUSERS where UID='" + delUserID + "'").list();

            if (l.size() == 1) {
                MCRUSERS user = (MCRUSERS) l.get(0);
                session.delete(user);
                session.flush();
            } else {
                logger.warn("There is no user '" + delUserID + "'");
            }

            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Hibernate error while deleting user '" + delUserID + "'.", e);
        } finally {
            session.close();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where UID = '" + userID + "'").list();
        tx.commit();
        session.close();

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
    public synchronized boolean existsUser(int numID, String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List l = null;

        try {
            l = session.createQuery("from MCRUSERS where NUMID = " + numID + " or UID = '" + userID + "'").list();
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error during existsUser()", e);
        } finally {
            session.close();
        }

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
    public synchronized MCRUser retrieveUser(String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRUser retuser = new MCRUser();

        try {
            List l = session.createQuery("from MCRUSERS where UID = '" + userID + "'").list();
            MCRUSERS user;
            retuser = null;

            if (l.size() == 0) {
                tx.rollback();
                throw new MCRException("There is no user with ID = " + userID);
            } else {
                user = (MCRUSERS) l.get(0);

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
                String primaryGroupID = user.getPrimgroup();

                // Now lookup the groups this user is a member of
                ArrayList groups = new ArrayList();
                l = session.createQuery("from MCRGROUPMEMBERS where USERID = '" + userID + "'").list();

                if (l.size() >= 1) {
                    MCRGROUPMEMBERS group;

                    for (int i = 0; i < l.size(); i++) {
                        group = (MCRGROUPMEMBERS) l.get(i);
                        groups.add(group.getGid());
                    }
                } else {
                    logger.warn("User with ID = " + userID + " is no groupmember.");
                }

                // set some boolean values
                boolean id_enabled = (idEnabled.equals("true")) ? true : false;
                boolean update_allowed = (updateAllowed.equals("true")) ? true : false;

                // We create the user object
                try {
                    retuser = new MCRUser(numID, userID, creator, created, modified, id_enabled, update_allowed, description, passwd, primaryGroupID, groups, salutation, firstname, lastname, street, city, postalcode, country, state, institution, faculty, department, institute, telephone, fax, email, cellphone);
                } catch (MCRException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Error while reading user information.", e);
        } finally {
            session.close();
        }

        return retuser;
    }

    /**
     * This method updates a MyCoRe user object in the persistent datastore.
     * 
     * @param updUser
     *            the user to be updated
     */
    public synchronized void updateUser(MCRUser updUser) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where UID = '" + updUser.getID() + "'").list();
        MCRUSERS user = new MCRUSERS();

        if (l.size() == 1) {
            user = (MCRUSERS) l.get(0);

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
            user.setPrimgroup(updUser.getPrimaryGroupID());
            session.save(user);
        } else {
            tx.commit();
            session.close();
            throw new MCRException("User " + updUser.getID() + " not found.");
        }

        tx.commit();
        session.close();
    }

    /**
     * This method gets all user IDs and returns them as a ArrayList of strings.
     * 
     * @return ArrayList of strings including the user IDs of the system
     */
    public synchronized ArrayList getAllUserIDs() throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = null;
        ArrayList list = new ArrayList();

        try {
            l = session.createQuery("from MCRUSERS").list();

            for (int i = 0; i < l.size(); i++) {
                MCRUSERS user = (MCRUSERS) l.get(i);
                list.add(user.getUid());
            }

            tx.commit();
        } catch (Exception e) {
            logger.error(e);
            tx.rollback();
        } finally {
            session.close();
        }

        return list;
    }

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public synchronized int getMaxUserNumID() throws MCRException {
        int ret = 0;
        Session session = MCRHIBConnection.instance().getSession();
        List l = null;

        try {
            l = session.createQuery("select max(key.numid) from MCRUSERS").list();
            if (l.size() > 0 && l.get(0) != null) {            	
            	Integer max = (Integer) l.get(0);
            	if (max != null) 
            		 ret = max.intValue();
            }
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("error during getMaxUserNumID()", e);
        } finally {
            session.close();
        }
        return ret;                       
    }

    /**
     * This method creates a MyCoRe group object in the persistent datastore.
     * 
     * @param newGroup
     *            the new group object to be stored
     */
    public synchronized void createGroup(MCRGroup newGroup) throws MCRException {
        MCRGROUPS group = new MCRGROUPS();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            // insert values
            group.setGid(newGroup.getID());
            group.setCreator(newGroup.getCreator());
            group.setCreationdate(newGroup.getCreationDate());
            group.setModifieddate(newGroup.getModifiedDate());
            group.setDescription(newGroup.getDescription());
            session.save(group);

            // now update the member lookup table Groupmembers
            for (int i = 0; i < newGroup.getMemberUserIDs().size(); i++) {
                MCRGROUPMEMBERS member = new MCRGROUPMEMBERS();
                member.setGid(newGroup.getID());
                member.setUserid((String) newGroup.getMemberUserIDs().get(i));
                session.save(member);
            }

            // Groupadmins
            for (int i = 0; i < newGroup.getAdminUserIDs().size(); i++) {
                MCRGROUPADMINS admin = new MCRGROUPADMINS();
                admin.setGid(newGroup.getID());
                admin.setUserid((String) newGroup.getAdminUserIDs().get(i));
                admin.setGroupid("");
                session.saveOrUpdate(admin);
            }
            for (int i = 0; i < newGroup.getAdminGroupIDs().size(); i++) {
                MCRGROUPADMINS admin = new MCRGROUPADMINS();
                admin.setGid(newGroup.getID());
                admin.setUserid("");
                admin.setGroupid((String) newGroup.getAdminGroupIDs().get(i));
                session.saveOrUpdate(admin);
            }            

            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Error in UserStore.", e);
        } finally {
            session.close();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRGROUPS where GID = '" + groupID + "'").list();
        tx.commit();
        session.close();

        if (l.size() > 0) {
            return true;
        } else {
            return false;
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            List l = session.createQuery("from MCRGROUPS where GID = '" + delGroupID + "'").list();

            if (l.size() == 1) {
                MCRGROUPS group = (MCRGROUPS) l.get(0);
                session.delete(group);
            }

            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Error while deleting group '" + delGroupID + "'.", e);
        } finally {
            session.close();
        }
    }

    /**
     * This method gets all group IDs and returns them as a ArrayList of
     * strings.
     * 
     * @return ArrayList of strings including the group IDs of the system
     */
    public synchronized ArrayList getAllGroupIDs() throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRGROUPS").list();
        tx.commit();
        session.close();

        if (l.size() > 0) {
            ArrayList retList = new ArrayList();

            for (int i = 0; i < l.size(); i++) {
                MCRGROUPS group = (MCRGROUPS) l.get(i);
                retList.add(group.getGid());
            }

            return retList;
        } else {
            return null;
        }
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
        ArrayList retList = new ArrayList();
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRGROUPADMINS where USERID = '" + userID + "'").list();

        if (l.size() > 0) {
            for (int i = 0; i < l.size(); i++) {
                MCRGROUPADMINS groupadmins = (MCRGROUPADMINS) l.get(i);
                retList.add(groupadmins.getGid());
            }
        }

        session.close();

        return retList;
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
        ArrayList retList = new ArrayList();
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRUSERS where PRIMGROUP = '" + groupID + "'").list();

        if (l.size() > 0) {
            for (int i = 0; i < l.size(); i++) {
                MCRUSERS users = (MCRUSERS) l.get(i);
                retList.add(users.getUid());
            }
        }

        session.close();

        return retList;
    }

    /**
     * This method updates a MyCoRe group object in the persistent datastore.
     * 
     * @param group
     *            the group to be updated
     */
    public synchronized void updateGroup(MCRGroup group) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            List l = session.createQuery("from MCRGROUPS where GID = '" + group.getID() + "'").list();
            MCRGROUPS dbgroup = new MCRGROUPS();

            if (l.size() >= 0) {
                for (int i = 0; i < l.size(); i++) {
                    dbgroup = (MCRGROUPS) l.get(i);
                    dbgroup.setDescription(group.getDescription());
                    dbgroup.setModifieddate(group.getModifiedDate());
                    session.saveOrUpdate(dbgroup);
                    session.flush();
                }
            }

            // prepare groupadmin arraylist
            ArrayList oldAdminUserIDs = new ArrayList();
            ArrayList oldAdminGroupIDs = new ArrayList();
            ArrayList newAdminGroupIDs = group.getAdminGroupIDs();
            ArrayList newAdminUserIDs = group.getAdminUserIDs();
            l = session.createQuery("from MCRGROUPADMINS where GID='" + group.getID() + "'").list();

            for (int i = 0; i < l.size(); i++) {
                MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);

                if ((grpadmins.getUserid() != null) && !grpadmins.getUserid().equals("")) {
                    oldAdminUserIDs.add(grpadmins.getUserid());
                }

                if ((grpadmins.getUserid() != null) && !grpadmins.getGroupid().equals("")) {
                    oldAdminGroupIDs.add(grpadmins.getGroupid());
                }
            }

            // prepare groupmember arraylist
            ArrayList oldUserIDs = new ArrayList();
            ArrayList newUserIDs = group.getMemberUserIDs();
            l = session.createQuery("from MCRGROUPMEMBERS where GID='" + group.getID() + "'").list();

            for (int i = 0; i < l.size(); i++) {
                MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);

                if ((grpmembers.getUserid() != null) && !grpmembers.getUserid().equals("")) {
                    oldUserIDs.add(grpmembers.getUserid());
                }
            }

            // after selection close commit transaction
            tx.commit();
            session.close();
            session = MCRHIBConnection.instance().getSession();
            tx = session.beginTransaction();

            // insert
            for (int i = 0; i < newAdminUserIDs.size(); i++) {
                if (!oldAdminUserIDs.contains(newAdminUserIDs.get(i))) {
                    MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
                    grpadmins.setGid(group.getID());
                    grpadmins.setUserid((String) newAdminUserIDs.get(i));
                    grpadmins.setGroupid("");
                    session.save(grpadmins);
                    session.flush();
                }
            }

            for (int i = 0; i < newAdminGroupIDs.size(); i++) {
                if (!oldAdminGroupIDs.contains(newAdminGroupIDs.get(i))) {
                    MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
                    grpadmins.setGid(group.getID());
                    grpadmins.setUserid("");
                    grpadmins.setGroupid((String) newAdminGroupIDs.get(i));
                    session.save(grpadmins);
                    session.flush();
                }
            }

            // We search for the recently removed admins and remove them from
            // the table
            for (int i = 0; i < oldAdminUserIDs.size(); i++) {
                if (!newAdminUserIDs.contains(oldAdminUserIDs.get(i))) {
                    int deletedEntities = session.createQuery("delete MCRGROUPADMINS " + "where GID = '" + group.getID() + "'" + " and USERID = '" + (String) oldAdminUserIDs.get(i) + "'").executeUpdate();
                    logger.info(deletedEntities + " groupadmin-entries deleted");
                }
            }

            for (int i = 0; i < oldAdminGroupIDs.size(); i++) {
                if (!newAdminGroupIDs.contains(oldAdminGroupIDs.get(i))) {
                    int deletedEntities = session.createQuery("delete MCRGROUPADMINS " + "where GID = '" + group.getID() + "'" + " and GROUPID = '" + (String) oldAdminGroupIDs.get(i) + "'").executeUpdate();
                    logger.info(deletedEntities + " groupadmin-entries deleted");
                }
            }

            // We search for the new members and insert them into the lookup
            // table
            for (int i = 0; i < newUserIDs.size(); i++) {
                if (!oldUserIDs.contains(newUserIDs.get(i))) {
                    MCRGROUPMEMBERS grpmembers = new MCRGROUPMEMBERS();
                    grpmembers.setGid(group.getID());
                    grpmembers.setUserid((String) newUserIDs.get(i));
                    session.save(grpmembers);
                    session.flush();
                }
            }

            // We search for the users which have been removed from this group
            // and delete the entries from the member lookup table
            for (int i = 0; i < oldUserIDs.size(); i++) {
                if (!newUserIDs.contains(oldUserIDs.get(i))) {
                    int deletedEntities = session.createQuery("delete MCRGROUPMEMBERS where GID = '" + group.getID() + "' " + "and USERID ='" + (String) oldUserIDs.get(i) + "'").executeUpdate();
                    logger.info(deletedEntities + " groupmember-entries deleted");
                }
            }

            tx.commit();
        } catch (MCRPersistenceException e) {
            tx.rollback();
            logger.error("stackTrace", e);
            throw new MCRException("Error in UserStore.", e);
        } catch (HibernateException e) {
            tx.rollback();
            logger.error("stackTrace", e);
            throw new MCRException("Hibernate error in UserStore.", e);
        } catch (Exception e) {
            tx.rollback();
            logger.error("stackTrace", e);
            throw new MCRException("Exception: ", e);
        } finally {
            session.close();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRGroup group = new MCRGroup();

        try {
            List l = session.createQuery("from MCRGROUPS where GID = '" + groupID + "'").list();

            if (l.size() == 0) {
                tx.rollback();
                throw new MCRException("There is no group with ID = " + groupID);
            } else {
                MCRGROUPS groups = (MCRGROUPS) l.get(0);

                String creator = groups.getCreator();
                Timestamp created = groups.getCreationdate();
                Timestamp modified = groups.getModifieddate();
                String description = groups.getDescription();

                // Now lookup the lists of admin users, admin groups, users
                // (members) and privileges
                l = session.createQuery("from MCRGROUPADMINS where GID = '" + groupID + "'").list();

                ArrayList admUserIDs = new ArrayList();
                ArrayList admGroupIDs = new ArrayList();

                if (l.size() > 0) {
                    for (int i = 0; i < l.size(); i++) {
                        MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);

                        if (grpadmins.getUserid() == null) {
                        } else {
                            if (!grpadmins.getUserid().equals("")) {
                                admUserIDs.add(grpadmins.getUserid());
                            }
                        }

                        if (grpadmins.getGroupid() == null) {
                        } else {
                            if (!grpadmins.getGroupid().equals("")) {
                                admGroupIDs.add(grpadmins.getGroupid());
                            }
                        }
                    }
                }

                l = session.createQuery("from MCRGROUPMEMBERS where GID = '" + groupID + "'").list();

                ArrayList mbrUserIDs = new ArrayList();

                if (l.size() > 0) {
                    for (int i = 0; i < l.size(); i++) {
                        MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);

                        if (grpmembers.getUserid() == null) {
                        } else {
                            if (!grpmembers.getUserid().equals("")) {
                                mbrUserIDs.add(grpmembers.getUserid());
                            }
                        }
                    }
                }

                // We create the group object
                try {
                    group = new MCRGroup(groupID, creator, created, modified, description, admUserIDs, admGroupIDs, mbrUserIDs);
                } catch (MCRException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            tx.commit();
        } catch (HibernateException ex) {
            tx.rollback();
            throw new MCRException("Error in UserStore.", ex);
        } finally {
            session.close();
        }

        return group;
    }

    public void createUserTables() {
        System.out.println("Create all user tables for hibernate User Store");
    }
}
