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
package org.mycore.services.urn;

/**
 * Stores the data of URNs and the document IDs assigned to them.
 * 
 * @author Frank Lützenkirchen
 */
public interface MCRURNStore {
    /** Returns true if the given urn is assigned to a document ID */
    public boolean isAssigned(String urn);

    /** Assigns the given urn to the given document ID */
    public void assignURN(String urn, String documentID);

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @return the urn, or null if no urn is assigned to this ID
     */
    public String getURNforDocument(String documentID);

    /**
     * Retrieves the document ID that is assigned to the given urn
     * 
     * @return the ID, or null if no ID is assigned to this urn
     */
    public String getDocumentIDforURN(String urn);

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     */
    public void removeURN(String urn);
}
