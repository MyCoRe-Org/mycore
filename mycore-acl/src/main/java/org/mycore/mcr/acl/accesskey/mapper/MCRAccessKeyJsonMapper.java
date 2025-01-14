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

import java.util.Arrays;
import java.util.List;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for mapping between access key and JSON.
 * Uses Jackson library for serialization and deserialization.
 */
public final class MCRAccessKeyJsonMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MCRAccessKeyJsonMapper() {

    }

    /**
     * Converts {@link MCRAccessKeyDto} to a JSON string.
     *
     * @param accessKeyDto the access key DTO
     * @return the JSON representation of the access key DTO
     * @throws IllegalArgumentException if access key DTO cannot be mapped to JSON
     */
    public static String accessKeyDtoToJson(MCRAccessKeyDto accessKeyDto) {
        try {
            return OBJECT_MAPPER.writeValueAsString(accessKeyDto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while mapping access key DTO to JSON", e);
        }
    }

    /**
     * Converts {@link MCRAccessKeyDto}s to a JSON string.
     *
     * @param accessKeyDtos the access key DTOs
     * @return the JSON representation of the access keys DTOs
     * @throws IllegalArgumentException if access key DTOs cannot be mapped to JSON
     */
    public static String accessKeyDtosToJson(List<MCRAccessKeyDto> accessKeyDtos) {
        try {
            return OBJECT_MAPPER.writeValueAsString(accessKeyDtos);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while mapping access key DTOs to JSON", e);
        }
    }

    /**
     * Converts JSON to {@link MCRAccessKeyDto}.
     *
     * @param json the JSON representation of the access key DTO
     * @return the access key DTO
     * @throws IllegalArgumentException if JSON cannot be mapped to access key DTO
     */
    public static MCRAccessKeyDto jsonToAccessKeyDto(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, MCRAccessKeyDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while mapping JSON to access key DTO", e);
        }
    }

    /**
     * Transforms JSON to {@link MCRAccessKeyDto}s.
     *
     * @param json the JSON representation of the access key DTO elements
     * @return the access key DTOs
     * @throws IllegalArgumentException if JSON cannot be mapped to access key DTOs
     */
    public static List<MCRAccessKeyDto> jsonToAccessKeyDtos(String json) {
        try {
            return Arrays.asList(OBJECT_MAPPER.readValue(json, MCRAccessKeyDto[].class));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while mapping JSON to access key DTOs", e);
        }
    }

}
