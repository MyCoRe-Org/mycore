/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyPermissionChecker;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.restapi.v2.MCRAccessKeyRestConstants;
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
 * The <code>MCRRestAccessKeyAccessCheckStrategy</code> class implements the
 * <code>MCRRestAccessCheckStrategy</code> interface and provides a strategy
 * for checking access to REST services based on an access key.
 */
public class MCRRestAccessKeyAccessCheckStrategy implements MCRRestAccessCheckStrategy {

    private final MCRAccessKeyPermissionChecker permissionChecker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Default constructor that initializes the {@code MCRRestAccessKeyAccessCheckStrategy}
     * with a default {@code MCRAccessKeyPermissionChecker} instance.
     */
    public MCRRestAccessKeyAccessCheckStrategy() {
        permissionChecker = new MCRAccessKeyPermissionChecker();
    }

    /**
     * Constructor that allows injecting a custom {@code MCRAccessKeyPermissionChecker}.
     *
     * @param permissionChecker the {@code MCRAccessKeyPermissionChecker} to be used for checking access permissions
     */
    public MCRRestAccessKeyAccessCheckStrategy(MCRAccessKeyPermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public void checkPermission(ResourceInfo resourceInfo, ContainerRequestContext requestContext)
        throws ForbiddenException {
        if (permissionChecker.checkPoolPrivilege()) {
            return;
        }
        final String method = requestContext.getMethod();
        try {
            switch (method) {
                case "GET" -> handleGetRequest(requestContext);
                case "POST" -> handleCreateRequest(requestContext);
                case "PUT", "PATCH" -> handleUpdateRequest(requestContext);
                case "DELETE" -> handleDeleteRequest(requestContext);
                default -> throw new InternalServerErrorException();
            }
        } catch (IOException e) {
            throw new InternalServerErrorException();
        }
    }

    private void handleGetRequest(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        final Optional<String> idString
            = Optional.ofNullable(pathParameters.getFirst(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID));
        if (idString.isPresent()) {
            final MCRAccessKeyDto accessKeyDto = getAccessKeyDtoByPath(requestContext);
            if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
                throw new ForbiddenException();
            }
        } else {
            final MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
            final List<String> permissions = queryParameters.get(MCRAccessKeyRestConstants.QUERY_PARAM_PERMISSIONS);
            if (permissions == null || permissions.size() == 0) {
                throw new ForbiddenException("permission is required");
            }
            final String reference = queryParameters.getFirst(MCRAccessKeyRestConstants.QUERY_PARAM_REFERENCE);
            if (reference == null) {
                throw new ForbiddenException("reference is required");
            }
            for (String permission : permissions) {
                if (!permissionChecker.canManageAccessKey(reference, permission)) {
                    throw new ForbiddenException();
                }
            }
        }
    }

    private void handleCreateRequest(ContainerRequestContext requestContext) throws IOException {
        if (!checkJsonContentType(requestContext)) {
            throw new NotSupportedException();
        }
        final byte[] requestBody = readRequestBody(requestContext);
        if (requestBody == null || requestBody.length == 0) {
            throw new BadRequestException();
        }
        final MCRAccessKeyDto accessKeyDto = extractAccessKeyDto(requestBody);
        if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
            throw new ForbiddenException();
        }
        resetRequestBody(requestContext, requestBody);
    }

    private void handleUpdateRequest(ContainerRequestContext requestContext) throws IOException {
        if (!checkJsonContentType(requestContext)) {
            throw new NotSupportedException();
        }
        final MCRAccessKeyDto accessKeyDto
            = Optional.ofNullable(getAccessKeyDtoByPath(requestContext)).orElseThrow(() -> new BadRequestException());
        if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
            throw new ForbiddenException();
        }
        final byte[] requestBody = readRequestBody(requestContext);
        if (requestBody == null || requestBody.length == 0) {
            throw new BadRequestException();
        }
        if (Objects.equals(HttpMethod.PUT, requestContext.getMethod())) {
            final MCRAccessKeyDto accessKeyDtoBody = extractAccessKeyDto(requestBody);
            if (!permissionChecker.canManageAccessKey(accessKeyDtoBody)) {
                throw new ForbiddenException();
            }
        } else {
            final MCRAccessKeyPartialUpdateDto accessKeyDtoBody = extractAccessKeyPartialUpdateDto(requestBody);
            accessKeyDtoBody.getPermission().getOptional().ifPresent(accessKeyDto::setPermission);
            accessKeyDtoBody.getReference().getOptional().ifPresent(accessKeyDto::setReference);
            if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
                throw new ForbiddenException();
            }
        }
        resetRequestBody(requestContext, requestBody);
    }

    private void handleDeleteRequest(ContainerRequestContext requestContext) {
        final MCRAccessKeyDto accessKeyDto = getAccessKeyDtoByPath(requestContext);
        if (!permissionChecker.canManageAccessKey(accessKeyDto)) {
            throw new ForbiddenException();
        }
    }

    private boolean checkJsonContentType(ContainerRequestContext requestContext) {
        MediaType contentType = requestContext.getMediaType();
        return contentType != null && contentType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    private MCRAccessKeyDto getAccessKeyDtoByPath(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        return Optional.ofNullable(pathParameters.getFirst(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID))
            .map(UUID::fromString).map(MCRAccessKeyServiceFactory.getAccessKeyService()::findAccessKey).get();
    }

    private byte[] readRequestBody(ContainerRequestContext requestContext) throws IOException {
        InputStream inputStream = requestContext.getEntityStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private MCRAccessKeyDto extractAccessKeyDto(byte[] requestBody) {
        try {
            return objectMapper.readValue(requestBody, MCRAccessKeyDto.class);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    private MCRAccessKeyPartialUpdateDto extractAccessKeyPartialUpdateDto(byte[] requestBody) {
        try {
            return objectMapper.readValue(requestBody, MCRAccessKeyPartialUpdateDto.class);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    private void resetRequestBody(ContainerRequestContext requestContext, byte[] requestBody) {
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody));
    }

}
