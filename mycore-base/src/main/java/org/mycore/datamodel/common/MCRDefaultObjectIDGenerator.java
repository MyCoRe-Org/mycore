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
package org.mycore.datamodel.common;

import java.util.HashMap;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * default implementation for generator of MyCoRe Object IDs.
 *
 * @author Robert Stephan
 *
 */
 // code was moved from MCRObjectID into this class
public class MCRDefaultObjectIDGenerator implements MCRObjectIDGenerator {
    
    // counter for the next IDs per project base ID
    private HashMap<String, Integer> lastNumber = new HashMap<>();
    
    /**
     * First invocation may return MCR.Metadata.ObjectID.InitialNumberDistance if set,
     * following invocations will return MCR.Metadata.ObjectID.NumberDistance.
     * The default for both is 1.
     */
    private int numberDistance = MCRConfiguration2.getInt("MCR.Metadata.ObjectID.InitialNumberDistance")
        .orElse(MCRConfiguration2.getInt("MCR.Metadata.ObjectID.NumberDistance").orElse(1));

   
    /**
     * Returns a MCRObjectID from a given base ID string
     * The additional parameter acts as a
     * lower limit for integer part of the ID.
     * 
     * Otherwise it is the next free number of an item in the database for the
     * given project ID and type ID, with the following additional restriction:
     * The ID returned can be divided by the configured numberDistance without remainder.
     * The ID returned minus the last ID returned is at least the configured numberDistance.
     *
     * Example for number distance of 1 (default):
     *   last ID = 7, next ID = 8
     *   last ID = 8, next ID = 9
     *
     * Example for number distance of 2:
     *   last ID = 7, next ID = 10
     *   last ID = 8, next ID = 10
     *   last ID = 10, next ID = 20
     * 
     *
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     * @param maxInWorkflow
     *            returned integer part of id will be at least
     *            <code>maxInWorkflow + 1</code>
     */
    public synchronized MCRObjectID getNextFreeId(String baseId, int maxInWorkflow) {
        int last = Math.max(getLastIDNumber(baseId), maxInWorkflow);
        int next = last + numberDistance;

        int rest = next % numberDistance;
        if (rest != 0) {
            next += numberDistance - rest;
        }

        lastNumber.put(baseId, next);
        return MCRObjectID.getInstance(MCRObjectID.formatID(baseId, next));
    }
    
    /**
     * Returns the last ID used or reserved for the given object base type.
     *
     * @return a valid MCRObjectID, or null when there is no ID for the given
     *         type
     */
    public synchronized MCRObjectID getLastID(String baseId) {
        int lastIDNumber = getLastIDNumber(baseId);
        if (lastIDNumber == 0) {
            return null;
        }
        return MCRObjectID.getInstance(MCRObjectID.formatID(baseId, lastIDNumber));
    }
    
    /**
     * Returns the last ID number used or reserved for the given object base
     * type. This may return the value 0 when there is no ID last used or in the
     * store.
     */
    private int getLastIDNumber(String baseId) {
        int lastIDKnown = lastNumber.getOrDefault(baseId, 0);
        int highestStoredID = MCRXMLMetadataManager.instance().getHighestStoredID(baseId);

        return Math.max(lastIDKnown, highestStoredID);
    }
}
