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

package org.mycore.user.restapi.v2.dto;

import java.util.List;
import java.util.Map;

/**
 * Request body for updating an existing user.
 *
 * <p>All fields are required and will completely replace the existing user data.
 *
 * @param name          the real name of the user
 * @param email         the email address of the user
 * @param password      the password of the user
 * @param passwordHint  a hint for the user in case the password is forgotten, or {@code null}
 * @param locked        whether the user should be locked
 * @param validUntil    the date until the user is valid as ISO-8601 string, or {@code null} if unlimited
 * @param attributes    the user attributes, or {@code null}
 * @param owner         the ID of the user that owns this user, or {@code null} if independent
 * @param roles         the roles assigned to the user, or {@code null}
 * @param owns          the IDs of users owned by this user, or {@code null}
 */
public record MCRUpdateUserRequest(
    String name,
    String email,
    String password,
    String passwordHint,
    boolean locked,
    String validUntil,
    Map<String, String> attributes,
    String owner,
    List<String> roles,
    List<String> owns
) {
}
