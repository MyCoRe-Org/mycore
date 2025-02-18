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

/**
 * A utility class that defines constant keys for constructing Solr query parameters.
 * <p>
 * These constants represent common parameter names used in Solr requests, such as query,
 * filter query, pagination, sorting, and specifying the fields to return.
 */
public final class MCRSolrParameter {

    public static final String QUERY = "q";

    public static final String FILTER_QUERY = "fq";

    public static final String START = "start";

    public static final String ROWS = "rows";

    public static final String SORT = "sort";

    public static final String FIELD_LIST = "fl";

    public static final String REQUEST_HANDLER = "qt";

}
