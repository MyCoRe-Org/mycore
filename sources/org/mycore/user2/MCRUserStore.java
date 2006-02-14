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

package org.mycore.user2;

import java.util.ArrayList;

import org.mycore.common.MCRException;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe user, group and
 * privilege information must implement this interface. Which database actually
 * will be used can then be configured by reading the value
 * <code>MCR.userstore_class_name</code> from mycore.properties.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public interface MCRUserStore {
    /**
     * This method creates a MyCoRe user object in the persistent datastore.
     * 
     * @param newUser
     *            the new user object to be stored
     */
    public void createUser(MCRUser newUser) throws MCRException;

    /**
     * This method creates a MyCoRe group object in the persistent datastore.
     * 
     * @param newGroup
     *            the new group object to be stored
     */
    public void createGroup(MCRGroup newGroup) throws MCRException;

    /**
     * This method deletes a MyCoRe user object in the persistent datastore.
     * 
     * @param delUserID
     *            a String representing the MyCoRe user object which is to be
     *            deleted
     */
    public void deleteUser(String delUserID) throws MCRException;

    /**
     * This method deletes a MyCoRe group object in the persistent datastore.
     * 
     * @param delGroupID
     *            a String representing the MyCoRe group object which is to be
     *            deleted
     */
    public void deleteGroup(String delGroupID) throws MCRException;

    /**
     * This method tests if a MyCoRe user object is available in the persistent
     * datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            looked for
     */
    public boolean existsUser(String userID) throws MCRException;

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
    public boolean existsUser(int numID, String userID) throws MCRException;

    /**
     * This method tests if a MyCoRe group object is available in the persistent
     * datastore.
     * 
     * @param groupID
     *            a String representing the MyCoRe group object which is to be
     *            looked for
     */
    public boolean existsGroup(String groupID) throws MCRException;

    /**
     * This method gets all user IDs and returns them as a ArrayList of strings.
     * 
     * @return ArrayList of strings including the user IDs of the system
     */
    public ArrayList getAllUserIDs() throws MCRException;

    /**
     * This method gets all group IDs and returns them as a ArrayList of
     * strings.
     * 
     * @return ArrayList of strings including the group IDs of the system
     */
    public ArrayList getAllGroupIDs() throws MCRException;

    /**
     * This method returns the maximum value of the numerical user IDs
     * 
     * @return maximum value of the numerical user IDs
     */
    public int getMaxUserNumID() throws MCRException;

    /**
     * This method gets all group IDs where a given user ID can manage the group
     * (i.e. is in the administrator user IDs list) as a ArrayList of strings.
     * 
     * @param userID
     *            a String representing the administrative user
     * @return ArrayList of strings including the group IDs of the system which
     *         have userID in their administrators list
     */
    public ArrayList getGroupIDsWithAdminUser(String userID) throws MCRException;

    /**
     * This method gets all user IDs with a given primary group and returns them
     * as a ArrayList of strings.
     * 
     * @param groupID
     *            a String representing a primary Group
     * @return ArrayList of strings including the user IDs of the system which
     *         have groupID as primary group
     */
    public ArrayList getUserIDsWithPrimaryGroup(String groupID) throws MCRException;

    /**
     * This method retrieves a MyCoRe user object from the persistent datastore.
     * 
     * @param userID
     *            a String representing the MyCoRe user object which is to be
     *            retrieved
     * @return the requested user object
     */
    public MCRUser retrieveUser(String userID) throws MCRException;

    /**
     * This method retrieves a MyCoRe group object from the persistent
     * datastore.
     * 
     * @param groupID
     *            a String representing the MyCoRe group object which is to be
     *            retrieved
     * @return the requested group object
     */
    public MCRGroup retrieveGroup(String groupID) throws MCRException;

    /**
     * This method updates a MyCoRe group object in the persistent datastore.
     * 
     * @param group
     *            the group to be updated
     */
    public void updateGroup(MCRGroup group) throws MCRException;


    /**
     * This method updates a MyCoRe user object in the persistent datastore.
     * 
     * @param user
     *            the user to be updated
     */
    public void updateUser(MCRUser user) throws MCRException;
}
