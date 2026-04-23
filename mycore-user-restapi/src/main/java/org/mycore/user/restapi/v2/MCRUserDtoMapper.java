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
import org.mycore.user2.MCRUserManager;

/**
 * Maps between {@link MCRUser} domain objects and REST DTOs.
 */
public class MCRUserDtoMapper {

    /**
     * Creates a new {@link MCRUser} domain object from the given create request.
     *
     * @param dto the create request
     * @return a new {@link MCRUser} with the given data applied
     */
    public MCRUser toDomain(MCRCreateUserRequest dto) {
        MCRUser user = new MCRUser(dto.userId());
        applyCommon(user, dto.name(), dto.email(), dto.passwordHint(), dto.password(),
            dto.locked(), dto.validUntil(), dto.roles(), dto.attributes(), dto.owner());
        return user;
    }

    /**
     * Applies the given update request to an existing {@link MCRUser}.
     *
     * <p>All roles and attributes are replaced completely.
     *
     * @param user the user to update
     * @param dto the update request
     * @return the updated {@link MCRUser}
     */
    public MCRUser applyUpdate(MCRUser user, MCRUpdateUserRequest dto) {
        new ArrayList<>(user.getSystemRoleIDs()).forEach(user::unassignRole);
        new ArrayList<>(user.getExternalRoleIDs()).forEach(user::unassignRole);
        user.getAttributes().clear();
        applyCommon(user, dto.name(), dto.email(), dto.passwordHint(), dto.password(),
            dto.locked(), dto.validUntil(), dto.roles(), dto.attributes(), dto.owner());
        return user;
    }

    /**
     * Maps a {@link MCRUser} to a {@link MCRUserDetail}.
     *
     * @param user the user to map
     * @return a detailed view of the user
     */
    public MCRUserDetail toDetail(MCRUser user) {
        return new MCRUserDetail(
            user.getUserID(),
            user.getRealName(),
            user.getEMail(),
            user.getLastLogin() != null ? user.getLastLogin().toString() : null,
            user.isLocked(),
            user.getValidUntil() != null ? user.getValidUntil().toString() : null,
            toAttributeMap(user),
            user.getOwner() != null ? user.getOwner().getUserID() : null,
            getAllRoles(user),
            getOwns(user)
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
            user.getValidUntil() != null ? user.getValidUntil().toString() : null,
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

    private void applyCommon(MCRUser user, String name, String email, String passwordHint,
        String password, boolean locked, String validUntil,
        List<String> roles, Map<String, String> attributes, String owner) {
        user.setRealName(name);
        user.setEMail(email);
        user.setHint(passwordHint);
        user.setHash(password);
        user.setLocked(locked);
        user.setValidUntil(validUntil != null ? Date.from(Instant.parse(validUntil)) : null);
        if (roles != null) {
            roles.forEach(user::assignRole);
        }
        if (attributes != null) {
            attributes.forEach(user::setUserAttribute);
        }
        if (owner != null) {
            user.setOwner(MCRUserManager.getUser(owner));
        } else {
            user.setOwner(null);
        }
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

    private static List<String> getOwns(MCRUser user) {
        return MCRUserManager.listUsers(user).stream()
            .map(MCRUser::getUserID)
            .toList();
    }

}
