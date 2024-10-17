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

import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
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
    public static List<MCRAccessKeyDto> findAccessKeys(String reference, String permission, int offset, int limit,
        HttpServletResponse response) {
        if (reference.isEmpty() || permission.isEmpty()) {
            throw new BadRequestException();
        }
        List<MCRAccessKeyDto> accessKeyDtos = MCRAccessKeyServiceFactory.getAccessKeyService()
            .findAccessKeysByReferenceAndPermission(reference, permission);
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
    public static MCRAccessKeyDto findAccessKey(UUID id) {
        return MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKey(id);
    }

    /**
     * Adds access key.
     *
     * @param accessKeyDto the access key DTO
     * @param uriInfo the URI info
     * @return the response
     */
    public static Response addAccessKey(MCRAccessKeyDto accessKeyDto, UriInfo uriInfo) {
        final MCRAccessKeyDto createdAccessKey = MCRAccessKeyServiceFactory.getAccessKeyService()
            .addAccessKey(accessKeyDto);
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
    public static Response updateAccessKey(UUID id, MCRAccessKeyDto accessKeyDto) {
        MCRAccessKeyServiceFactory.getAccessKeyService().updateAccessKey(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Partial updates access key by id.
     *
     * @param id the access key id
     * @param accessKeyDto the update DTO
     * @return the response
     */
    public static Response partialUpdateAccessKey(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto) {
        MCRAccessKeyServiceFactory.getAccessKeyService().partialUpdateAccessKey(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Remove access key by id.
     *
     * @param id the access key id
     * @return the response
     */
    public static Response removeAccessKey(UUID id) {
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAccessKey(id);
        return Response.noContent().build();
    }
}
