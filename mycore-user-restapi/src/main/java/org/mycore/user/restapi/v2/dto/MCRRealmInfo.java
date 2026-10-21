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
 * A single realm.
 *
 * @param id the unique ID of the realm, e.g. {@code local}
 * @param label the realm's label in the current session's language
 * @param loginURL where users log in via this realm
 * @param passwordChangeURL where users change their password in this realm, or {@code null}
 *        if this realm has none (typically true only for the local realm, which manages
 *        passwords through this REST API instead)
 * @param createURL where users create a new account in this realm, or {@code null} if this
 *        realm does not support self-registration
 */
public record MCRRealmInfo(
    String id,
    String label,
    String loginURL,
    String passwordChangeURL,
    String createURL
) {
}
