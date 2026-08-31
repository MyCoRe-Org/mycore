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

package org.mycore.dedup.resources;

import java.util.Map;

import org.mycore.dedup.MCRPossibleDuplicate;

/**
 * JSON representation of a possible duplicate pair enriched with the display titles of both objects, so
 * that clients can render the pair without loading the object metadata per row.
 *
 * @param objectId1      the lexicographically smaller object id of the pair
 * @param title1         the display title of {@code objectId1}, or {@code null} if unknown
 * @param objectId2      the lexicographically larger object id of the pair
 * @param title2         the display title of {@code objectId2}, or {@code null} if unknown
 * @param criterionType  the type of the criterion the two objects have in common
 * @param criterionValue the value of the criterion the two objects have in common
 */
public record MCRDuplicateDto(String objectId1, String title1, String objectId2, String title2,
    String criterionType, String criterionValue) {

    public static MCRDuplicateDto of(MCRPossibleDuplicate duplicate, Map<String, String> titles) {
        return new MCRDuplicateDto(
            duplicate.objectId1(), titles.get(duplicate.objectId1()),
            duplicate.objectId2(), titles.get(duplicate.objectId2()),
            duplicate.criterion().type(), duplicate.criterion().value());
    }
}
