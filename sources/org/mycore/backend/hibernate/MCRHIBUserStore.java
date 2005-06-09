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
import org.mycore.backend.hibernate.tables.*;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrivilege;
import org.mycore.user.MCRPrivilegeSet;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserStore;
import org.mycore.user.*;

import org.hibernate.*;

import java.sql.Timestamp;

/**
 * This class implements the interface MCRUserStore
 * 
 * @author Matthias Kramm
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore implements MCRUserStore {
    static Logger logger = Logger.getLogger(MCRHIBUserStore.class.getName());

    /**
     * The constructor reads the names of the SQL tables which hold the user
     * information data from mycore.properties.
     */
    public MCRHIBUserStore() {
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
        
        // insert values
        String idEnabled = (newUser.isEnabled()) ? "true" : "false";
        String updateAllowed = (newUser.isUpdateAllowed()) ? "true" : "false";
        MCRUserContact userContact = newUser.getUserContact();
        
        user.setNumid(newUser.getNumID());
        user.setUid(newUser.getID());
        user.setCreator(newUser.getCreator());
        user.setCreationdate(newUser.getCreationDate());
        user.setModifieddate(newUser.getModifiedDate());
        user.setDescription(newUser.getDescription());
        user.setPasswd(newUser.getPassword());
        user.setEnabled(idEnabled);
        user.setUpd(updateAllowed);
        user.setSalutation(userContact.getSalutation());
        user.setFirstname(userContact.getFirstName());
        user.setLastname(userContact.getLastName());
        user.setStreet(userContact.getStreet());
        user.setCity(userContact.getCity());
        user.setPostalcode(userContact.getPostalCode());
        user.setCountry(userContact.getCountry());
        user.setState(userContact.getState());
        user.setInstitution(userContact.getInstitution());
        user.setFaculty(userContact.getFaculty());
        user.setDepartment(userContact.getDepartment());
        user.setInstitute(userContact.getInstitute());
        user.setTelephone(userContact.getTelephone());
        user.setFax(userContact.getFax());
        user.setEmail(userContact.getEmail());
        user.setCellphone(userContact.getCellphone());
        user.setPrimgroup(newUser.getPrimaryGroupID());

        session.save(user);
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        session.delete("from MCRUSERS where MCRID = '" + delUserID + "'");
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where UID = '" + userID + "'").list();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where NUMID = '" + numID + "' or UID = '" + userID + "'").list();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where UID = '" + userID +"'").list();
        
        MCRUSERS user = (MCRUSERS) l.get(0);
        
        int numID = user.getNumid();
        String creator = user.getCreator();
        Timestamp created =  user.getCreationdate() ;
        Timestamp modified =  user.getModifieddate();
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
        l = session.createQuery("from MCRGROUPMEMBERS where USERID = '" + userID +"'").list();

        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): User with ID = "
                    + userID + " is no groupmember.";
            MCRGROUPMEMBERS group;
            
           for (int i=0; i< l.size();i++){
               group =(MCRGROUPMEMBERS) l.get(i);
               groups.add(group.getGid());
               System.out.println("-"+group.getGid());
            }

           throw new MCRException(msg);
        }
        
        // set some boolean values
        boolean id_enabled = (idEnabled.equals("true")) ? true : false;
        boolean update_allowed = (updateAllowed.equals("true")) ? true
                : false;
        // We create the user object
            MCRUser retuser = null;
            try {
                retuser = new MCRUser(numID, userID, creator,
                        created, modified, id_enabled, 
                        update_allowed, description, passwd,
                        primaryGroupID, groups, salutation, 
                        firstname, lastname, street, city,
                        postalcode, country, state,
                        institution, faculty, department,
                        institute, telephone, fax, email,
                        cellphone);
            } catch (MCRException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        tx.commit(); 
        session.close();
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
        if(l.size() < 1){
            user =(MCRUSERS) l.get(0);
            
            String isEnabled = (updUser.isEnabled()) ? "true" : "false";
            String updateAllowed = (updUser.isUpdateAllowed()) ? "true" : "false";
            
            user.setNumid(updUser.getNumID());
            user.setUid(updUser.getID());
            user.setCreator(updUser.getCreator());
            user.setCreationdate(updUser.getCreationDate()) ;
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
        
        }else{
            tx.commit();
            session.close();
            throw new MCRException("User " + updUser.getID()+ " not found."); 
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
        List l = session.createQuery("from MCRUSERS").list();
        
        ArrayList list = new ArrayList();
        
        for (int i=0; i<l.size(); i++){
            MCRUSERS user = (MCRUSERS) l.get(i);
            list.add(user.getUid()); 
        }
        tx.commit();
        session.close();
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
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS").list();
        tx.commit();
        session.close();
        if (l.size() > 0){
            MCRUSERS user = (MCRUSERS) l.get(l.size()-1);
            ret = user.getNumid();
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
            for (int i = 0; i < newGroup.getMemberUserIDs().size(); i++){
                MCRGROUPMEMBERS member = new MCRGROUPMEMBERS();
                member.setGid(newGroup.getID());
                member.setUserid((String) newGroup.getMemberUserIDs().get(i));
                session.save(member);
            }
 
            for (int i = 0; i < newGroup.getMemberGroupIDs().size(); i++){
                MCRGROUPMEMBERS member = new MCRGROUPMEMBERS();
                member.setGid(newGroup.getID());
                member.setGroupid((String) newGroup.getMemberGroupIDs().get(i));
                session.save(member);
            }
            
            // Groupadmins
            for (int i = 0; i < newGroup.getMemberUserIDs().size(); i++){
                MCRGROUPADMINS admin = new MCRGROUPADMINS();
                admin.setGid(newGroup.getID());
                admin.setUserid((String) newGroup.getMemberUserIDs().get(i));
                session.save(admin);
            }
            
            for (int i = 0; i < newGroup.getMemberGroupIDs().size(); i++){
                MCRGROUPADMINS admin = new MCRGROUPADMINS();
                admin.setGid(newGroup.getID());
                admin.setUserid((String) newGroup.getMemberGroupIDs().get(i));
                session.save(admin);
            }
            
            // Privileges lookup
            for (int i = 0; i < newGroup.getPrivileges().size(); i++) {
                MCRPRIVSLOOKUP privslookup = new MCRPRIVSLOOKUP();
                privslookup.setGid(newGroup.getID());
                privslookup.setName((String) newGroup.getPrivileges().get(i));
                session.save(privslookup);
            }
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw new MCRException("Error in UserStore.", e);
        }finally{
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        session.delete("from MCRGROUPS where GID = '" + delGroupID + "'");
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRGROUPS").list();
        tx.commit();
        session.close();
        if (l.size() > 0){
            ArrayList retList = new ArrayList();
            for (int i=0;i<l.size();i++){
                MCRGROUPS group = (MCRGROUPS) l.get(i);
                retList.add((String) group.getGid());
            }
            return retList;
        }else{
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
    public synchronized ArrayList getGroupIDsWithAdminUser(String userID)
            throws MCRException {
        ArrayList retList = new ArrayList();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRGROUPADMINS where USERID = '" + userID + "'").list();
        tx.commit();
        if (l.size() > 0){ 
            for (int i=0;i<l.size();i++){
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
    public synchronized ArrayList getUserIDsWithPrimaryGroup(String groupID)
            throws MCRException {
        ArrayList retList = new ArrayList();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRUSERS where PRIMGROUP = '" + groupID + "'").list();
        tx.commit();
        if (l.size() > 0){
            for (int i=0;i<l.size();i++){
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
        MCRGROUPS groups = new MCRGROUPS();
        try {
            groups.setGid(group.getID());
            groups.setDescription(group.getDescription());
            groups.setModifieddate(group.getModifiedDate());
 
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            
            String hqlUpdate = "update MCRGROUPS set CREATOR '=" + group.getCreator() + "'," +
            		" CREATIONDATE = '" + group.getCreationDate() + "'" +
            		" DESCRIPTION = '" + group.getDescription() + "'" +
            		" MODIFOEDDATE = '" + group.getModifiedDate() + "'" +
            		" where GID = '" + group.getID() + "'";
            int updatedEntities = session.createQuery( hqlUpdate )
            .executeUpdate();

            // update groupmembers
            ArrayList oldAdminUserIDs = new ArrayList();
            ArrayList oldAdminGroupIDs = new ArrayList();
            ArrayList newAdminGroupIDs = group.getAdminGroupIDs();
            ArrayList newAdminUserIDs = group.getAdminUserIDs();
            List l = session.createQuery("from MCRGROUPADMINS where GID='" + group.getID() + "'").list();
            for (int i = 0; i < l.size(); i++){
                MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);
                if (grpadmins.getUserid()!=null && ! grpadmins.getUserid().equals("")){
                    oldAdminUserIDs.add(grpadmins.getUserid());
            	}
                if (grpadmins.getGroupid()!=null && ! grpadmins.getGroupid().equals("")){
                    oldAdminGroupIDs.add(grpadmins.getGroupid());
            	}
            }
            
            // insert
            for (int i = 0; i < newAdminUserIDs.size(); i++) {
                if (!oldAdminUserIDs.contains(newAdminUserIDs.get(i))) {
                    MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
                    grpadmins.setGroupid(group.getID());
                    grpadmins.setUserid((String) newAdminUserIDs.get(i));
                    session.save(grpadmins);
                }
            }
    		for (int i = 0; i < newAdminGroupIDs.size(); i++) {
    		    if (!oldAdminGroupIDs.contains(newAdminGroupIDs.get(i))) {
    		        MCRGROUPADMINS grpadmins = new MCRGROUPADMINS();
    		        grpadmins.setGid(group.getID());
    		        grpadmins.setGroupid((String) newAdminGroupIDs.get(i));
    		        session.save(grpadmins);
 		    	}
    		}
  		
    		// We search for the recently removed admins and remove them from
            // the table
    		for (int i = 0; i < oldAdminUserIDs.size(); i++) {
                if (!newAdminUserIDs.contains(oldAdminUserIDs.get(i))) {
                    int deletedEntities = session.createQuery("delete MCRGROUPADMINS " +
                    		"where GID = '" + group.getID() + "'" +
                    		" and GROUPID = '" + (String) oldAdminUserIDs.get(i) + "'")
                    .executeUpdate();
                }
            }
            for (int i = 0; i < oldAdminGroupIDs.size(); i++) {
                if (!newAdminGroupIDs.contains(oldAdminGroupIDs.get(i))) {
                    int deletedEntities = session.createQuery("delete MCRGROUPADMINS " +
                    		"where GID = '" + group.getID() + "'" +
                    		" and USERID = '" + (String) oldAdminGroupIDs.get(i) + "'")
                    .executeUpdate();
                }
            }
            
            // Now we update the membership lookup table. First we collect
            // information about which users have been added or removed.
            // Therefore we compare the list of users this group has as members
            // before and after the update.
            ArrayList oldUserIDs = new ArrayList();
            ArrayList newUserIDs = group.getMemberUserIDs();
            l = session.createQuery("from MCRGROUPMEMBERS where GID='" + group.getID() + "' and USERID is not null").list();
            for (int i = 0; i < l.size(); i++){
                MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);
                oldUserIDs.add((String) grpmembers.getUserid());
            }
  
            // We search for the new members and insert them into the lookup
            // table
            for (int i = 0; i < newUserIDs.size(); i++) {
                if (!oldUserIDs.contains(newUserIDs.get(i))) {
                    MCRGROUPMEMBERS grpmembers = new MCRGROUPMEMBERS();
                    grpmembers.setGid(group.getID());
                    grpmembers.setUserid((String) newUserIDs.get(i));
                    session.save(grpmembers);
                }
            }
            
            // We search for the users which have been removed from this group
            // and delete the entries from the member lookup table
            for (int i = 0; i < oldUserIDs.size(); i++) {
                if (!newUserIDs.contains(oldUserIDs.get(i))) {                
                    int deletedEntities = session.createQuery(
                            "delete MCRGROUPMEMBERS where GID = '" + group.getID() + "' " +
                            		"and USERID ='"+ (String) oldUserIDs.get(i) +"'")
                    .executeUpdate();
                }
            }

            // Now we collect information about which groups have been added or
            // removed. Therefore we compare the list of groups this group has
            // as members before and after the update.
            ArrayList oldGroupIDs = new ArrayList();
            ArrayList newGroupIDs = group.getMemberGroupIDs();
            
            l = session.createQuery("from MCRGROUPMEMBERS where GID='" + group.getID() + "' and GROUPID is not null").list();
            
            for (int i = 0; i < l.size(); i++){
                MCRGROUPMEMBERS grpmembers = new MCRGROUPMEMBERS();
                oldGroupIDs.add((String) grpmembers.getGroupid());
            }

            // We search for the new members and insert them into the lookup
            // table
            for (int i = 0; i < newGroupIDs.size(); i++) {
                if (!oldGroupIDs.contains(newGroupIDs.get(i))) {
                    MCRGROUPMEMBERS grpmembers = new MCRGROUPMEMBERS();
                    grpmembers.setGid(group.getID());
                    grpmembers.setGroupid((String) newGroupIDs.get(i));
                    session.save(grpmembers);
                }
            }

            // We search for the groups which have been removed from this group
            // and delete the entries from the member lookup table
            for (int i = 0; i < oldGroupIDs.size(); i++) {
                if (!newGroupIDs.contains(oldGroupIDs.get(i))) {
                    int deletedEntities = session.createQuery(
                            "delete MCRGROUPMEMBERS where GID = '" + group.getID() + "' " +
                            		"and USERID ='"+ (String) oldGroupIDs.get(i) +"'")
                    .executeUpdate();
                }
            }
            
            // Now we collect information about which privileges have been added
            // or removed. Therefor we compare the list of privileges this group
            // has before and after the update.
            ArrayList oldPrivs = new ArrayList();
            ArrayList newPrivs = group.getPrivileges();
            l = session.createQuery("from MCRPRIVSLOOKUP where GID='" + group.getID() + "'").list();
            for (int i = 0; i < l.size(); i++){
                MCRPRIVSLOOKUP privsLookup = new MCRPRIVSLOOKUP();
                oldPrivs.add((String) privsLookup.getName());
            }

            // We search for new privileges and insert them into the lookup
            // table
            for (int i = 0; i < newPrivs.size(); i++) {
                if (!oldPrivs.contains(newPrivs.get(i))) {
                    MCRPRIVSLOOKUP privsLookup = new MCRPRIVSLOOKUP();
                    privsLookup.setGid(group.getID());
                    privsLookup.setName((String) newPrivs.get(i));
                    session.save(privsLookup);
                }
            }

            // We search for the privileges which have been removed from this
            // group and delete the entries from the privilege lookup table
            for (int i = 0; i < oldPrivs.size(); i++) {
                if (!newPrivs.contains(oldPrivs.get(i))) {
                    int deletedEntities = session.createQuery(
                            "delete MCRPRIVSLOOKUP where GID = '" + group.getID() + "' " +
                            		"and NAME ='"+ (String) oldPrivs.get(i) +"'")
                    .executeUpdate();
                }
            }
            
            tx.commit();
            session.close();
        } catch (MCRPersistenceException e) {
            new MCRException("Error in UserStore.", e);
        } catch (HibernateException e) {
            new MCRException("Hibernate error in UserStore.", e);
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
    public synchronized MCRGroup retrieveGroup(String groupID)
            throws MCRException {
        Session session = null;
        try {
            session = MCRHIBConnection.instance().getSession();
            List l = session.createQuery("from MCRGROUPS where GID = '"
                    + groupID + "'").list();
            
            if (l.size() == 0){
                String msg = "MCRHIMLUserStore.retrieveGroup():" +
                		" There is no group with ID = " + groupID;
                throw new MCRException(msg);
            }
            MCRGROUPS groups = (MCRGROUPS) l.get(0);
            
            String creator = groups.getCreator();
            Timestamp created = groups.getCreationdate();
            Timestamp modified = groups.getModifieddate();
            String description = groups.getDescription();

            List l1 = session.createQuery("from MCRPRIVSLOOKUP where GID = '"
                    + groupID + "' and NAME is not null").list();
            ArrayList privs = new ArrayList();
            for(int i=0; i<l1.size(); i++){
                MCRPRIVSLOOKUP privslookup = (MCRPRIVSLOOKUP) l1.get(i);
                privs.add(privslookup.getName());
            }
            
            // Now lookup the lists of admin users, admin groups, users
            // (members) and privileges
            l = session.createQuery("from MCRGROUPADMINS where GID = '"
                    + groupID + "'").list();
            ArrayList admUserIDs = new ArrayList();
            ArrayList admGroupIDs = new ArrayList();
            if (l.size() > 0){
                for(int i=0; i<l.size();i++){
                    MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);
                    if(grpadmins.getUserid() != null 
                            &&  ! grpadmins.getUserid().equals("")){
                        admUserIDs.add(grpadmins.getUserid());
                    }
                    if(grpadmins.getGroupid() != null 
                            && ! grpadmins.getGroupid().equals("")){
                        admGroupIDs.add(grpadmins.getGroupid());
                    }  
                }
            }

            l = session.createQuery("from MCRGROUPMEMBERS where GID = '"
                    + groupID + "'").list();
            ArrayList mbrUserIDs = new ArrayList();
            ArrayList mbrGroupIDs = new ArrayList();
            if (l.size() > 0){
                for(int i=0; i<l.size();i++){
                    MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);
                    if(grpmembers.getUserid() != null && ! grpmembers.getUserid().equals("")){
                        mbrUserIDs.add(grpmembers.getUserid());
                    }
                    if(grpmembers.getGroupid() != null && ! grpmembers.getGroupid().equals("")){
                        mbrGroupIDs.add(grpmembers.getGroupid());
                    }
                }
            }
            
            l = session.createQuery("from MCRGROUPMEMBERS where GROUPID = '"
                    + groupID + "'").list();
            ArrayList groupIDs = new ArrayList();
            if (l.size()>0){
                for(int i=0; i<l.size(); i++){
                    MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);
                    if(grpmembers.getGid() != null && ! grpmembers.getGid().equals("")){
                        groupIDs.add(grpmembers.getGid());
                    }
                }
            }

           //
            // We create the group object
            MCRGroup group = new MCRGroup(groupID, creator, created, modified,
                    description, admUserIDs, admGroupIDs, mbrUserIDs,
                    mbrGroupIDs, groupIDs, privs);
            return group;

        }catch (Exception ex) {
            throw new MCRException("Error in UserStore.", ex);
        }finally{
            session.close();
        }
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
        Session session = MCRHIBConnection.instance().getSession();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRPRIVSM where NAME = '"
                + privName + "'").list();
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRPRIVSM").list();
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
        Session session = MCRHIBConnection.instance().getSession();
        ArrayList privileges = new ArrayList();
        MCRPrivilege thePrivilege;
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRPRIVSM").list();
        for (int i=0; i<l.size();i++){
            MCRPRIVSM privs = (MCRPRIVSM) l.get(i);
            thePrivilege = new MCRPrivilege(privs.getName(), 
                    privs.getDescription());
            privileges.add(thePrivilege);
        } 
        tx.commit();
        session.close();
        return privileges;
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
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        
        ArrayList privileges = privilegeSet.getPrivileges();
        try{
            for(int i = 0; i < privileges.size(); i++){
                MCRPrivilege thePrivilege = (MCRPrivilege) privileges.get(i);
                if (!existsPrivilege(thePrivilege.getName())) {
                    // insert
                    MCRPRIVSM priv = new MCRPRIVSM();
                    priv.setName(thePrivilege.getName());
                    priv.setDescription(thePrivilege.getDescription());
                    session.save(priv);
               }else{
                    // update
                    String hqlUpdate= "update MCRPRIVS set DESCRIPTION = '" + thePrivilege.getDescription() + "'" +
            		" where NAME = '" + thePrivilege.getName() + "'";
                    int updatedEntities = session.createQuery(hqlUpdate)
                    		.executeUpdate();
               }
            }
            tx.commit();
        }catch (Exception ex) {
            tx.rollback();
            throw new MCRException("Error in UserStore.", ex);
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
        throw new IllegalStateException(
                "Hibernate backend doesn't support direct SQL queries");
    }

}
