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
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTransformer;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.restapi.v2.MCRErrorResponse;

/**
 * Helper class for {@link org.mycore.datamodel.metadata.MCRObject}
 * and {@link org.mycore.datamodel.metadata.MCRDerivate} endpoint.
 */
public class MCRRestAccessKeyHelper {

    /**
     * Placeholder for the path param secret.
     */
    protected static final String PARAM_SECRET = "secret";

    /**
     * Placeholder for the query param secret_format.
     */
    protected static final String QUERY_PARAM_SECRET_ENCODING = "secret_encoding";

    private static WebApplicationException getUnknownObjectException(MCRObjectID objectId) {
        return MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withMessage(objectId + " does not exist!")
            .withErrorCode("objectNotFound")
            .toException();
    }

    /**
     * Adds {@link MCRAccessKey} for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID of MCRObject
     * @param accessKeyJson the MCRAccessKey as json
     * @param uriInfo the UriInfo
     * @return the Response
     */
    protected static Response doCreateAccessKey(MCRObjectID objectId, String accessKeyJson, UriInfo uriInfo) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        final String encodedSecret = URLEncoder.encode(accessKey.getSecret(), UTF_8);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(encodedSecret).build()).build();
    }

    /**
     * Returns {@link MCRAccessKey} with secret for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret
     * @param secretEncoding the enconding of the secret
     * @return the Response
     */
    protected static Response doGetAccessKey(MCRObjectID objectId, String secret, String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        MCRAccessKey accessKey = null;
        if (secretEncoding != null) {
            accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId, decode(secret, secretEncoding));
        } else {
            accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId, secret);
        }
        if (accessKey != null) {
            return Response.ok(accessKey).build();
        }
        throw new MCRAccessKeyNotFoundException("Key does not exist.");
    }

    /**
     * Returns all {@link MCRAccessKey}s for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @param offset the offset
     * @param limit the limit
     * @return the Response
     */
    protected static Response doListAccessKeys(MCRObjectID objectId, int offset, int limit) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.listAccessKeys(objectId);
        final List<MCRAccessKey> accessKeysResult = accessKeys.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
        return Response.ok(accessKeysResult)
            .header("X-Total-Count", accessKeys.size())
            .build();
    }

    /**
     * Removes {@link MCRAccessKey} with secret for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret
     * @param secretEncoding the enconding of the secret
     * @return the Response
     */
    protected static Response doRemoveAccessKey(MCRObjectID objectId, String secret, String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        if (secretEncoding != null) {
            MCRAccessKeyManager.removeAccessKey(objectId, decode(secret, secretEncoding));
        } else {
            MCRAccessKeyManager.removeAccessKey(objectId, secret);
        }
        return Response.noContent().build();
    }

    /**
     * Updates {@link MCRAccessKey} with secret for {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret
     * @param accessKeyJson the MCRAccessKey as json
     * @param secretEncoding the enconding of the secret
     * @return the Response
     */
    protected static Response doUpdateAccessKey(MCRObjectID objectId, String secret, String accessKeyJson,
        String secretEncoding) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        if (secretEncoding != null) {
            MCRAccessKeyManager.updateAccessKey(objectId, decode(secret, secretEncoding), accessKey);
        } else {
            MCRAccessKeyManager.updateAccessKey(objectId, secret, accessKey);
        }
        return Response.noContent().build();
    }

    private static String decode(String text, String encoding) {
        if (Objects.equals(encoding, "base64url")) {
            return new String(Base64.getUrlDecoder().decode(text.getBytes(UTF_8)), UTF_8);
        }
        return text;
    }
}
