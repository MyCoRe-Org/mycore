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

package org.mycore.restapi.converter;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum MCRDetailLevel {
    SUMMARY("summary"), NORMAL("normal"), DETAILED("detailed");

    public static final String MEDIA_TYPE_PARAMETER = "detail";

    private final String value;

    private static final Map<String, MCRDetailLevel> DETAIL_LEVELS = EnumSet
        .allOf(MCRDetailLevel.class)
        .stream()
        .collect(Collectors.toMap(MCRDetailLevel::getValue, Function.identity()));

    MCRDetailLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static MCRDetailLevel fromString(String value) {
        MCRDetailLevel level = DETAIL_LEVELS.get(value);
        if (level != null) {
            return level;
        }
        throw new IllegalArgumentException("No constant with value " + value + " found");
    }
}
