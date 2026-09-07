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

/**
 * Request body for a user updating their own profile.
 * <p>
 * A user's <b>profile</b> is the subset of their data that describes them personally -
 * real name, email address, and password hint - as opposed to the administrative fields
 * (roles, lock state, validity period, ownership) that only an administrator may change via
 * {@link MCRUpdateUserRequest}. This is the only data a non-administrator may ever change about
 * themselves.
 * <p>
 * All fields are required and will completely replace the corresponding existing values.
 *
 * @param name the real name of the user
 * @param email the email address of the user
 * @param passwordHint a hint for the user in case the password is forgotten, or {@code null}
 * @see MCRUpdateUserRequest
 */
public record MCRUpdateUserProfileRequest(
    String name,
    String email,
    String passwordHint
) {
}
