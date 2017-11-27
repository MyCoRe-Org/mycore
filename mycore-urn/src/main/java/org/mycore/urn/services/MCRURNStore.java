/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.urn.services;

import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.urn.hibernate.MCRURN;

/**
 * Stores the data of URNs and the document IDs assigned to them.
 * 
 * @author Frank Lützenkirchen
 */
public interface MCRURNStore {
    /** Returns true if the given urn is assigned to a document ID */
    boolean isAssigned(String urn);

    /** Assigns the given urn to the given document ID */
    void assignURN(String urn, String documentID);

    /** Assigns the given urn to the given derivate ID */
    void assignURN(String urn, String derivate, String path, String filename);

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @return the urn, or null if no urn is assigned to this ID
     */
    String getURNforDocument(String documentID);

    /**
     * Retrieves the document ID that is assigned to the given urn
     * 
     * @return the ID, or null if no ID is assigned to this urn
     */
    String getDocumentIDforURN(String urn);

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     */
    void removeURN(String urn);

    /**
     * Removes the urn (and assigned document ID) from the persistent store by the 
     * given object id
     */
    void removeURNByObjectID(String objID);

    /**Checks wether an object or derivate has an urn assigned*/
    boolean hasURNAssigned(String objId);

    String getURNForFile(String derivateId, String path, String fileName);

    /**
     * @return the count of urn matching the given 'registered' attribute
     */
    long getCount(boolean registered);

    List<MCRURN> get(boolean registered, int start, int rows);

    void update(MCRURN urn);

    /**
     * Get all URN for the given object id.
     */
    List<MCRURN> get(MCRObjectID id);

    /**
     * @return a {@link List} of {@link MCRURN} where path and file name are just blanks or null;
     */
    List<MCRURN> getBaseURN(boolean registered, boolean dfg, int start, int rows);
}
