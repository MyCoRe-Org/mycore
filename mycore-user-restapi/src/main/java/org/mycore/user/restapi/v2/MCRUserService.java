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
import java.util.Optional;
import java.util.function.Function;

import org.mycore.common.MCRException;
import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Service for user management operations in the REST layer.
 *
 * <p>Provides CRUD operations for users and supports different levels of detail
 * via separate view types ({@link MCRUserStandard}, {@link MCRUserDetail}, {@link MCRUserSummary}).
 */
public class MCRUserService {

    private final MCRUserDtoMapper userDtoMapper;

    /**
     * Creates a new instance with the given mapper.
     *
     * @param userDtoMapper the user DTO mapper
     */
    public MCRUserService(MCRUserDtoMapper userDtoMapper) {
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * Returns the singleton instance of {@code MCRUserService}.
     *
     * @return the user service instance
     */
    public static MCRUserService obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    /**
     * Returns a new instance of {@code MCRUserService}.
     *
     * @return the user service instance
     */
    public static MCRUserService createInstance() {
        return new MCRUserService(new MCRUserDtoMapper());
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
        validateCreateRequest(createUserRequest);
        MCRUser owner = MCRUserManager.getUser(createUserRequest.owner());
        MCRUser user = userDtoMapper.toDomain(createUserRequest, owner);
        try {
            MCRUserManager.createUser(user);
            MCRUserManager.setPassword(user, createUserRequest.password());
        } catch (MCRException e) {
            throw new MCRUserValidationException(e.getMessage(), e);
        }
        MCRUser created = getUserOrThrow(createUserRequest.id());
        return userDtoMapper.toDetail(created, getOwns(created));
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
        MCRUser user = getUserOrThrow(userId);
        return userDtoMapper.toDetail(user, getOwns(user));
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
        return listPage(filter, offset, limit, user -> userDtoMapper.toDetail(user, getOwns(user)));
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
        validateUpdateRequest(updateUserRequest);
        MCRUser user = getUserOrThrow(userId);
        MCRUser owner = Optional.ofNullable(updateUserRequest.owner()).map(this::getUserOrThrow).orElse(null);
        MCRUser updated = userDtoMapper.applyUpdate(user, updateUserRequest, owner);
        try {
            MCRUserManager.updateUser(updated);
            if (updateUserRequest.password() != null) {
                MCRUserManager.setPassword(updated, updateUserRequest.password());
            }
        } catch (MCRException e) {
            throw new MCRUserValidationException(user.getUserName(), e.getMessage(), e);
        }
        MCRUser fresh = getUserOrThrow(updated.getUserName());
        return userDtoMapper.toDetail(fresh, getOwns(fresh));
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
        List<MCRUser> users =
            MCRUserManager.listUsers(filter.idPattern, filter.realm, filter.namePattern, filter.mailPattern, null, null,
                offset, limit);
        List<T> page = users.stream().map(mapper).toList();
        long size = MCRUserManager.countUsers(filter.idPattern, filter.realm, filter.namePattern, filter.mailPattern);
        return new MCRUserPage<>(page, size);
    }

    private MCRUser getUserOrThrow(String userId) {
        return Optional.ofNullable(MCRUserManager.getUser(userId))
            .orElseThrow(() -> new MCRUserNotFoundException(userId));
    }

    private static List<MCRUser> getOwns(MCRUser user) {
        return MCRUserManager.listUsers(user);
    }

    private static void validateCreateRequest(MCRCreateUserRequest request) {
        if (request.id() == null || request.id().isBlank()) {
            throw new MCRUserValidationException("id is required");
        }
        if (MCRUserManager.exists(request.id())) {
            throw new MCRUserAlreadyExistsException(request.id());
        }
        validatePassword(request.password());
        validateOwner(request.owner());
        validateRoles(request.roles());
        validateEmail(request.email());
    }

    private static void validateUpdateRequest(MCRUpdateUserRequest request) {
        if (request.password() != null) {
            validatePassword(request.password());
        }
        validateOwner(request.owner());
        validateRoles(request.roles());
        validateEmail(request.email());
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new MCRUserValidationException("password is required");
        }
    }

    private static void validateOwner(String id) {
        if (id != null && !MCRUserManager.exists(id)) {
            throw new MCRUserValidationException("user " + id + " (owner) does not exist");
        }
    }

    private static void validateRoles(List<String> roles) {
        if (roles == null) {
            return;
        }
        for (String role : roles) {
            if (MCRRoleManager.getRole(role) == null) {
                throw new MCRUserValidationException("role " + role + " does not exist");
            }
        }
    }

    private static void validateEmail(String email) {
        if (email != null && !email.contains("@")) {
            throw new MCRUserValidationException("email is invalid");
        }
    }

    private static final class LazyInstanceHolder {
        private static final MCRUserService SHARED_INSTANCE = createInstance();
    }

    /**
     * Filter criteria for user search queries.
     * All fields support wildcards: * for any sequence of characters, ? for a single character.
     * Any field may be null to indicate no filtering on that field.
     *
     * @param idPattern  a wildcard pattern for the login userid
     * @param realm the realm the user belongs to
     * @param namePattern  a wildcard pattern for the person's real name
     * @param mailPattern  a wildcard pattern for the person's email address
     */
    public record MCRUserFilter(
        String idPattern,
        String realm,
        String namePattern,
        String mailPattern
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
