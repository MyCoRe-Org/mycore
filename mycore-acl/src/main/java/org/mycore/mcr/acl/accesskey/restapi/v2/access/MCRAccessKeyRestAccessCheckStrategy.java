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

package org.mycore.mcr.acl.accesskey.restapi.v2.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyPermissionChecker;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.restapi.v2.MCRAccessKeyRestConstants;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceFactory;
import org.mycore.restapi.v2.access.MCRRestAccessCheckStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * The {@code MCRRestAccessKeyAccessCheckStrategy} class implements the
 * {@link MCRRestAccessCheckStrategy} interface and provides a strategy
 * for checking access to REST services based on an access key.
 */
public class MCRAccessKeyRestAccessCheckStrategy implements MCRRestAccessCheckStrategy {

    private final MCRAccessKeyPermissionChecker permissionChecker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Default constructor that initializes the {@code MCRRestAccessKeyAccessCheckStrategy}
     * with a default {@link MCRAccessKeyPermissionChecker} instance.
     */
    public MCRAccessKeyRestAccessCheckStrategy() {
        permissionChecker = new MCRAccessKeyPermissionChecker();
    }

    /**
     * Constructor that allows injecting a custom {@code MCRAccessKeyPermissionChecker}.
     *
     * @param permissionChecker the {@link MCRAccessKeyPermissionChecker} to be used for checking access permissions
     */
    public MCRAccessKeyRestAccessCheckStrategy(MCRAccessKeyPermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public void checkPermission(ResourceInfo resourceInfo, ContainerRequestContext requestContext)
        throws ForbiddenException {
        if (permissionChecker.checkPoolPrivilege()) {
            return;
        }
        switch (requestContext.getMethod()) {
            case "GET" -> handleGetRequest(requestContext);
            case "POST" -> handleCreateRequest(requestContext);
            case "PUT", "PATCH" -> handleUpdateRequest(requestContext);
            case "DELETE" -> handleDeleteRequest(requestContext);
            default -> throw new InternalServerErrorException();
        }
    }

    private void handleGetRequest(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        final Optional<String> idString
            = Optional.ofNullable(pathParameters.getFirst(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID));
        if (idString.isPresent()) {
            final MCRAccessKeyDto accessKeyDto = getAccessKeyDtoByPath(requestContext);
            ensureHasManagePermission(accessKeyDto);
        } else {
            final MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
            final String permissionsParam = queryParameters.getFirst(MCRAccessKeyRestConstants.QUERY_PARAM_PERMISSIONS);
            if (permissionsParam == null || permissionsParam.isBlank()) {
                throw new ForbiddenException();
            }
            final List<String> permissions = Arrays.stream(permissionsParam.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
            if (permissions.isEmpty()) {
                throw new ForbiddenException();
            }
            final String reference
                = Optional.ofNullable(queryParameters.getFirst(MCRAccessKeyRestConstants.QUERY_PARAM_REFERENCE))
                    .orElseThrow(() -> new ForbiddenException());
            for (String permission : permissions) {
                ensureHasManagePermission(reference, permission);
            }
        }
    }

    private void handleCreateRequest(ContainerRequestContext requestContext) {
        ensureJsonContentType(requestContext);
        final byte[] requestBody = getRequestBody(requestContext);
        final MCRAccessKeyDto accessKeyDto = extractAccessKeyDto(requestBody);
        ensureHasManagePermission(accessKeyDto);
        resetRequestBody(requestContext, requestBody);
    }

    private void handleUpdateRequest(ContainerRequestContext requestContext) {
        ensureJsonContentType(requestContext);
        final MCRAccessKeyDto accessKeyDto
            = Optional.ofNullable(getAccessKeyDtoByPath(requestContext)).orElseThrow(BadRequestException::new);
        ensureHasManagePermission(accessKeyDto);
        final byte[] requestBody = getRequestBody(requestContext);
        if (checkPutRequest(requestContext)) {
            handlePutRequest(requestBody);
        } else {
            handlePartialUpdateRequest(accessKeyDto, requestBody);
        }
        resetRequestBody(requestContext, requestBody);
    }

    private void handlePutRequest(byte[] requestBody) {
        final MCRAccessKeyDto accessKeyDto = extractAccessKeyDto(requestBody);
        ensureHasManagePermission(accessKeyDto);
    }

    private void handlePartialUpdateRequest(MCRAccessKeyDto accessKeyDto, byte[] requestBody) {
        final MCRAccessKeyPartialUpdateDto accessKeyDtoBody = extractAccessKeyPartialUpdateDto(requestBody);
        accessKeyDtoBody.getPermission().getOptional().ifPresent(accessKeyDto::setPermission);
        accessKeyDtoBody.getReference().getOptional().ifPresent(accessKeyDto::setReference);
        ensureHasManagePermission(accessKeyDto);
    }

    private void ensureHasManagePermission(String reference, String permission) {
        if (!permissionChecker.canManageAccessKey(reference, permission)) {
            throw new ForbiddenException();
        }
    }

    private void ensureHasManagePermission(MCRAccessKeyDto accessKeyDto) {
        if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
            throw new ForbiddenException();
        }
    }

    private boolean checkPutRequest(ContainerRequestContext requestContext) {
        return Objects.equals(HttpMethod.PUT, requestContext.getMethod());
    }

    private byte[] getRequestBody(ContainerRequestContext requestContext) {
        try {
            return Optional.ofNullable(readRequestBody(requestContext)).filter(body -> body.length > 0)
                .orElseThrow(BadRequestException::new);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private void ensureJsonContentType(ContainerRequestContext requestContext) {
        if (!checkJsonContentType(requestContext)) {
            throw new NotSupportedException();
        }
    }

    private void handleDeleteRequest(ContainerRequestContext requestContext) {
        final MCRAccessKeyDto accessKeyDto = getAccessKeyDtoByPath(requestContext);
        if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
            throw new ForbiddenException();
        }
    }

    private boolean checkJsonContentType(ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getMediaType())
            .map(type -> type.isCompatible(MediaType.APPLICATION_JSON_TYPE)).orElse(false);
    }

    private MCRAccessKeyDto getAccessKeyDtoByPath(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        return Optional.ofNullable(pathParameters.getFirst(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID))
            .map(UUID::fromString).map(MCRAccessKeyServiceFactory.getAccessKeyService()::findAccessKey).get();
    }

    private byte[] readRequestBody(ContainerRequestContext requestContext) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4096);
        requestContext.getEntityStream().transferTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private MCRAccessKeyDto extractAccessKeyDto(byte[] requestBody) {
        try {
            return objectMapper.readValue(requestBody, MCRAccessKeyDto.class);
        } catch (IOException e) {
            throw new BadRequestException("Invalid access key", e);
        }
    }

    private MCRAccessKeyPartialUpdateDto extractAccessKeyPartialUpdateDto(byte[] requestBody) {
        try {
            return objectMapper.readValue(requestBody, MCRAccessKeyPartialUpdateDto.class);
        } catch (IOException e) {
            throw new BadRequestException("Invalid access key", e);
        }
    }

    private void resetRequestBody(ContainerRequestContext requestContext, byte[] requestBody) {
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody));
    }

}
