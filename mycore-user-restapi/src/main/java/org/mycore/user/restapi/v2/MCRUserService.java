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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

/**
 * Service for user management operations in the REST layer.
 *
 * <p>Provides CRUD operations for users and supports different levels of detail
 * via separate view types ({@link MCRUserStandard}, {@link MCRUserDetail}, {@link MCRUserSummary}).
 */
@MCRConfigurationProxy(proxyClass = MCRUserService.Factory.class)
public class MCRUserService {

    private static final String CONF_PROPERTY = "MCR.User.RestAPI.UserService.Class";

    private final MCRUserDtoMapper userDtoMapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new instance with the given mapper.
     *
     * @param userDtoMapper the user DTO mapper
     * @param objectMapper the object mapper
     */
    public MCRUserService(MCRUserDtoMapper userDtoMapper, ObjectMapper objectMapper) {
        this.userDtoMapper = userDtoMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the singleton instance of {@code MCRUserService}.
     *
     * @return the user service instance
     */
    @MCRFactory
    public static MCRUserService obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRUserService.class, CONF_PROPERTY);
    }

    /**
     * Returns a new instance of {@code MCRUserService}.
     *
     * @return the user service instance
     */
    public static MCRUserService createInstance() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRUserService.class, CONF_PROPERTY);
    }

    /**
     * Creates a new user.
     *
     * @param createUserRequest the request containing the user data
     * @return the created user as a detailed view
     * @throws MCRUserAlreadyExistsException if a user with the given ID already exists
     * @throws MCRUserValidationException if the user data is invalid
     */
    public MCRUserDetail createUser(MCRCreateUserRequest createUserRequest) {
        String userId = createUserRequest.userId();
        if (MCRUserManager.exists(userId)) {
            throw new MCRUserAlreadyExistsException(userId);
        }
        try {
            MCRUserManager.createUser(userDtoMapper.toDomain(createUserRequest));
        } catch (MCRException e) {
            throw new MCRUserValidationException(e.getMessage(), e);
        }
        return userDtoMapper.toDetail(Objects.requireNonNull(MCRUserManager.getUser(userId)));
    }

    /**
     * Returns a summary view of the user with the given ID.
     *
     * @param userId the ID of the user
     * @return a summary view of the user
     * @throws MCRUserNotFoundException if no user with the given ID exists
     */
    public MCRUserSummary getUserSummary(String userId) {
        return userDtoMapper.toSummary(getUserOrThrow(userId));
    }

    /**
     * Returns a detailed view of the user with the given ID.
     *
     * @param userId the ID of the user
     * @return a detailed view of the user
     * @throws MCRUserNotFoundException if no user with the given ID exists
     */
    public MCRUserDetail getUserDetail(String userId) {
        return userDtoMapper.toDetail(getUserOrThrow(userId));
    }

    /**
     * Returns a standard view of the user with the given ID.
     *
     * @param userId the ID of the user
     * @return a standard view of the user
     * @throws MCRUserNotFoundException if no user with the given ID exists
     */
    public MCRUserStandard getUserStandard(String userId) {
        return userDtoMapper.toStandard(getUserOrThrow(userId));
    }

    /**
     * Returns a paginated standard view of users matching the given filter.
     *
     * @param filter the filter criteria, may contain null values
     * @param offset the index of the first result to return
     * @param limit the maximum number of results to return
     * @return a paginated list of matching users as standard views
     */
    public MCRUserPage<MCRUserStandard> listStandard(MCRUserFilter filter, int offset, int limit) {
        return listPage(filter, offset, limit, userDtoMapper::toStandard);
    }

    /**
     * Returns a paginated detailed view of users matching the given filter.
     *
     * @param filter the filter criteria, may contain null values
     * @param offset the index of the first result to return
     * @param limit the maximum number of results to return
     * @return a paginated list of matching users as detailed views
     */
    public MCRUserPage<MCRUserDetail> listDetail(MCRUserFilter filter, int offset, int limit) {
        return listPage(filter, offset, limit, userDtoMapper::toDetail);
    }

    /**
     * Returns a paginated summary view of users matching the given filter.
     *
     * @param filter the filter criteria, may contain null values
     * @param offset the index of the first result to return
     * @param limit the maximum number of results to return
     * @return a paginated list of matching users as summary views
     */
    public MCRUserPage<MCRUserSummary> listSummary(MCRUserFilter filter, int offset, int limit) {
        return listPage(filter, offset, limit, userDtoMapper::toSummary);
    }

    /**
     * Updates the user with the given ID.
     *
     * @param userId the ID of the user to update
     * @param updateUserRequest the request containing the updated user data
     * @return the updated user as a detailed view
     * @throws MCRUserNotFoundException if no user with the given ID exists
     * @throws MCRUserValidationException if the updated user data is invalid
     */
    public MCRUserDetail updateUser(String userId, MCRUpdateUserRequest updateUserRequest) {
        MCRUser updated = userDtoMapper.applyUpdate(getUserOrThrow(userId), updateUserRequest);
        try {
            MCRUserManager.updateUser(updated);
        } catch (MCRException e) {
            throw new MCRUserValidationException(userId, e.getMessage(), e);
        }
        return userDtoMapper.toDetail(Objects.requireNonNull(MCRUserManager.getUser(userId)));
    }

    /**
     * Patches the user with the given ID.
     *
     * @param userId the ID of the user to update
     * @param patch the json patch
     * @return the updated user as a detailed view
     * @throws MCRUserException if patch fails
     * @throws MCRUserNotFoundException if no user with the given ID exists
     * @throws MCRUserValidationException if the updated user data is invalid
     */
    public MCRUserDetail patchUser(String userId, JsonPatch patch) {
        JsonNode userNode = objectMapper.valueToTree(getUserDetail(userId));
        try {
            JsonNode patched = patch.apply(userNode);
            MCRUpdateUserRequest request = objectMapper.treeToValue(patched, MCRUpdateUserRequest.class);
            return updateUser(userId, request);
        } catch (JsonProcessingException | JsonPatchException e) {
            throw new MCRUserException("Cannot patch user: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the user with the given ID.
     *
     * @param userId the ID of the user to delete
     * @throws MCRUserNotFoundException if no user with the given ID exists
     */
    public void deleteUser(String userId) {
        getUserOrThrow(userId);
        MCRUserManager.deleteUser(userId);
    }

    private <T> MCRUserPage<T> listPage(MCRUserFilter filter, int offset, int limit, Function<MCRUser, T> mapper) {
        List<MCRUser> all = MCRUserManager.listUsers(filter.user, filter.realm, filter.name, filter.mail);
        List<T> page = all.stream()
            .skip(offset)
            .limit(limit)
            .map(mapper)
            .toList();
        return new MCRUserPage<>(page, all.size());
    }

    private MCRUser getUserOrThrow(String userId) {
        return Optional.ofNullable(MCRUserManager.getUser(userId))
            .orElseThrow(() -> new MCRUserNotFoundException(userId));
    }

    /**
     * Default factory for {@link MCRUserService}.
     *
     * <p>Used by {@link MCRConfigurationProxy} to create instances via configuration mechanism.
     */
    public static class Factory implements Supplier<MCRUserService> {

        @Override
        public MCRUserService get() {
            return new MCRUserService(new MCRUserDtoMapper(), new ObjectMapper());
        }
    }

    /**
     * Filter criteria for user search queries.
     * All fields support wildcards: * for any sequence of characters, ? for a single character.
     * Any field may be null to indicate no filtering on that field.
     *
     * @param user  a wildcard pattern for the login userid
     * @param realm the realm the user belongs to
     * @param name  a wildcard pattern for the person's real name
     * @param mail  a wildcard pattern for the person's email address
     */
    public record MCRUserFilter(
        String user,
        String realm,
        String name,
        String mail
    ) {
    }

    /**
     * A paginated result containing a subset of users along with pagination metadata.
     *
     * @param users  the list of users on the current page
     * @param total  the total number of matching users across all pages
     * @param <T>    the user view type, e.g. {@link MCRUserStandard}, {@link MCRUserDetail},
     *               or {@link MCRUserSummary}
     */
    public record MCRUserPage<T>(
        List<T> users,
        long total
    ) {
    }
}
