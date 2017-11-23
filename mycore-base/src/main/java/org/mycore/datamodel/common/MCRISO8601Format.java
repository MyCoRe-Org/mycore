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

package org.mycore.datamodel.common;

import java.util.stream.Stream;

public enum MCRISO8601Format {
    YEAR("UUUU"),
    YEAR_MONTH("UUUU-MM"),
    COMPLETE("UUUU-MM-DD"),
    COMPLETE_HH_MM(
        "UUUU-MM-DDThh:mmTZD"),
    COMPLETE_HH_MM_SS("UUUU-MM-DDThh:mm:ssTZD"),
    COMPLETE_HH_MM_SS_SSS(
        "UUUU-MM-DDThh:mm:ss.sTZD"),
    YEAR_ERA("YYYY"),
    YEAR_MONTH_ERA("YYYY-MM"),
    COMPLETE_ERA(
        "YYYY-MM-DD"),
    COMPLETE_HH_MM_ERA(
        "YYYY-MM-DDThh:mmTZD"),
    COMPLETE_HH_MM_SS_ERA("YYYY-MM-DDThh:mm:ssTZD"),
    COMPLETE_HH_MM_SS_SSS_ERA(
        "YYYY-MM-DDThh:mm:ss.sTZD");

    private String format;

    MCRISO8601Format(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }

    public static MCRISO8601Format getFormat(String format) {
        return Stream.of(values())
            .filter(f -> f.format.equals(format))
            .findAny()
            .orElse(null);
    }

}
