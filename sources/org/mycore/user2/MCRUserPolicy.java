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

/**
 * This class defines the policies of the MyCoRe user and group objects such as
 * required fields or password policy. It is implemented as a singleton since
 * there must not be two instances of this class.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserPolicy {
    /** ArrayList with Strings indicating required user fields */
    private ArrayList reqUserAttributes = null;

    /** ArrayList with Strings indicating required group fields */
    private ArrayList reqGroupAttributes = null;

    /** The one and only instance of this class */
    private static MCRUserPolicy theInstance = null;

    /** private constructor to create the singleton instance. */
    private MCRUserPolicy() {
        // For the moment this is hard coded but this will change soon....
        reqUserAttributes = new ArrayList();
        reqUserAttributes.add("numID");
        reqUserAttributes.add("userID");
        reqUserAttributes.add("password");
        reqUserAttributes.add("creator");
        reqUserAttributes.add("primary_group");

        reqGroupAttributes = new ArrayList();
        reqGroupAttributes.add("groupID");
        //reqGroupAttributes.add("creator");
    }

    /**
     * This method is the only way to get an instance of this class. It calls
     * the private constructor to create the singleton.
     * 
     * @return returns the one and only instance of <CODE>MCRUserPolicy</CODE>
     */
    public final static synchronized MCRUserPolicy instance() {
        if (theInstance == null) {
            theInstance = new MCRUserPolicy();
        }

        return theInstance;
    }

    /**
     * This method returns true if the given field is a required user attribute.
     * 
     * @param required
     *            string value representing a user attribute to check whether it
     *            is required
     */
    public boolean isRequiredForUser(String required) {
        return (reqUserAttributes.contains(required)) ? true : false;
    }

    /**
     * This method returns true if the given field is a required group
     * attribute.
     * 
     * @param required
     *            string value representing a group attribute to check whether
     *            it is required
     */
    public boolean isRequiredForGroup(String required) {
        return (reqGroupAttributes.contains(required)) ? true : false;
    }

    /**
     * @return This method returns a ArrayList of strings with the names of
     *         required user attributes.
     */
    public ArrayList getRequiredUserAttributes() {
        return reqUserAttributes;
    }

    /**
     * @return This method returns a ArrayList of strings with the names of
     *         required group attributes.
     */
    public ArrayList getRequiredGroupAttributes() {
        return reqGroupAttributes;
    }
}
