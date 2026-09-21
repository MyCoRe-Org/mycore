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

import org.mycore.dedup.backend.MCRDeDupNoDuplicate;

/**
 * JSON representation of a {@link MCRDeDupNoDuplicate} marking, enriched with the display titles of both
 * objects and with the creation timestamp rendered as an ISO-8601 string.
 *
 * @param id        the database id of the marking
 * @param objectId1 the lexicographically smaller object id of the pair
 * @param title1    the display title of {@code objectId1}, or {@code null} if unknown
 * @param objectId2 the lexicographically larger object id of the pair
 * @param title2    the display title of {@code objectId2}, or {@code null} if unknown
 * @param creator   the id of the user that created the marking
 * @param created   the creation timestamp as ISO-8601 string
 */
public record MCRNoDuplicateDto(long id, String objectId1, String title1, String objectId2, String title2,
    String creator, String created) {

    public static MCRNoDuplicateDto of(MCRDeDupNoDuplicate noDuplicate, String title1, String title2) {
        return new MCRNoDuplicateDto(noDuplicate.getId(), noDuplicate.getObjectId1(), title1,
            noDuplicate.getObjectId2(), title2, noDuplicate.getCreator(),
            noDuplicate.getCreated() == null ? null : noDuplicate.getCreated().toString());
    }
}
