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

package org.mycore.solr.search;

public enum MCRSolrParameter {

    QUERY("q"), FILTER_QUERY("fq"),
    START("start"), ROWS("rows"),
    SORT("sort"),
    FIELD_LIST("fl"),
    REQUEST_HANDLER("qt");

    private final String value;

    MCRSolrParameter(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MCRSolrParameter fromString(String value) {
        for (MCRSolrParameter parameter : values()) {
            if (parameter.value.equals(value)) {
                return parameter;
            }
        }
        throw new IllegalArgumentException("No constant with value " + value + " found");
    }

}
