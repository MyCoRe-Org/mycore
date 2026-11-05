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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

/**
 * A pair of objects that are possible duplicates of each other because they share the given
 * {@link MCRDeDupCriterion criteria}. The two object ids are kept in a normalized order
 * ({@code objectId1 <= objectId2}) so that the pair {@code (a, b)} and {@code (b, a)} are represented
 * identically and compare equal.
 *
 * @param objectId1 the lexicographically smaller object id of the pair
 * @param objectId2 the lexicographically larger object id of the pair
 * @param criteria  all criteria the two objects have in common, never empty
 */
public record MCRPossibleDuplicate(String objectId1, String objectId2, SequencedSet<MCRDeDupCriterion> criteria) {

    public MCRPossibleDuplicate {
        Objects.requireNonNull(objectId1, "objectId1 must not be null");
        Objects.requireNonNull(objectId2, "objectId2 must not be null");
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("criteria must not be empty");
        }
        criteria = Collections.unmodifiableSequencedSet(new LinkedHashSet<>(criteria));
    }

    /**
     * Creates a possible duplicate for the two given object ids and their shared criteria, normalizing
     * the order of the object ids. The iteration order of the criteria is kept.
     *
     * @param objectIdA one object id
     * @param objectIdB the other object id
     * @param criteria  the shared criteria, must not be empty
     */
    public static MCRPossibleDuplicate of(String objectIdA, String objectIdB,
        Collection<MCRDeDupCriterion> criteria) {
        SequencedSet<MCRDeDupCriterion> copy = new LinkedHashSet<>(criteria);
        if (objectIdA.compareTo(objectIdB) <= 0) {
            return new MCRPossibleDuplicate(objectIdA, objectIdB, copy);
        }
        return new MCRPossibleDuplicate(objectIdB, objectIdA, copy);
    }

    /**
     * Returns the id of the object of this pair that is not the given one.
     *
     * @param objectId one of the two object ids of this pair
     * @return the other object id
     */
    public String otherObjectId(String objectId) {
        return objectId.equals(objectId1) ? objectId2 : objectId1;
    }
}
