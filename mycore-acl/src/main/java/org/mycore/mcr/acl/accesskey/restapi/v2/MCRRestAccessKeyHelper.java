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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.restapi.v2.dto.MCRAccessKeyInformation;
import org.mycore.mcr.acl.accesskey.restapi.v2.dto.MCRRestAccessKeyDto;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class MCRRestAccessKeyHelper {

    /**
     * Placeholder for the path param secret
     */
    protected static final String PARAM_SECRET = "secret";

    /**
     * Placeholder for the query param secret_format
     */
    protected static final String QUERY_PARAM_SECRET_ENCODING = "secret_encoding";

    private static WebApplicationException getUnknownObjectException(final MCRObjectID objectId) {
        return MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withMessage(objectId + " does not exist!")
            .withErrorCode("objectNotFound")
            .toException();
    }

    protected static Response doCreateAccessKey(MCRObjectID objectId, MCRRestAccessKeyDto restAccessKeyDto,
        UriInfo uriInfo) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKeyDto accessKeyDto = toAccessKeyDto(restAccessKeyDto);
        accessKeyDto.setReference(objectId.toString());
        final MCRAccessKeyDto createdAccessKey
            = MCRAccessKeyServiceFactory.getAccessKeyService().addAccessKey(accessKeyDto);
        final String encodedSecret = URLEncoder.encode(createdAccessKey.getValue(), UTF_8);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(encodedSecret).build()).build();
    }

    protected static MCRRestAccessKeyDto doGetAccessKey(MCRObjectID objectId, String secret, String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getAccessKeyService()
                .findAccessKeyByReferenceAndValue(objectId.toString(), decode(secret, secretEncoding))
            : MCRAccessKeyServiceFactory.getAccessKeyService()
                .findAccessKeyByReferenceAndValue(objectId.toString(), secret);
        return Optional.ofNullable(accessKeyDto).map(MCRRestAccessKeyHelper::toRestAccessKeyDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Key does not exist"));
    }

    protected static MCRAccessKeyInformation doListAccessKeys(MCRObjectID objectId, int offset, int limit) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final List<MCRAccessKeyDto> accessKeyDtos = MCRAccessKeyServiceFactory.getAccessKeyService()
            .findAccessKeysByReference(objectId.toString());
        final List<MCRRestAccessKeyDto> accessKeyDtosResult
            = accessKeyDtos.stream().skip(offset).limit(limit).map(MCRRestAccessKeyHelper::toRestAccessKeyDto)
                .collect(Collectors.toList());
        return new MCRAccessKeyInformation(accessKeyDtosResult, accessKeyDtos.size());
    }

    protected static Response doRemoveAccessKey(MCRObjectID objectId, String secret, String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndValue(objectId.toString(),
                decode(secret, secretEncoding))
            : MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndValue(objectId.toString(),
                secret);
        if (accessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Key does not exist");
        }
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAccessKey(accessKeyDto.getId());
        return Response.noContent().build();
    }

    protected static Response doUpdateAccessKey(MCRObjectID objectId, String secret,
        MCRRestAccessKeyDto updatedRestAccessKeyDto, String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        MCRAccessKeyPartialUpdateDto partialUpdateDto = new MCRAccessKeyPartialUpdateDto();
        // prevent reference and value update to be compatible with AccessKeyManager
        partialUpdateDto.setReference(new MCRNullable<>());
        partialUpdateDto.setValue(new MCRNullable<>());
        final String type = updatedRestAccessKeyDto.getType();
        if (type != null) {
            partialUpdateDto.setPermission(new MCRNullable<>(type));
        }
        final Boolean isActive = updatedRestAccessKeyDto.getIsActive();
        if (isActive != null) {
            partialUpdateDto.setActive(new MCRNullable<>(isActive));
        }
        final Date expiration = updatedRestAccessKeyDto.getExpiration();
        if (expiration != null) {
            partialUpdateDto.setExpiration(new MCRNullable<>(expiration));
        }
        final String comment = updatedRestAccessKeyDto.getComment();
        if (comment != null) {
            partialUpdateDto.setComment(new MCRNullable<>(comment));
        }
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndValue(objectId.toString(),
                decode(secret, secretEncoding))
            : MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndValue(objectId.toString(),
                secret);
        MCRAccessKeyServiceFactory.getAccessKeyService().partialUpdateAccessKey(accessKeyDto.getId(),
            partialUpdateDto);
        return Response.noContent().build();
    }

    private static String decode(final String text, final String encoding) {
        if (Objects.equals(encoding, "base64url")) {
            return new String(Base64.getUrlDecoder().decode(text.getBytes(UTF_8)), UTF_8);
        }
        return text;
    }

    private static MCRRestAccessKeyDto toRestAccessKeyDto(MCRAccessKeyDto accessKeyDto) {
        final MCRRestAccessKeyDto restAccessKeyDto = new MCRRestAccessKeyDto();
        restAccessKeyDto.setSecret(accessKeyDto.getValue());
        restAccessKeyDto.setType(accessKeyDto.getPermission());
        restAccessKeyDto.setComment(accessKeyDto.getComment());
        restAccessKeyDto.setCreated(accessKeyDto.getCreated());
        restAccessKeyDto.setCreatedBy(accessKeyDto.getCreatedBy());
        restAccessKeyDto.setExpiration(accessKeyDto.getExpiration());
        restAccessKeyDto.setIsActive(accessKeyDto.getActive());
        restAccessKeyDto.setLastModified(accessKeyDto.getLastModified());
        restAccessKeyDto.setLastModifiedBy(accessKeyDto.getLastModifiedBy());
        return restAccessKeyDto;
    }

    private static MCRAccessKeyDto toAccessKeyDto(MCRRestAccessKeyDto restAccessKeyDto) {
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setValue(restAccessKeyDto.getSecret());
        accessKeyDto.setPermission(restAccessKeyDto.getType());
        accessKeyDto.setComment(restAccessKeyDto.getComment());
        accessKeyDto.setCreated(restAccessKeyDto.getCreated());
        accessKeyDto.setCreatedBy(restAccessKeyDto.getCreatedBy());
        accessKeyDto.setExpiration(restAccessKeyDto.getExpiration());
        accessKeyDto.setActive(restAccessKeyDto.getIsActive());
        accessKeyDto.setLastModified(restAccessKeyDto.getLastModified());
        accessKeyDto.setLastModifiedBy(restAccessKeyDto.getLastModifiedBy());
        return accessKeyDto;
    }
}
