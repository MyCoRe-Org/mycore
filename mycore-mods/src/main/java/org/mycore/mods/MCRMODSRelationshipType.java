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
package org.mycore.mods;

/**
 * Represents all supported relatedItem type supported for metadata sharing and linking.
 * 
 * @author Thomas Scheffler
 * @see MCRMODSMetadataShareAgent
 * @see MCRMODSLinksEventHandler
 * @since 2015.03
 */
public enum MCRMODSRelationshipType {
    host, preceeding, original, series, references, reviewOf;
    static String xPathList() {
        StringBuilder sb = new StringBuilder();
        for (MCRMODSRelationshipType type : values()) {
            sb.append(type.name()).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
