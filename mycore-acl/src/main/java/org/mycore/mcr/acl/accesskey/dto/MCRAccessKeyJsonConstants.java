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

package org.mycore.mcr.acl.accesskey.dto;

/**
 * This class provides constants for access key JSON objects.
 */
public final class MCRAccessKeyJsonConstants {

    /**
     * Property name for id.
     */
    public static final String NAME_ID = "id";

    /**
     * Property name for permission.
     */
    public static final String NAME_PERMISSION = "type";

    /**
     * Property name for secret.
     */
    public static final String NAME_SECRET = "secret";

    /**
     * Property name for reference.
     */
    public static final String NAME_REFERENCE = "reference";

    /**
     * Property name for comment.
     */
    public static final String NAME_COMMENT = "comment";

    /**
     * Property name for active.
     */
    public static final String NAME_ACTIVE = "isActive";

    /**
     * Property name for expiration.
     */
    public static final String NAME_EXPIRATION = "expiration";

    /**
     * Property name for created.
     */
    public static final String NAME_CREATED = "created";

    /**
     * Property name for created by.
     */
    public static final String NAME_CREATED_BY = "createdBy";

    /**
     * Property name for last modified.
     */
    public static final String NAME_LAST_MODIFIED = "lastModified";

    /**
     * Property name for last modified by.
     */
    public static final String NAME_LAST_MODIFIED_BY = "lastModifiedBy";

    private MCRAccessKeyJsonConstants() {

    }

}
