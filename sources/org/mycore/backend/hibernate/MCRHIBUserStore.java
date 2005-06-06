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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.backend.hibernate.tables.*;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.common.MCRException;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRPrivilege;
import org.mycore.user.MCRPrivilegeSet;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserStore;
import org.mycore.user.*;

import org.hibernate.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * This class implements the interface MCRUserStore
 * 
 * @author Matthias Kramm
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore implements MCRUserStore {
    static Logger logger = Logger.getLogger(MCRHIBUserStore.class.getName());

    //private SessionFactory sessionFactory;

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
        createUser(updUser);
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
             
             int numID = user.getNumid();
             String userID = user.getUid();
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
             
             boolean id_enabled = (idEnabled.equals("true")) ? true : false;
             boolean update_allowed = (updateAllowed.equals("true")) ? true
                     : false;
             
             MCRUser retuser = null;
             ArrayList groups = new ArrayList();
             
             
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
             
             list.add(retuser);

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
        /**
         * TODO: Change behaviour
         */
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
        MCRGROUPS u = new MCRGROUPS();
        /**
         * TODO: impement correct behaviour
         */
       /* if (newGroup instanceof MCRGroupExt) {
            u = (MCRGroupExt) newGroup;
        } else {
            u = new MCRGroupExt(newGroup);
        }*/

        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        session.save(u);
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
        Session session = null;
        try {
            session = MCRHIBConnection.instance().getSession();
            List l = session.createQuery("from MCRGROUPS where GID = '" + groupID + "'").list();
            
            if (l.size() == 0){
                String msg = "MCRHIMLUserStore.retrieveGroup(): There is no group with ID = "
                    + groupID;
                throw new MCRException(msg);
            }
            MCRGROUPS groups = (MCRGROUPS) l.get(0);
            
            String creator = groups.getCreator();
            Timestamp created = groups.getCreationdate();
            Timestamp modified = groups.getModifieddate();
            String description = groups.getDescription();
            
            // Now lookup the lists of admin users, admin groups, users
            // (members)
            // and privileges
            l = session.createQuery("from MCRGROUPADMINS where GID = '" + groupID + "'").list();
            ArrayList admUserIDs = new ArrayList();
            ArrayList admGroupIDs = new ArrayList();
            if (l.size() > 0){
                for(int i=0; i<l.size();i++){
                    MCRGROUPADMINS grpadmins = (MCRGROUPADMINS) l.get(i);
                    if(grpadmins.getUserid() != null &&  ! grpadmins.getUserid().equals("")){
                        admUserIDs.add(grpadmins.getUserid());
                    }
                    if(grpadmins.getGroupid() != null && ! grpadmins.getGroupid().equals("")){
                        admGroupIDs.add(grpadmins.getGroupid());
                    }  
                }
            }

            l = session.createQuery("from MCRGROUPMEMBERS where GID = '" + groupID + "'").list();
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
            
            l = session.createQuery("from MCRGROUPMEMBERS where GROUPID = '" + groupID + "'").list();
            ArrayList groupIDs = new ArrayList();
            if (l.size()>0){
                for(int i=0; i<l.size();i++){
                    MCRGROUPMEMBERS grpmembers = (MCRGROUPMEMBERS) l.get(i);
                    if(grpmembers.getGid() != null && ! grpmembers.getGid().equals("")){
                        groupIDs.add(grpmembers.getGid());
                    }
                }
            }

            l = session.createQuery("from MCRPRIVSLOOKUP where GID = '" + groupID + "'").list();
            ArrayList privs = new ArrayList();
            MCRPRIVSLOOKUP privslookup = new MCRPRIVSLOOKUP();
            for(int i=0; i<l.size();i++){
                privslookup = (MCRPRIVSLOOKUP) l.get(i);
                if(privslookup.getName() != null && ! privslookup.getName().equals("")){
                    privs.add(privslookup.getName());
                }
            }
            // We create the group object
            MCRGroup group = new MCRGroup(groupID, creator, created, modified,
                    description, admUserIDs, admGroupIDs, mbrUserIDs,
                    mbrGroupIDs, groupIDs, privs);
            return group;

        }catch (Exception ex) {
            System.out.println(ex.toString());
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
        List l = session.createQuery("from MCRPRIVSM where NAME = '" + privName + "'").list();
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
