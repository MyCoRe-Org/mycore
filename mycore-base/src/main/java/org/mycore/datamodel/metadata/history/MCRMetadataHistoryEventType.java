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

package org.mycore.datamodel.metadata.history;

public enum MCRMetadataHistoryEventType {

    CREATE('c'), DELETE('d');

    private final char abbr;

    MCRMetadataHistoryEventType(char abbr) {
        this.abbr = abbr;
    }

    public static MCRMetadataHistoryEventType fromAbbr(char abbr) {
        return switch (abbr) {
            case 'c' -> CREATE;
            case 'd' -> DELETE;
            default -> throw new IllegalArgumentException(
                "No such " + MCRMetadataHistoryEventType.class.getSimpleName() + ": " + abbr);
        };
    }

    protected char getAbbr() {
        return abbr;
    }

}
