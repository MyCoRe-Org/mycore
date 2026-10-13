/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.dedup;

import java.util.Objects;

/**
 * A pair of objects that are possible duplicates of each other because they share the given
 * {@link MCRDeDupCriterion}. The two object ids are kept in a normalized order
 * ({@code objectId1 <= objectId2}) so that the pair {@code (a, b)} and {@code (b, a)} are represented
 * identically and compare equal.
 *
 * @param objectId1 the lexicographically smaller object id of the pair
 * @param objectId2 the lexicographically larger object id of the pair
 * @param criterion the criterion the two objects have in common
 */
public record MCRPossibleDuplicate(String objectId1, String objectId2, MCRDeDupCriterion criterion) {

    public MCRPossibleDuplicate {
        Objects.requireNonNull(objectId1, "objectId1 must not be null");
        Objects.requireNonNull(objectId2, "objectId2 must not be null");
        Objects.requireNonNull(criterion, "criterion must not be null");
    }

    /**
     * Creates a possible duplicate for the two given object ids and a shared criterion, normalizing the
     * order of the object ids.
     *
     * @param objectIdA one object id
     * @param objectIdB the other object id
     * @param criterion the shared criterion
     */
    public static MCRPossibleDuplicate of(String objectIdA, String objectIdB, MCRDeDupCriterion criterion) {
        if (objectIdA.compareTo(objectIdB) <= 0) {
            return new MCRPossibleDuplicate(objectIdA, objectIdB, criterion);
        }
        return new MCRPossibleDuplicate(objectIdB, objectIdA, criterion);
    }
}
