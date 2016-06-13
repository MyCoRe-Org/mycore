package org.mycore.datamodel.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Experimental class to improve performance on delete and update operations.
 * You can mark object's as "will be deleted" or "will be updated". You can
 * use this information on {@link MCREventHandler}'s to exclude those
 * marked objects from operations which makes no sense.
 * 
 * <p>
 * Current behavior:
 * </p>
 * <ol>
 *   <li>An user delete's a parent object with 500 children.</li>
 *   <li>MyCoRe tries to delete the parent, but first, it has to delete all children.</li>
 *   <li>MyCoRe runs through each child and deletes it.</li>
 *   <li><b>BUT</b> after the deletion of <b>ONE</b> child, the parent object will be updated.</li>
 *   <li>This results in updating the parent 500 times, before its actually deleted.</li>
 * </ol>
 * 
 * <p>
 * What this class tries to solve:<br>
 * We mark the parent as "will be deleted". When a child is deleted, and the EventHandler tries
 * to removed the child from its parent, the parent will not be updated because its "marked as
 * deleted".
 * </p>
 * 
 * TODO: check side effects
 * 
 * @author Matthias Eichner
 */
public class MCRMarkManager {

    private static MCRMarkManager INSTANCE = null;

    public static enum Operation {
        UPDATE, DELETE
    }

    private Map<MCRObjectID, Operation> marks;

    protected MCRMarkManager() {
        this.marks = new ConcurrentHashMap<>();
    }

    /**
     * Returns the instance to the singleton {@link MCRMarkManager}.
     * 
     * @return instance of {@link MCRMarkManager}
     */
    public static MCRMarkManager instance() {
        if (INSTANCE == null) {
            // make it thread safe
            synchronized (MCRMarkManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MCRMarkManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Marks a single object with the given operation.
     * 
     * @param mcrId the mycore object identifier
     * @param operation the operation
     * @return the previous Operation associated with the mycore identifier or null
     */
    public Operation mark(MCRObjectID mcrId, Operation operation) {
        return this.marks.put(mcrId, operation);
    }

    /**
     * Removes the current mark for the given mycore identifier.
     * 
     * @param mcrId the object where the mark should be removed
     */
    public void remove(MCRObjectID mcrId) {
        this.marks.remove(mcrId);
    }

    /**
     * Checks if the object is marked.
     * 
     * @param mcrId the mcr identifier
     * @return true if its marked
     */
    public boolean isMarked(MCRObjectID mcrId) {
        return this.marks.containsKey(mcrId);
    }

    /**
     * Checks if the object is marked for deletion.
     * 
     * @param mcrId the mcr identifier
     * @return true if its marked for deletion
     */
    public boolean isMarkedForDeletion(MCRObjectID mcrId) {
        return Operation.DELETE.equals(this.marks.get(mcrId));
    }

    /**
     * Checks if the object is marked for deletion.
     * 
     * @param mcrId the mcr identifier
     * @return true if its marked for deletion
     */
    public boolean isMarkedForUpdate(MCRObjectID mcrId) {
        return Operation.UPDATE.equals(this.marks.get(mcrId));
    }

}
