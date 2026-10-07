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

package org.mycore.user.restapi.v2.resource;

import static org.mycore.restapi.v2.MCRRestStatusCode.BAD_REQUEST;
import static org.mycore.restapi.v2.MCRRestStatusCode.FORBIDDEN;
import static org.mycore.restapi.v2.MCRRestStatusCode.NO_CONTENT;
import static org.mycore.restapi.v2.MCRRestStatusCode.OK;

import org.mycore.common.MCRSessionMgr;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.v2.MCRDetailLevelResolver;
import org.mycore.restapi.v2.MCRRestSchemaType;
import org.mycore.user.restapi.exception.MCRUserNoLocalPasswordException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.exception.MCRUserWrongPasswordException;
import org.mycore.user.restapi.v2.MCRUserService;
import org.mycore.user.restapi.v2.dto.MCRChangePasswordRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserProfileRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for operations on the currently authenticated user's own account.
 *
 * @see MCRUsers
 */
@MCRApiDraft("MCRUsers")
@Path("/user")
public class MCRCurrentUser {

    private static final String DESC_NO_LOCAL_ACCOUNT = "Caller has no corresponding local user account";

    @Context
    private ContainerRequestContext request;

    private final MCRUserService userService;

    /**
     * Creates a new instance with the default {@link MCRUserService}.
     */
    public MCRCurrentUser() {
        this(MCRUserService.obtainInstance());
    }

    /**
     * Creates a new instance with the given service.
     *
     * @param userService the service to use for user management operations
     */
    public MCRCurrentUser(MCRUserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the currently authenticated user's own data.
     *
     * @return 200 OK with the caller's own data at the requested detail level
     * @throws NotAuthorizedException if the caller is not authenticated
     * @throws ForbiddenException if the caller has no corresponding local user account
     * @throws BadRequestException if the detail level is unknown
     */
    @Operation(
        summary = "Returns the currently authenticated user's own data",
        parameters = {
            @Parameter(
                name = "Accept",
                in = ParameterIn.HEADER,
                description = MCRDetailLevelResolver.DETAIL_LEVEL_DESCRIPTION,
                example = MCRDetailLevelResolver.DETAIL_LEVEL_EXAMPLE,
                schema = @Schema(type = MCRRestSchemaType.STRING)
            )
        },
        responses = {
            @ApiResponse(
                responseCode = FORBIDDEN,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_NO_LOCAL_ACCOUNT
            ),
            @ApiResponse(
                responseCode = OK,
                description = "Caller's own data at the requested detail level",
                content = {
                    @Content(
                        mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema(
                            oneOf = {
                                MCRUserSummary.class,
                                MCRUserStandard.class,
                                MCRUserDetail.class
                            }
                        )
                    )
                }
            )
        },
        tags = MCRUsers.TAG_MCR_USER
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() {
        String userId = currentUserId();
        try {
            return switch (MCRDetailLevelResolver.resolve(request)) {
                case SUMMARY -> Response.ok(userService.getUserSummary(userId)).build();
                case DETAILED -> Response.ok(userService.getUserDetail(userId)).build();
                default -> Response.ok(userService.getUserStandard(userId)).build();
            };
        } catch (MCRUserNotFoundException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * Updates the profile data of the currently authenticated user.
     *
     * @param updateOwnUserDto the request body containing the updated profile data
     * @return 204 No Content
     * @throws NotAuthorizedException if the caller is not authenticated
     * @throws ForbiddenException if the caller has no corresponding local user account
     * @throws BadRequestException if the data is invalid
     */
    @Operation(
        summary = "Updates the profile data of the currently authenticated user",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRUpdateUserProfileRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = FORBIDDEN,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_NO_LOCAL_ACCOUNT
            ),
            @ApiResponse(
                responseCode = BAD_REQUEST,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = MCRUsers.DESC_INVALID_BODY_CONTENT
            ),
            @ApiResponse(responseCode = NO_CONTENT, description = "Profile successfully updated"),
        },
        tags = MCRUsers.TAG_MCR_USER
    )
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    public Response updateCurrentUser(MCRUpdateUserProfileRequest updateOwnUserDto) {
        String userId = currentUserId();
        try {
            userService.updateUser(userId, updateOwnUserDto);
            return Response.noContent().build();
        } catch (MCRUserNotFoundException e) {
            throw new ForbiddenException(e);
        } catch (MCRUserValidationException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * Changes the password of the currently authenticated user.
     *
     * @param changePasswordDto the request body containing the current and new password
     * @return 204 No Content
     * @throws NotAuthorizedException if the caller is not authenticated
     * @throws ForbiddenException if the current password does not match, or the caller has
     *         no corresponding local user account
     * @throws BadRequestException if the new password is invalid
     */
    @Operation(
        summary = "Changes the password of the currently authenticated user",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRChangePasswordRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = FORBIDDEN,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = "Current password is incorrect"
            ),
            @ApiResponse(
                responseCode = BAD_REQUEST,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = MCRUsers.DESC_INVALID_BODY_CONTENT
            ),
            @ApiResponse(responseCode = NO_CONTENT, description = "Password successfully changed"),
        },
        tags = MCRUsers.TAG_MCR_USER
    )
    @PUT
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    public Response changePassword(MCRChangePasswordRequest changePasswordDto) {
        String userId = currentUserId();
        try {
            userService.changePassword(userId, changePasswordDto);
            return Response.noContent().build();
        } catch (MCRUserWrongPasswordException | MCRUserNotFoundException | MCRUserNoLocalPasswordException e) {
            throw new ForbiddenException(e);
        } catch (MCRUserValidationException e) {
            throw new BadRequestException(e);
        }
    }

    private String currentUserId() {
        return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
    }

}
