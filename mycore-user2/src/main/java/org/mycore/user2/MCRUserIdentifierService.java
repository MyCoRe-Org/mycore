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

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;

/**
 * This class identifies {@link MCRUser users} by their user ID and looks up their identifiers through the
 * {@link MCRUserAttribute attributes} attached to the user entity. New attributes are also persisted in the user
 * entity.
 */
public class MCRUserIdentifierService implements MCRLegalEntityService {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATTR_ID_PREFIX = "id_";

    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @return all identifiers with the prefix {@link MCRUserIdentifierService#ATTR_ID_PREFIX}
     * or an empty set if user has none. Prefix is stripped
     *
     * @throws MCRException if the referenced user isn't found
     */
    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        MCRUser user = findUserByUserID(userId);

        return user.getAttributes().stream()
            .filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
            .map(a -> new MCRIdentifier(stripPrefix(a.getName()), a.getValue()))
            .collect(Collectors.toSet());
    }

    /**
     * Adds an attribute to a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @param attributeToAdd the attribute to add in the form of a {@link MCRIdentifier}
     *
     * @throws MCRException if an identifier cannot be added to the user
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        MCRUser user = findUserByUserID(userId);

        MCRUserAttribute newAttribute = new MCRUserAttribute(
            ATTR_ID_PREFIX + attributeToAdd.getType(), attributeToAdd.getValue());

        if (user.getAttributes().add(newAttribute)) {
            try {
                MCRUserManager.updateUser(user);
            } catch (Exception e) {
                throw new MCRException("Failed to update user for identifier: " + userId, e);
            }
        } else {
            LOGGER.warn("The attribute {} already exists in user {} and will not be added again"
                , attributeToAdd, userId);
        }
    }

    /**
     * Takes a user id and returns an Optional with the corresponding user.
     * @param userId the user id
     * @return a {@link MCRUser user}
     *
     * @throws MCRException if the user cannot be found via the user id or another error occurs
     */
    private MCRUser findUserByUserID(MCRIdentifier userId) {
        if (userId == null || !MCRIdentifier.USER_ID_TYPE.equals(userId.getType())) {
            throw new MCRException("Invalid user id: " + userId);
        }
        MCRUser user = MCRUserManager.getUser(userId.getValue());
        if (user == null) {
            throw new MCRException("User not found for identifier: " + userId);
        }
        return user;
    }

    private String stripPrefix(String name) {
        return name.startsWith(ATTR_ID_PREFIX) ? name.substring(ATTR_ID_PREFIX.length()) : name;
    }

}
