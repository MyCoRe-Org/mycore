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

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Optional extension of {@link MCRObjectIDGenerator} for generators that maintain their own
 * "last used ID" state (e.g. a cache file) instead of deriving it from the underlying object store
 * on every call.
 * <p>
 * Such generators must be informed about IDs that have been assigned outside of
 * {@link #getNextFreeId(String, int)}, e.g. when an object is created with a fixed, externally
 * supplied ID. Otherwise their internal state would lag behind the actual store and subsequent
 * calls to {@link #getNextFreeId(String, int)} could return colliding IDs.
 */
public interface MCRTrackingObjectIDGenerator extends MCRObjectIDGenerator {

    /**
     * Records that the given ID has been used. Implementations must update their internal
     * "last used ID" state for the corresponding base ID so that subsequent calls to
     * {@link #getNextFreeId(String, int)} return values strictly greater than {@code id}.
     * <p>
     * Calls with an ID lower than or equal to the currently tracked last ID must be ignored.
     *
     * @param id the ID that has been used
     */
    void recordID(MCRObjectID id);

}
