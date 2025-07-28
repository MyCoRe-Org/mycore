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
package org.mycore.mods;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents all supported relatedItem type supported for metadata sharing and linking.
 *
 * @author Thomas Scheffler
 * @see MCRMODSMetadataShareAgent
 * @see MCRMODSLinksEventHandler
 * @since 2015.03
 */
public enum MCRMODSRelationshipType {
    HOST("host"), PRECEDING("preceding"), ORIGINAL("original"), SERIES("series"), OTHER_VERSION("otherVersion"),
    OTHER_FORMAT("otherFormat"), REFERENCES("references"), REVIEW_OF("reviewOf"), PERSONAL("personal");

    private final String value;

    private static final Map<String, MCRMODSRelationshipType> RELATIONSHIP_TYPES = EnumSet
        .allOf(MCRMODSRelationshipType.class)
        .stream()
        .collect(Collectors.toMap(MCRMODSRelationshipType::getValue, Function.identity()));

    MCRMODSRelationshipType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static MCRMODSRelationshipType fromValue(String value) {
        MCRMODSRelationshipType type = RELATIONSHIP_TYPES.get(value);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("Unknown relatedItem type: " + value);
    }
}
