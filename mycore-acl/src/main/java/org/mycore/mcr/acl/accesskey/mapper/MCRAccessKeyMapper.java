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

package org.mycore.mcr.acl.accesskey.mapper;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Utility class for mapping between {@link MCRAccessKeyDto} and {@link MCRAccessKey} entities.
 *
 * The {@code AccessKeyMapper} provides static methods to convert between the DTO
 * representation and the entity representation of access keys.
 */
public final class MCRAccessKeyMapper {

    private MCRAccessKeyMapper() {

    }

    /**
     * Converts an {@link MCRAccessKeyDto} to an {@link MCRAccessKey} entity.
     *
     * @param accessKeyDto the DTO to be converted
     * @return the converted access key entity
     */
    public static MCRAccessKey toEntity(MCRAccessKeyDto accessKeyDto) {
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setSecret(accessKeyDto.getSecret());
        accessKey.setType(accessKeyDto.getPermission());
        accessKey.setUuid(accessKeyDto.getId());
        accessKey.setComment(accessKeyDto.getComment());
        accessKey.setCreated(accessKeyDto.getCreated());
        accessKey.setCreatedBy(accessKeyDto.getCreatedBy());
        accessKey.setExpiration(accessKeyDto.getExpiration());
        accessKey.setIsActive(accessKeyDto.getActive());
        accessKey.setLastModified(accessKeyDto.getLastModified());
        accessKey.setLastModifiedBy(accessKeyDto.getLastModifiedBy());
        accessKey.setReference(accessKeyDto.getReference());
        return accessKey;
    }

    /**
     * Converts an {@link MCRAccessKey} entity to an {@link MCRAccessKeyDto}.
     *
     * @param accessKey the entity to be converted
     * @return the converted access key DTO
     */
    public static MCRAccessKeyDto toDto(MCRAccessKey accessKey) {
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setId(accessKey.getUuid());
        accessKeyDto.setSecret(accessKey.getSecret());
        accessKeyDto.setPermission(accessKey.getType());
        accessKeyDto.setComment(accessKey.getComment());
        accessKeyDto.setCreated(accessKey.getCreated());
        accessKeyDto.setCreatedBy(accessKey.getCreatedBy());
        accessKeyDto.setExpiration(accessKey.getExpiration());
        accessKeyDto.setActive(accessKey.getIsActive());
        accessKeyDto.setLastModified(accessKey.getLastModified());
        accessKeyDto.setLastModifiedBy(accessKey.getLastModifiedBy());
        accessKeyDto.setReference(accessKey.getReference());
        return accessKeyDto;
    }

}
