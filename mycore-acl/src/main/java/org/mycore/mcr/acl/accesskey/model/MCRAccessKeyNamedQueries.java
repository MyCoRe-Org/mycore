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

package org.mycore.mcr.acl.accesskey.model;

/**
 * Utility class for storing the names of the Named Queries for {@link MCRAccessKey}.
 */
public class MCRAccessKeyNamedQueries {

    /**
     * Name for named query to find all access keys.
     */
    public static final String NAME_FIND_ALL = "MCRAccessKey.findAll";

    /**
     * Name for named query to delete all access keys.
     */
    public static final String NAME_DELETE_ALL = "MCRAccessKey.deleteAll";

    /**
     * Name for named query to delete access keys by reference.
     */
    public static final String NAME_DELETE_BY_REFERENCE = "MCRAccessKey.deleteByReference";

    /**
     * Name for named query to find access keys by permission.
     */
    public static final String NAME_FIND_BY_PERMISSION = "MCRAccessKey.findByPermission";

    /**
     * Name for named query to find access keys by reference and permission.
     */
    public static final String NAME_FIND_BY_REFERENCE_AND_PERMISSION
        = "MCRAccessKey.findByReferenceAndPermission";

    /**
     * Name for named query to find access keys by reference and value.
     */
    public static final String NAME_FIND_BY_REFERENCE_AND_VALUE = "MCRAccessKey.findByReferenceAndValue";

    /**
     * Name for named query to find access key by uuid.
     */
    public static final String NAME_FIND_BY_UUID = "MCRAccessKey.findByUuid";

    /**
     * Name for named query to find access keys by reference.
     */
    public static final String NAME_FIND_BY_REFERENCE = "MCRAccessKey.findByReference";

    /**
     * Query parameter for value.
     */
    public static final String PARAM_VALUE = "value";

    /**
     * Query parameter for reference.
     */
    public static final String PARAM_REFERENCE = "reference";

    /**
     * Query parameter for permission.
     */
    public static final String PARAM_PERMISSION = "permission";

    /**
     * Query parameter for UUID.
     */
    public static final String PARAM_UUID = "uuid";

    private MCRAccessKeyNamedQueries() {

    }
}
