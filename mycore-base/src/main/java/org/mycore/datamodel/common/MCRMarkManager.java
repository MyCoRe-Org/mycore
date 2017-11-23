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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.events.MCREventHandler;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Experimental class to improve performance on delete and import operations.
 * You can mark object's as "will be deleted" or "will be imported". You can
 * use this information on {@link MCREventHandler}'s to exclude those
 * marked objects from operations which makes no sense.
 *
 * <h1>
 * Current delete behavior:
 * </h1>
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
 * <h1>
 * Current import behavior:
 * </h1>
 *
 * <ol>
 *   <li>An import is started with a bunch of hierarchic objects.</li>
 *   <li>MyCoRe imports all the objects and does a "create" update.</li>
 *   <li><b>BUT</b> for each object created the parent is updated again (because a child was added)!</li>
 *   <li>This results in unnecessary updates.</li>
 * </ol>
 *
 * <p>
 * What this class tries to solve:<br>
 * We mark all objects as "will be imported". On import, we ignore all solr index call for
 * those objects. After the import, we delete all marks and do an solr import for all
 * objects at once.
 * </p>
 *
 * TODO: check side effects
 *
 * @author Matthias Eichner
 */
public class MCRMarkManager {

    private static MCRMarkManager INSTANCE = null;

    public enum Operation {
        DELETE, IMPORT
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
     * Checks if the derivate or the corresponding mycore object is
     * marked for deletion.
     *
     * @return true if one of them is marked for deletion
     */
    public boolean isMarkedForDeletion(MCRDerivate derivate) {
        return isMarkedForDeletion(derivate.getId()) || isMarkedForDeletion(derivate.getOwnerID());
    }

    /**
     * Checks if the object is marked for import.
     *
     * @param mcrId the mcr identifier
     * @return true if its marked for import
     */
    public boolean isMarkedForImport(MCRObjectID mcrId) {
        return Operation.IMPORT.equals(this.marks.get(mcrId));
    }

}
