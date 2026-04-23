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
import java.util.concurrent.TimeUnit;

import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.MCRRestConstants;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.v2.MCRRestSchemaType;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;
import org.mycore.user.restapi.v2.MCRUserService;
import org.mycore.user.restapi.v2.dto.MCRCreateUserRequest;
import org.mycore.user.restapi.v2.dto.MCRUpdateUserRequest;

import com.github.fge.jsonpatch.JsonPatch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
@MCRApiDraft("users")
@Path("/users")
public class MCRUsers {

    private static final String PARAM_USER_ID = "user_id";
    private static final String PERMISSION_MANAGE_USER = "manage-user";
    private static final String DEFAULT_OFFSET_STR = "0";
    private static final String DEFAULT_LIMIT_STR = "100";
    private static final String TAG_MCR_USER = "mcr_user";
    private static final String DESC_USER_NOT_FOUND = "User not found";
    private static final String DESC_INVALID_BODY_CONTENT = "Invalid body";

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
     */
    @Operation(
        summary = "Creates a new user.",
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
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response createUser(MCRCreateUserRequest createUserDto) {
        String userId = userService.createUser(createUserDto).userId();
        return Response.created(uriInfo.getAbsolutePathBuilder().path(userId).build()).build();
    }

    /**
     * Returns a single user by ID.
     *
     * @param userId the ID of the user
     * @return 200 OK with the user data at the requested detail level
     */
    @Operation(
        summary = "Returns a single user by ID.",
        parameters = {
            @Parameter(
                name = "X-Detail-Level",
                in = ParameterIn.HEADER,
                description = "Level of detail for the response. Defaults to NORMAL",
                schema = @Schema(implementation = MCRDetailLevel.class)
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
                content = @Content(mediaType = MediaType.APPLICATION_JSON),
                description = "User found at the requested detail level"
            )
        },
        tags = TAG_MCR_USER
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRCacheControl(
        maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS)
    )
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response getUser(@PathParam(PARAM_USER_ID) String userId) {
        return switch (getDetailLevel()) {
            case SUMMARY -> Response.ok(userService.getUserSummary(userId)).build();
            case DETAILED -> Response.ok(userService.getUserDetail(userId)).build();
            default -> Response.ok(userService.getUserStandard(userId)).build();
        };
    }

    /**
     * Returns a paginated list of users matching the given filter criteria.
     * Wildcards * and ? may be used for pattern parameters.
     *
     * @param userPattern a wildcard pattern for the login userid, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param offset the index of the first result to return, defaults to 0
     * @param limit the maximum number of results to return, defaults to 100
     * @return a paginated list of matching users
     */
    @Operation(
        summary = "Lists all users.",
        parameters = {
            @Parameter(
                name = "X-Detail-Level",
                in = ParameterIn.HEADER,
                description = "Level of detail for the response. Defaults to NORMAL",
                schema = @Schema(implementation = MCRDetailLevel.class)
            ),
            @Parameter(
                name = "user",
                description = "Wildcard pattern for the login user name. Supports * and ?",
                example = "admin*"
            ),
            @Parameter(name = "realm", description = "Realm the user belongs to", example = "local"),
            @Parameter(
                name = "name",
                description = "Wildcard pattern for the person's real name. Supports * and ?",
                example = "John*"
            ),
            @Parameter(name = "mail",
                description = "Wildcard pattern for the person's email address. Supports * and ?",
                example = "*@example.com"
            ),
            @Parameter(
                name = "offset",
                description = "Index of the first result to return",
                schema = @Schema(type = MCRRestSchemaType.INTEGER, defaultValue = DEFAULT_OFFSET_STR)
            ),
            @Parameter(
                name = "limit",
                description = "Maximum number of results to return",
                schema = @Schema(type = MCRRestSchemaType.INTEGER, defaultValue = DEFAULT_LIMIT_STR)
            )
        },
        responses = @ApiResponse(
            responseCode = OK,
            description = "List of all users at the requested detail level",
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
        maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS)
    )
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    @MCRAccessControlExposeHeaders({ MCRRestConstants.HEADER_X_TOTAL_COUNT })
    public Response listUsers(
        @QueryParam("user") String userPattern,
        @QueryParam("realm") String realm,
        @QueryParam("name") String namePattern,
        @QueryParam("mail") String mailPattern,
        @QueryParam("offset") @DefaultValue(DEFAULT_OFFSET_STR) int offset,
        @QueryParam("limit") @DefaultValue(DEFAULT_LIMIT_STR) int limit) {
        MCRUserService.MCRUserFilter filter
            = new MCRUserService.MCRUserFilter(userPattern, realm, namePattern, mailPattern);

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
     */
    @Operation(
        summary = "Updates an existing user by ID.",
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
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response updateUser(@PathParam(PARAM_USER_ID) String userId, MCRUpdateUserRequest updateUserDto) {
        userService.updateUser(userId, updateUserDto);
        return Response.noContent().build();
    }

    /**
     * Patches an existing user.
     *
     * @param userId the ID of the user to update
     * @param patch the json patch
     * @return 204 No Content
     */
    @Operation(
        summary = "Patches an existing user by ID.",
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
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON))
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path("/{" + PARAM_USER_ID + "}")
    @MCRRequireTransaction
    @MCRRestRequiredPermission(PERMISSION_MANAGE_USER)
    public Response patchUser(@PathParam(PARAM_USER_ID) String userId, JsonPatch patch) {
        userService.patchUser(userId, patch);
        return Response.noContent().build();
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the ID of the user to delete
     * @return 204 No Content
     */
    @Operation(
        summary = "Deletes a user by ID.",
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
        userService.deleteUser(userId);
        return Response.noContent().build();
    }

    private <T> Response pageResponse(MCRUserService.MCRUserPage<T> page) {
        return Response.ok(page.users())
            .header(MCRRestConstants.HEADER_X_TOTAL_COUNT, page.total())
            .build();
    }

    private MCRDetailLevel getDetailLevel() {
        return request.getAcceptableMediaTypes().stream()
            .flatMap(m -> m.getParameters().entrySet().stream()
                .filter(e -> MCRDetailLevel.MEDIA_TYPE_PARAMETER.equals(e.getKey()))).map(Map.Entry::getValue)
            .findFirst()
            .map(MCRDetailLevel::valueOf).orElse(MCRDetailLevel.NORMAL);
    }

}
