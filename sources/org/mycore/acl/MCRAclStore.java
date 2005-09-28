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

package org.mycore.acl;

// /============================================================================§

/**
 * This interface defines method to store and retrieve ACLs to or from
 * persistant storage.
 * 
 * @author Benno Süselbeck
 * @version 1.0.0, 01.11.2003
 */
public interface MCRAclStore {
    // /============================================================================/

    /**
     * Retrieves an ACL from a persistence layer.
     * 
     * <P>
     * Implementations will extract some information from the guarded object,
     * e.g. an object ID, and use this as key in database table to obtain a
     * description for the ACL, e.g. as XML. The ACL is created from this
     * description and returned.
     * 
     * <P>
     * This method is normally called in a contructor of an object which is
     * itself retrieved from persistant storage. The internal ACL is then
     * modified with information retrieved form the persistance layer.
     * 
     * @param object
     *            the guarded object which wants to retrieve it's ACL
     */
    public boolean retrieveAcl(MCRAclGuarded object);

    // -------------------------------------------------------------------------------

    /**
     * Stores an ACL in a persistence layer.
     * 
     * This method is normally called when objects are serialized to persistant
     * storage. The ACL is made persistent, e.g. in XML format, and then stored
     * as part of the object description or in a separate database table
     * connecting object IDs with´ACLs.
     * 
     * @param object
     *            the guarded object which wants to store it's ACL
     * 
     * @return true if storing was successful.
     */
    public boolean storeAcl(MCRAclGuarded object);

    // -============================================================================\
}
