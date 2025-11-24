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

package org.mycore.user2;

import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MCRUserIdentifierService implements MCRLegalEntityService {

    public static final String ATTR_ID_PREFIX = "id_";

    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @return all identifiers with the prefix {@link MCRUserIdentifierService#ATTR_ID_PREFIX}
     * or an empty set. prefix is stripped
     */
    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        return findUserByUserID(userId)
            .map(user -> user.getAttributes().stream()
                .filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
                .map(a -> new MCRIdentifier(stripPrefix(a.getName()), a.getValue()))
                .collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
    }

    /**
     * Gets a user's {@link MCRIdentifier MCRIdentifiers} of a specified type by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @param identifierType the type of identifier to filter for, without prefix
     * @return all identifiers of the specified type containing the prefix {@link MCRUserIdentifierService#ATTR_ID_PREFIX}
     * or an empty set. prefix is stripped
     */
    @Override
    public Set<MCRIdentifier> getTypedIdentifiers(MCRIdentifier userId, String identifierType) {
        return findUserByUserID(userId)
            .map(user -> user.getAttributes().stream()
                .filter(a -> stripPrefix(a.getName()).equals(identifierType))
                .map(a -> new MCRIdentifier(stripPrefix(a.getName()), a.getValue()))
                .collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
    }

    /**
     * Adds an attribute to a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @param attributeToAdd the attribute to add in the form of a {@link MCRIdentifier}
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        findUserByUserID(userId).ifPresent(user -> {
            MCRUserAttribute newAttribute = new MCRUserAttribute(
                ATTR_ID_PREFIX + attributeToAdd.getType(), attributeToAdd.getValue());
            if (user.getAttributes().add(newAttribute)) {
                MCRUserManager.updateUser(user);
            }
        });
    }

    /**
     * Takes a user id and returns an Optional with the corresponding user.
     * @param userId the user id
     * @return a nullable Optional that might contain a user
     */
    private Optional<MCRUser> findUserByUserID(MCRIdentifier userId) {
        if (userId == null) {
            return Optional.empty();
        }
        if (!"userid".equals(userId.getType())) {
            return Optional.empty();
        }
        MCRUser user = MCRUserManager.getUser(userId.getValue());
        return Optional.ofNullable(user);
    }

    private String stripPrefix(String name) {
        return name.startsWith(ATTR_ID_PREFIX) ? name.substring(ATTR_ID_PREFIX.length()) : name;
    }

}
