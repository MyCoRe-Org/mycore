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

import java.util.List;
import java.util.UUID;

import org.mycore.mcr.acl.accesskey.MCRAccessKeyService;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Provides CRUD methods for REST.
 */
public class MCRAccessKeyRestHelper {

    /**
     * Returns all access keys for reference and permission.
     *
     * @param reference the reference
     * @param permission the permission
     * @param offset the optional offset
     * @param limit the optional list
     * @param response the response
     * @return list of all access keys for object
     */
    public static List<MCRAccessKeyDto> getAccessKeys(String reference, String permission, int offset, int limit,
        HttpServletResponse response) {
        final MCRAccessKeyService service = MCRAccessKeyServiceFactory.getService();
        List<MCRAccessKeyDto> accessKeyDtos = null;
        if (!reference.isEmpty() && !permission.isEmpty()) {
            accessKeyDtos = service.getAccessKeysByReferenceAndPermission(reference, permission);
        } else if (!reference.isEmpty()) {
            accessKeyDtos = service.getAccessKeysByReference(reference);
        } else if (!permission.isEmpty()) {
            accessKeyDtos = service.getAccessKeysByPermission(permission);
        } else {
            accessKeyDtos = service.getAllAccessKeys();
        }
        response.setHeader(MCRAccessKeyRestConstants.HEADER_TOTAL_COUNT, Integer.toString(accessKeyDtos.size()));
        return accessKeyDtos.stream().skip(offset).limit(limit)
            .sorted((a1, a2) -> a1.getCreated().compareTo(a2.getCreated())).toList();
    }

    /**
     * Returns specific access key by id for object.
     *
     * @param id the access key id
     * @return the access key
     */
    public static MCRAccessKeyDto getAccessKeyById(UUID id) {
        return MCRAccessKeyServiceFactory.getService().getAccessKeyById(id);
    }

    /**
     * Creates access key for object id.
     *
     * @param accessKeyDto the access key DTO
     * @param uriInfo the URI info
     * @return the response
     */
    public static Response createAccessKey(MCRAccessKeyDto accessKeyDto, UriInfo uriInfo) {
        final MCRAccessKeyDto createdAccessKey = MCRAccessKeyServiceFactory.getService().createAccessKey(accessKeyDto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(createdAccessKey.getId().toString()).build())
            .build();
    }

    /**
     * Updates access key by id.
     *
     * @param id the access key id
     * @param accessKeyDto the access key DTO
     * @return the response
     */
    public static Response updateAccessKeyById(UUID id, MCRAccessKeyDto accessKeyDto) {
        MCRAccessKeyServiceFactory.getService().updateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Partial updates access key by id.
     *
     * @param id the access key id
     * @param accessKeyDto the update DTO
     * @return the response
     */
    public static Response partialUpdateAccessKeyById(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto) {
        MCRAccessKeyServiceFactory.getService().partialUpdateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Deletes access key by id.
     *
     * @param id the access key id
     * @return the response
     */
    public static Response deleteAccessKeyById(UUID id) {
        MCRAccessKeyServiceFactory.getService().deleteAccessKeyById(id);
        return Response.noContent().build();
    }
}
