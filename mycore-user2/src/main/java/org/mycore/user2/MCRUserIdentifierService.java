package org.mycore.user2;

import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MCRUserIdentifierService implements MCRLegalEntityService {

    public static final String ATTR_ID_PREFIX = "id_";

    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @return all identifiers with the prefix {@link MCRUserIdentifierService#ATTR_ID_PREFIX} or an empty list
     */
    @Override
    public List<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        return findUserByUsername(userId)
            .map(user -> user.getAttributes().stream()
                .filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
                .map(a -> new MCRIdentifier(a.getName().substring(ATTR_ID_PREFIX.length()), a.getValue()))
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    /**
     * Adds an attribute to a user by its {@link MCRUser#getUserID() user ID}.
     * @param userId the user id
     * @param attributeToAdd the attribute to add in the form of a {@link MCRIdentifier}
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        findUserByUsername(userId).ifPresent(user -> {
            MCRUserAttribute newAttribute = new MCRUserAttribute(
                ATTR_ID_PREFIX + attributeToAdd.getType(), attributeToAdd.getValue());
            if (user.getAttributes().add(newAttribute)) {
                MCRUserManager.updateUser(user);
            }
        });
    }

    /**
     * Takes a username and returns an Optional with the corresponding user.
     * @param username the username
     * @return a nullable Optional that might contain a user
     */
    private Optional<MCRUser> findUserByUsername(MCRIdentifier username) {
        if (username == null) {
            return Optional.empty();
        }
        if (!"userid".equals(username.getType())) {
            return Optional.empty();
        }
        MCRUser user = MCRUserManager.getUser(username.getValue());
        return Optional.ofNullable(user);
    }

}
