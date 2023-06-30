package org.mycore.datamodel.common;

import java.util.HashMap;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * central generator for new MyCoRe Object IDs.
 *
 * @author Robert Stephan
 *
 */
 // code was moved from MCRObjectID into this class
public class MCRObjectIDGenerator {
    
    // counter for the next IDs per project base ID
    private static HashMap<String, Integer> lastNumber = new HashMap<>();
    
    /**
     * First invocation may return MCR.Metadata.ObjectID.InitialNumberDistance if set,
     * following invocations will return MCR.Metadata.ObjectID.NumberDistance.
     * The default for both is 1.
     */
    private static int numberDistance = MCRConfiguration2.getInt("MCR.Metadata.ObjectID.InitialNumberDistance")
        .orElse(MCRConfiguration2.getInt("MCR.Metadata.ObjectID.NumberDistance").orElse(1));

    /**
     * Returns a MCRObjectID from a given base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number is computed by this
     * method. It is the next free number of an item in the database for the
     * given project ID and type ID, with the following additional restriction:
     * The ID returned can be divided by idFormat.numberDistance without remainder.
     * The ID returned minus the last ID returned is at least idFormat.numberDistance.
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
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     */
    public static synchronized MCRObjectID getNextFreeId(String baseId) {
        return getNextFreeId(baseId, 0);
    }

    /**
     * Returns a MCRObjectID from a given the components of a base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number is computed by this
     * method. It is the next free number of an item in the database for the
     * given project ID and type ID, with the following additional restriction:
     * The ID returned can be divided by idFormat.numberDistance without remainder.
     * The ID returned minus the last ID returned is at least idFormat.numberDistance.
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
     * @param projectId
     *            The first component of <em>project_id</em>_<em>type_id</em>
     * @param type
     *            The second component of <em>project_id</em>_<em>type_id</em>
     */
    public static synchronized MCRObjectID getNextFreeId(String projectId, String type) {
        return getNextFreeId(projectId + "_" + type);
    }

    /**
     * Returns a MCRObjectID from a given base ID string. Same as
     * {@link #getNextFreeId(String)} but the additional parameter acts as a
     * lower limit for integer part of the ID.
     *
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     * @param maxInWorkflow
     *            returned integer part of id will be at least
     *            <code>maxInWorkflow + 1</code>
     */
    public static synchronized MCRObjectID getNextFreeId(String baseId, int maxInWorkflow) {
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
     * Returns the last ID number used or reserved for the given object base
     * type. This may return the value 0 when there is no ID last used or in the
     * store.
     */
    private static int getLastIDNumber(String baseId) {
        int lastIDKnown = lastNumber.getOrDefault(baseId, 0);
        int highestStoredID = MCRXMLMetadataManager.instance().getHighestStoredID(baseId);

        return Math.max(lastIDKnown, highestStoredID);
    }

    /**
     * Returns the last ID used or reserved for the given object base type.
     *
     * @return a valid MCRObjectID, or null when there is no ID for the given
     *         type
     */
    public static synchronized MCRObjectID getLastID(String baseId) {
        int lastIDNumber = getLastIDNumber(baseId);
        if (lastIDNumber == 0) {
            return null;
        }
        return MCRObjectID.getInstance(MCRObjectID.formatID(baseId, lastIDNumber));
    }
}
