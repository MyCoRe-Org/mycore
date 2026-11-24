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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

/**
 * A pair of objects that are possible duplicates of each other because they share the given
 * {@link MCRDeDupCriterion criteria}. The two object ids are kept in a normalized order
 * ({@code objectId1 <= objectId2}): if they are given the other way round, they are swapped. So the pair
 * {@code (a, b)} and {@code (b, a)} are represented identically and compare equal.
 *
 * @param objectId1 the lexicographically smaller object id of the pair
 * @param objectId2 the lexicographically larger object id of the pair
 * @param criteria  all criteria the two objects have in common, never empty and without {@code null}
 */
public record MCRPossibleDuplicate(String objectId1, String objectId2, SequencedSet<MCRDeDupCriterion> criteria) {

    public MCRPossibleDuplicate {
        Objects.requireNonNull(objectId1, "objectId1 must not be null");
        Objects.requireNonNull(objectId2, "objectId2 must not be null");
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (objectId1.compareTo(objectId2) > 0) {
            String swapped = objectId1;
            objectId1 = objectId2;
            objectId2 = swapped;
        }
        SequencedSet<MCRDeDupCriterion> copy = new LinkedHashSet<>(criteria);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("criteria must not be empty");
        }
        copy.forEach(criterion -> Objects.requireNonNull(criterion, "criteria must not contain null"));
        criteria = Collections.unmodifiableSequencedSet(copy);
    }

    /**
     * Returns the id of the object of this pair that is not the given one.
     *
     * @param objectId one of the two object ids of this pair
     * @return the other object id
     * @throws IllegalArgumentException if the given object id is not part of this pair
     */
    public String otherObjectId(String objectId) {
        if (objectId1.equals(objectId)) {
            return objectId2;
        }
        if (objectId2.equals(objectId)) {
            return objectId1;
        }
        throw new IllegalArgumentException(
            "Object " + objectId + " is not part of the pair " + objectId1 + " and " + objectId2);
    }
}
