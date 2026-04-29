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
import static org.mycore.restapi.v2.MCRRestStatusCode.CONFLICT;
import static org.mycore.restapi.v2.MCRRestStatusCode.CREATED;
import static org.mycore.restapi.v2.MCRRestStatusCode.NOT_FOUND;
import static org.mycore.restapi.v2.MCRRestStatusCode.NO_CONTENT;
import static org.mycore.restapi.v2.MCRRestStatusCode.OK;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.MCRRestConstants;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.v2.MCRRestSchemaType;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;
import org.mycore.user.restapi.exception.MCRUserAlreadyExistsException;
import org.mycore.user.restapi.exception.MCRUserNotFoundException;
import org.mycore.user.restapi.exception.MCRUserValidationException;
import org.mycore.user.restapi.v2.MCRUserService;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUserDetail;
import org.mycore.user.restapi.v2.dto.MCRUserStandard;
import org.mycore.user.restapi.v2.dto.MCRUserSummary;

import com.github.fge.jsonpatch.JsonPatch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST resource for user management.
 *
 * <p>Provides CRUD endpoints under {@code /users}. All endpoints require the
 * {@code manage-user} permission.
 *
 * <p>The level of detail in GET responses can be controlled via the
 * {@code Accept} header parameter defined in {@link MCRDetailLevel}:
 * <ul>
 *   <li>{@link MCRDetailLevel#SUMMARY} -> minimal user info</li>
 *   <li>{@link MCRDetailLevel#DETAILED} -> full user info</li>
 *   <li>{@link MCRDetailLevel#NORMAL} -> standard user info (default)</li>
 * </ul>
 *
 * @see MCRUserService
 */
@MCRApiDraft("MCRUsers")
@Path("/users")
public class MCRUsers {

    private static final String PERMISSION_MANAGE_USER = "manage-user";
    private static final String PARAM_USER_ID = "user_id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_REALM = "realm";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_EMAIL = "email";
    private static final String DEFAULT_OFFSET_STR = "0";
    private static final String DEFAULT_LIMIT_STR = "100";
    private static final String TAG_MCR_USER = "mcr_user";
    private static final String DESC_USER_NOT_FOUND = "User not found";
    private static final String DESC_INVALID_BODY_CONTENT = "Invalid body";
    private static final String DETAIL_LEVEL_DESCRIPTION =
        "Controls the level of detail in the response via the detail parameter. "
            + "Supported values: SUMMARY, NORMAL, DETAILED. "
            + "Example: application/json; detail=SUMMARY";

    @Context
    private UriInfo uriInfo;

    @Context
    ContainerRequestContext request;

    private final MCRUserService userService;

    /**
     * Creates a new instance with the default {@link MCRUserService}.
     */
    public MCRUsers() {
        this(MCRUserService.obtainInstance());
    }

    /**
     * Creates a new instance with the given service.
     *
     * @param userService the service to use for user management operations
     */
    public MCRUsers(MCRUserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param createUserDto the request body containing the user data
     * @return 201 Created with the URI of the new user in the {@code Location} header
     * @throws BadRequestException if the user data is invalid
     * @throws ClientErrorException with status 409 if the user already exists
     */
    @Operation(
        summary = "Creates a new user",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRCreateUserRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = CREATED,
                description = "User successfully created",
                headers = @Header(
                    name = HttpHeaders.LOCATION,
                    schema = @Schema(type = "string", format = "uri"),
                    description = "URL of the new user"
                )
            ),
            @ApiResponse(responseCode = BAD_REQUEST, description = DESC_INVALID_BODY_CONTENT),
            @ApiResponse(responseCode = CONFLICT, description = "User already exists")
        },
        tags = TAG_MCR_USER
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response createUser(MCRCreateUserRequest createUserDto) {
        try {
            String userId = userService.createUser(createUserDto).id();
            return Response.created(uriInfo.getAbsolutePathBuilder().path(userId).build()).build();
        } catch (MCRUserValidationException e) {
            throw new BadRequestException(e);
        } catch (MCRUserAlreadyExistsException e) {
            throw new ClientErrorException(Response.Status.CONFLICT, e);
        }
    }

    /**
     * Returns a single user by ID.
     *
     * @param userId the ID of the user
     * @return 200 OK with the user data at the requested detail level
     * @throws NotFoundException if no user with the given ID exists
     * @throws BadRequestException if the detail level is unknown
     */
    @Operation(
        summary = "Returns a single user by ID",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        parameters = {
            @Parameter(
                name = "Accept",
                in = ParameterIn.HEADER,
                description = DETAIL_LEVEL_DESCRIPTION,
                example = "application/json; detail=SUMMARY",
                schema = @Schema(type = MCRRestSchemaType.STRING)
            )
        },
        responses = {
            @ApiResponse(
                responseCode = NOT_FOUND,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_USER_NOT_FOUND
            ),
            @ApiResponse(
                responseCode = OK,
                description = "User found at the requested detail level",
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
        tags = TAG_MCR_USER
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response getUser(@PathParam(PARAM_USER_ID) String userId) {
        try {
            return switch (getDetailLevel()) {
                case SUMMARY -> Response.ok(userService.getUserSummary(userId)).build();
                case DETAILED -> Response.ok(userService.getUserDetail(userId)).build();
                default -> Response.ok(userService.getUserStandard(userId)).build();
            };
        } catch (MCRUserNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    /**
     * Returns a paginated list of users matching the given filter criteria.
     * Wildcards * and ? may be used for pattern parameters.
     *
     * @param idPattern a wildcard pattern for the login userid, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param offset the index of the first result to return, defaults to 0
     * @param limit the maximum number of results to return, defaults to 100
     * @return a paginated list of matching users
     */
    @Operation(
        summary = "Lists all users",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        parameters = {
            @Parameter(
                name = "Accept",
                in = ParameterIn.HEADER,
                description = DETAIL_LEVEL_DESCRIPTION,
                example = "application/json; detail=SUMMARY",
                schema = @Schema(type = MCRRestSchemaType.STRING)
            )
        },
        responses = @ApiResponse(
            responseCode = OK,
            description = "List of all users at the requested detail level",
            content = {
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(
                        type = "array",
                        oneOf = {
                            MCRUserSummary.class,
                            MCRUserStandard.class,
                            MCRUserDetail.class
                        }
                    )
                )
            },
            headers = {
                @Header(
                    name = MCRRestConstants.HEADER_X_TOTAL_COUNT,
                    schema = @Schema(type = MCRRestSchemaType.INTEGER)
                )
            }),
        tags = TAG_MCR_USER
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @MCRCacheControl(
        maxAge = @MCRCacheControl.Age(time = 0, unit = TimeUnit.SECONDS),
        noCache = @MCRCacheControl.FieldArgument(active = true)
    )
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    @MCRAccessControlExposeHeaders({ MCRRestConstants.HEADER_X_TOTAL_COUNT })
    public Response listUsers(
        @QueryParam(PARAM_ID)
        @Parameter(description = "Wildcard pattern for the login user name. Supports * and ?", example = "admin*")
        String idPattern,

        @QueryParam(PARAM_NAME)
        @Parameter(description = "Wildcard pattern for the user's real name. Supports * and ?", example = "John*")
        String namePattern,

        @QueryParam(PARAM_EMAIL)
        @Parameter(
            description = "Wildcard pattern for the user's email address. Supports * and ?",
            example = "*@example.com"
        )
        String mailPattern,

        @QueryParam(PARAM_REALM)
        @Parameter(description = "Realm the user belongs to", example = "local")
        String realm,

        @QueryParam(MCRRestConstants.PARAM_OFFSET)
        @DefaultValue(DEFAULT_OFFSET_STR)
        @Parameter(
            description = "Index of the first result to return",
            schema = @Schema(type = MCRRestSchemaType.INTEGER, format = "int32", defaultValue = DEFAULT_OFFSET_STR)
        )
        int offset,

        @QueryParam(MCRRestConstants.PARAM_LIMIT)
        @DefaultValue(DEFAULT_LIMIT_STR)
        @Parameter(
            description = "Maximum number of results to return",
            schema = @Schema(type = MCRRestSchemaType.INTEGER, format = "int32", defaultValue = DEFAULT_LIMIT_STR)
        )
        int limit
    ) {
        MCRUserService.MCRUserFilter filter
            = new MCRUserService.MCRUserFilter(idPattern, realm, namePattern, mailPattern);

        return switch (getDetailLevel()) {
            case SUMMARY -> pageResponse(userService.listSummary(filter, offset, limit));
            case DETAILED -> pageResponse(userService.listDetail(filter, offset, limit));
            default -> pageResponse(userService.listStandard(filter, offset, limit));
        };
    }

    /**
     * Updates an existing user.
     *
     * @param userId the ID of the user to update
     * @param updateUserDto the request body containing the updated user data
     * @return 204 No Content
     * @throws NotFoundException if no user with the given ID exists
     * @throws BadRequestException if the user data is invalid
     */
    @Operation(
        summary = "Updates an existing user by ID",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRUpdateUserRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = NOT_FOUND,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_USER_NOT_FOUND
            ),
            @ApiResponse(
                responseCode = BAD_REQUEST,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_INVALID_BODY_CONTENT
            ),
            @ApiResponse(responseCode = NO_CONTENT, description = "User successfully updated"),
        },
        tags = TAG_MCR_USER
    )
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response updateUser(@PathParam(PARAM_USER_ID) String userId, MCRUpdateUserRequest updateUserDto) {
        try {
            userService.updateUser(userId, updateUserDto);
            return Response.noContent().build();
        } catch (MCRUserNotFoundException e) {
            throw new NotFoundException(e);
        } catch (MCRUserValidationException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * Patches an existing user.
     *
     * @param userId the ID of the user to update
     * @param patch the json patch
     * @return 204 No Content
     * @throws NotFoundException if no user with the given ID exists
     * @throws BadRequestException if the patch is invalid or contains forbidden fields
     */
    @Operation(
        summary = "Patches an existing user by ID",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_PATCH_JSON,
                schema = @Schema(
                    type = "array",
                    example = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"John\"}]"
                )
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = NOT_FOUND,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_USER_NOT_FOUND
            ),
            @ApiResponse(
                responseCode = BAD_REQUEST,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_INVALID_BODY_CONTENT
            ),
            @ApiResponse(responseCode = NO_CONTENT, description = "User successfully patched"),
        },
        tags = TAG_MCR_USER
    )
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response patchUser(@PathParam(PARAM_USER_ID) String userId, JsonPatch patch) {
        try {
            userService.patchUser(userId, patch);
            return Response.noContent().build();
        } catch (MCRUserNotFoundException e) {
            throw new NotFoundException(e);
        } catch (MCRUserValidationException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the ID of the user to delete
     * @return 204 No Content
     * @throws NotFoundException if no user with the given ID exists
     */
    @Operation(
        summary = "Deletes a user by ID",
        security = @SecurityRequirement(name = PERMISSION_MANAGE_USER),
        responses = {
            @ApiResponse(
                responseCode = NOT_FOUND,
                content = @Content(mediaType = MediaType.TEXT_PLAIN),
                description = DESC_USER_NOT_FOUND
            ),
            @ApiResponse(responseCode = NO_CONTENT, description = "User successfully deleted"),
        },
        tags = TAG_MCR_USER
    )
    @DELETE
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response deleteUser(@PathParam(PARAM_USER_ID) String userId) {
        try {
            userService.deleteUser(userId);
            return Response.noContent().build();
        } catch (MCRUserNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private <T> Response pageResponse(MCRUserService.MCRUserPage<T> page) {
        return Response.ok(page.users())
            .header(MCRRestConstants.HEADER_X_TOTAL_COUNT, page.total())
            .build();
    }

    // TODO move to MCRRestUtils?
    // TODO case-sensitive?
    private MCRDetailLevel getDetailLevel() {
        Optional<String> detailLevelOptional = request.getAcceptableMediaTypes().stream()
            .flatMap(m -> m.getParameters().entrySet().stream()
                .filter(e -> MCRDetailLevel.MEDIA_TYPE_PARAMETER.equals(e.getKey()))).map(Map.Entry::getValue)
            .findFirst();
        if (detailLevelOptional.isEmpty()) {
            return MCRDetailLevel.NORMAL;
        }
        try {
            return MCRDetailLevel.valueOf(detailLevelOptional.get());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown detail level: " + detailLevelOptional.get(), e);
        }
    }

}
