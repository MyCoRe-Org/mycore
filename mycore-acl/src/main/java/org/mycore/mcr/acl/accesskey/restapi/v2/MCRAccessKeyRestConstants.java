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

package org.mycore.mcr.acl.accesskey.restapi.v2;

/**
 * This class provides constants for rest.
 */
public final class MCRAccessKeyRestConstants {

    /**
     * Required permission to manage access key.
     */
    public static final String PERMISSION_MANAGE_ACCESS_KEY = "manage-access-key";

    /**
     * Access key id path parameter.
     */
    public static final String PATH_PARAM_ACCESS_KEY_ID = "accessKeyId";

    /**
     * Type query parameter.
     */
    public static final String QUERY_PARAM_PERMISSIONS = "permissions";

    /**
     * Reference query parameter.
     */
    public static final String QUERY_PARAM_REFERENCE = "reference";

    /**
     * Offset query parameter.
     */
    public static final String QUERY_PARAM_OFFSET = "offset";

    /**
     * Limit query parameter.
     */
    public static final String QUERY_PARAM_LIMIT = "limit";

    /**
     * Header name for total count info.
     */
    public static final String HEADER_TOTAL_COUNT = "X-Total-Count";

    private MCRAccessKeyRestConstants() {

    }
}
