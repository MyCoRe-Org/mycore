package org.mycore.datamodel.common;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * central generator for new MyCoRe Object IDs.
 *
 * @author Robert Stephan
 *
 */
public interface MCRObjectIDGenerator {

    /**
     * Returns a MCRObjectID from a given base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>.
     * The number is computed by this method.
     *
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     */
    public default MCRObjectID getNextFreeId(String baseId) {
        return getNextFreeId(baseId, 0);
    }

    /**
     * Returns a MCRObjectID from a given the components of a base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number is computed by this method.
     *
     * @param projectId
     *            The first component of <em>project_id</em>_<em>type_id</em>
     * @param type
     *            The second component of <em>project_id</em>_<em>type_id</em>
     */
    public default MCRObjectID getNextFreeId(String projectId, String type) {
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
    public MCRObjectID getNextFreeId(String baseId, int maxInWorkflow);

    /**
     * Returns the last ID used or reserved for the given object base type.
     *
     * @return a valid MCRObjectID, or null when there is no ID for the given
     *         type
     */
    public MCRObjectID getLastID(String baseId);

}
