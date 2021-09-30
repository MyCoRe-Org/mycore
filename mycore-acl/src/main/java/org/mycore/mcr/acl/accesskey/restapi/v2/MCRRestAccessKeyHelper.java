/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTransformer;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.restapi.v2.model.MCRAccessKeyInformation;
import org.mycore.restapi.v2.MCRErrorResponse;

public class MCRRestAccessKeyHelper {

    /**
     * Placeholder for the path param secret
     */
    protected static final String PARAM_SECRET = "secret";

    private static WebApplicationException getUnknownObjectException(final MCRObjectID objectId) {
        return MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withMessage(objectId + " does not exist!")
            .withErrorCode("objectNotFound")
            .toException();
    }

    protected static Response doCreateAccessKey(final MCRObjectID objectId, final String accessKeyJson,
        final UriInfo uriInfo) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(encode(accessKey.getSecret())).build()).build();
    }

    protected static Response doGetAccessKey(final MCRObjectID objectId, final String encodedSecret) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId, decode(encodedSecret));
        if (accessKey != null) {
            return Response.ok(accessKey).build();
        }
        throw new MCRAccessKeyNotFoundException("Key does not exist.");
    }

    protected static Response doListAccessKeys(final MCRObjectID objectId, final int offset, final int limit) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.listAccessKeys(objectId);
        final List<MCRAccessKey> accessKeysResult = accessKeys.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
        return Response.ok(new MCRAccessKeyInformation(accessKeysResult, accessKeys.size())).build();
    }

    protected static Response doRemoveAccessKey(final MCRObjectID objectId, final String encodedSecret) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        MCRAccessKeyManager.removeAccessKey(objectId, decode(encodedSecret));
        return Response.noContent().build();
    }

    protected static Response doUpdateAccessKey(final MCRObjectID objectId, final String encodedSecret,
        final String accessKeyJson) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException(objectId);
        }
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        MCRAccessKeyManager.updateAccessKey(objectId, decode(encodedSecret), accessKey);
        return Response.noContent().build();
    }

    private static String encode(final String text) {
        return Base64.getUrlEncoder().encodeToString(text.getBytes(UTF_8));
    }

    private static String decode(final String text) {
        return new String(Base64.getUrlDecoder().decode(text.getBytes(UTF_8)), UTF_8);
    }
}
