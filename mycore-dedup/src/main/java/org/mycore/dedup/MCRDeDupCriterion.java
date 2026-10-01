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
 * Represents a single criterion used to decide whether two objects may be duplicates of each other.
 * <p>
 * A criterion consists of a {@code type} that classifies the criterion (e.g. {@code identifier},
 * {@code shelfmark} or a combined title/author criterion) and a normalized {@code value}.
 * Two objects are considered possible duplicates if they share an equal criterion, i.e. a criterion
 * with the same {@code type} and {@code value}.
 *
 * @param type  classifies the criterion, e.g. {@code identifier}, {@code shelfmark}
 * @param value the normalized value of the criterion, e.g. a normalized identifier or title
 */
public record MCRDeDupCriterion(String type, String value) {

    public MCRDeDupCriterion {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Creates a new deduplication criterion for the given type and value.
     *
     * @param type  classifies the criterion, e.g. {@code identifier}, {@code shelfmark}
     * @param value the normalized value of the criterion
     */
    public static MCRDeDupCriterion of(String type, String value) {
        return new MCRDeDupCriterion(type, value);
    }
}
