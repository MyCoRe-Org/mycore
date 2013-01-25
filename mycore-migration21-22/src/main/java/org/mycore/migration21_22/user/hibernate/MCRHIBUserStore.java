/*
 * 
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

package org.mycore.migration21_22.user.hibernate;

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
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.migration21_22.user.MCRGroup;
import org.mycore.migration21_22.user.MCRUser;
import org.mycore.migration21_22.user.MCRUserContact;

/**
 * This class implements the interface MCRUserStore - flushmode is configured to
 * COMMIT !
 * 
 * @author Matthias Kramm
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCRHIBUserStore {
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
     * This method tests if a MyCoRe user object is available in the persistent
     * datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            looked for
     */
    @SuppressWarnings("unchecked")
    public boolean existsUser(String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRUSERS> l = session.createQuery("from MCRUSERS where UID = '" + userID + "'").list();

        return !l.isEmpty();
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
    @SuppressWarnings("unchecked")
    public boolean existsUser(int numID, String userID) throws MCRException {
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRUSERS> l = session.createQuery("from MCRUSERS where NUMID = " + numID + " or UID = '" + userID + "'").list();

        return !l.isEmpty();
    }

    /**
     * This method retrieves a MyCoRe user object from the persistent datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            retrieved
     * @return the requested user object
     */
    @SuppressWarnings("unchecked")
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
            List<MCRGROUPMEMBERS> l = session.createQuery("from MCRGROUPMEMBERS where USERID = '" + userID + "'").list();

            for (MCRGROUPMEMBERS group : l) {
                groups.add(group.getGid().getGid());
            }

            // set some boolean values
            boolean id_enabled = idEnabled.equals("true");
            boolean update_allowed = updateAllowed.equals("true");

            // We create the user object
            try {
                MCRUserContact userContact = new MCRUserContact(salutation, firstname, lastname, street, city, postalcode, country, state, institution,
                    faculty, department, institute, telephone, fax, email, cellphone);
                retuser = new MCRUser(numID, userID, creator, created, modified, id_enabled, update_allowed, description, passwd, primaryGroupID, groups,
                    userContact);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return retuser;
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
        Criteria c = session
            .createCriteria(MCRGROUPADMINS.class)
            .setProjection(Projections.property("key.gid.gid"))
            .add(Restrictions.eq("key.userid", session.get(MCRUSERS.class, userID)));
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
     * This method retrieves a MyCoRe group object from the persistent
     * datastore.
     * 
     * @param groupID
     *            a String representing the MyCoRe group object which is to be
     *            retrieved
     * @return the requested group object
     */
    @SuppressWarnings("unchecked")
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
            List<MCRGROUPADMINS> groupAdmins = session.createQuery("from MCRGROUPADMINS where GID = '" + groupID + "'").list();

            ArrayList<String> admUserIDs = new ArrayList<String>();
            ArrayList<String> admGroupIDs = new ArrayList<String>();

            if (!groupAdmins.isEmpty()) {
                // may have rootGroup and rootUser here several time
                // make a temporarily set to filter dublicate entries
                Set<String> admUserSet = new HashSet<String>();
                Set<String> admGroupSet = new HashSet<String>();
                for (MCRGROUPADMINS grpadmins : groupAdmins) {
                    if (grpadmins.getUserid() != null) {
                        admUserSet.add(grpadmins.getUserid().getUid());
                    }

                    if (grpadmins.getGroupid() != null) {
                        admGroupSet.add(grpadmins.getGroupid().getGid());
                    }
                }
                admUserIDs.addAll(admUserSet);
                admGroupIDs.addAll(admGroupSet);
            }

            List<MCRGROUPMEMBERS> groupMembers = session.createQuery("from MCRGROUPMEMBERS where GID = '" + groupID + "'").list();

            ArrayList<String> mbrUserIDs = new ArrayList<String>();

            Set<String> users = new HashSet<String>();

            if (!groupMembers.isEmpty()) {
                for (MCRGROUPMEMBERS grpmembers : groupMembers) {
                    if (grpmembers.getUserid() != null) {
                        users.add(grpmembers.getUserid().getUid());
                    }
                }
            }

            // Add all users with groupID as primary group
            List<String> userIds = session
                .createCriteria(MCRUSERS.class)
                .setProjection(Projections.property("uid"))
                .add(Restrictions.eq("primgroup", groups))
                .list();
            for (String uid : userIds) {
                users.add(uid);
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
