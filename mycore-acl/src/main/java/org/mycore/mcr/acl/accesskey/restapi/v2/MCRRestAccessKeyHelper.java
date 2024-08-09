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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mycore.access.MCRAccessException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.restapi.v2.model.MCRAccessKeyInformation;
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

    protected static Response doCreateAccessKey(MCRObjectID objectId, MCRAccessKeyDto accessKeyDto, UriInfo uriInfo)
        throws MCRAccessException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        accessKeyDto.setReference(objectId.toString());
        final MCRAccessKeyDto createdAccessKey
            = MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck().createAccessKey(accessKeyDto);
        final String encodedSecret = URLEncoder.encode(createdAccessKey.getValue(), UTF_8);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(encodedSecret).build()).build();
    }

    protected static MCRAccessKeyDto doGetAccessKey(MCRObjectID objectId, String value, String secretEncoding)
        throws MCRAccessException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), decode(value, secretEncoding))
            : MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), value);
        return Optional.ofNullable(accessKeyDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Key does not exist"));
    }

    protected static MCRAccessKeyInformation doListAccessKeys(MCRObjectID objectId, int offset, int limit) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck().getAccessKeysByReference(objectId.toString());
        final List<MCRAccessKeyDto> accessKeyDtosResult
            = accessKeyDtos.stream().skip(offset).limit(limit).collect(Collectors.toList());
        return new MCRAccessKeyInformation(accessKeyDtosResult, accessKeyDtos.size());
    }

    protected static Response doRemoveAccessKey(MCRObjectID objectId, String value, String secretEncoding)
        throws MCRAccessException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), decode(value, secretEncoding))
            : MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), value);
        if (accessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Key does not exist");
        }
        MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck().deleteAccessKeyById(accessKeyDto.getId());
        return Response.noContent().build();
    }

    protected static Response doUpdateAccessKey(MCRObjectID objectId, String value,
        MCRAccessKeyPartialUpdateDto updatesAccessKeyDto, String secretEncoding) throws MCRAccessException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        // prevent reference and value update to be compatible with AccessKeyManager
        updatesAccessKeyDto.setReference(new MCRNullable<>());
        updatesAccessKeyDto.setValue(new MCRNullable<>());
        final MCRAccessKeyDto accessKeyDto = (secretEncoding != null)
            ? MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), decode(value, secretEncoding))
            : MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck()
                .getAccessKeyByReferenceAndValue(objectId.toString(), value);
        MCRAccessKeyServiceFactory.getServiceWithoutAccessCheck().partialUpdateAccessKeyById(accessKeyDto.getId(),
            updatesAccessKeyDto);
        return Response.noContent().build();
    }

    private static String decode(final String text, final String encoding) {
        if (Objects.equals(encoding, "base64url")) {
            return new String(Base64.getUrlDecoder().decode(text.getBytes(UTF_8)), UTF_8);
        }
        return text;
    }
}
