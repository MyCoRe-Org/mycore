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

package org.mycore.user.restapi.v2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

/**
 * Maps between {@link MCRUser} domain objects and REST DTOs.
 */
public class MCRUserDtoMapper {

    /**
     * Creates a new {@link MCRUser} domain object based on the provided request data.
     *
     * @param dto the request object containing the user data to be applied
     * @param owner the owner or creator of the new user
     * @return a new {@link MCRUser} populated with the values from the request
     */
    public MCRUser toDomain(MCRCreateUserRequest dto, MCRUser owner) {
        MCRUser user = new MCRUser(dto.id());
        applyCommon(user, dto.name(), dto.email(), dto.passwordHint(), dto.locked(), dto.validUntil(), dto.roles(),
            dto.attributes(), owner);
        return user;
    }

    /**
     * Applies the given update request to an existing {@link MCRUser}.
     *
     * <p>All existing roles (system and external) and attributes are removed
     * and replaced with the values provided in the update request.
     *
     * @param user the user to update
     * @param dto the update request containing the new values
     * @param owner the user performing the update or acting as owner
     * @return the updated {@link MCRUser}
     */
    public MCRUser applyUpdate(MCRUser user, MCRUpdateUserRequest dto, MCRUser owner) {
        new ArrayList<>(user.getSystemRoleIDs()).forEach(user::unassignRole);
        new ArrayList<>(user.getExternalRoleIDs()).forEach(user::unassignRole);
        user.getAttributes().clear();
        applyCommon(user, dto.name(), dto.email(), dto.passwordHint(), dto.locked(), dto.validUntil(), dto.roles(),
            dto.attributes(), owner);
        return user;
    }

    /**
     * Maps a {@link MCRUser} to a {@link MCRUserDetail}.
     *
     * @param user the user to map
     * @param owns the list of users owned or managed by the given user
     * @return a detailed representation of the user, including ownership information
     */
    public MCRUserDetail toDetail(MCRUser user, List<MCRUser> owns) {
        return new MCRUserDetail(
            user.getUserID(),
            user.getRealName(),
            user.getEMail(),
            user.getLastLogin() != null ? user.getLastLogin().toInstant() : null,
            user.isLocked(),
            user.getValidUntil() != null ? user.getValidUntil().toInstant() : null,
            toAttributeMap(user),
            user.getOwner() != null ? user.getOwner().getUserID() : null,
            getAllRoles(user),
            owns.stream().map(MCRUser::getUserID).toList()
        );
    }

    /**
     * Maps a {@link MCRUser} to a {@link MCRUserStandard}.
     *
     * @param user the user to map
     * @return a standard view of the user
     */
    public MCRUserStandard toStandard(MCRUser user) {
        return new MCRUserStandard(
            user.getUserID(),
            user.getRealName(),
            user.getEMail(),
            user.isLocked(),
            user.getValidUntil() != null ? user.getValidUntil().toInstant() : null,
            getAllRoles(user),
            user.getOwner() != null ? user.getOwner().getUserID() : null,
            toAttributeMap(user)
        );
    }

    /**
     * Maps a {@link MCRUser} to a {@link MCRUserSummary}.
     *
     * @param user the user to map
     * @return a summary view of the user
     */
    public MCRUserSummary toSummary(MCRUser user) {
        return new MCRUserSummary(user.getUserID(), user.getRealName());
    }

    private void applyCommon(MCRUser user, String name, String email, String passwordHint, boolean locked,
        Instant validUntil, List<String> roles, Map<String, String> attributes, MCRUser owner) {
        user.setRealName(name);
        user.setEMail(email);
        user.setHint(passwordHint);
        user.setLocked(locked);
        user.setValidUntil(validUntil != null ? Date.from(validUntil) : null);
        if (roles != null) {
            roles.forEach(user::assignRole);
        }
        if (attributes != null) {
            attributes.forEach(user::setUserAttribute);
        }
        user.setOwner(owner);
    }

    private static List<String> getAllRoles(MCRUser user) {
        return Stream.concat(
            user.getSystemRoleIDs().stream(),
            user.getExternalRoleIDs().stream()
        ).toList();
    }

    private Map<String, String> toAttributeMap(MCRUser user) {
        return user.getAttributes().stream()
            .collect(Collectors.toMap(
                MCRUserAttribute::getName,
                MCRUserAttribute::getValue
            ));
    }

}
