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

package org.mycore.orcid2.util;

import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDUserProperties;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for converting {@link MCRORCIDCredential} and {@link MCRORCIDUserProperties}
 * to and from JSON using Jackson's {@link ObjectMapper}.
 * <p>
 * This class provides methods to serialize and deserialize objects related to ORCID credentials
 * and user properties.
 * </p>
 */
public class MCRORCIDJSONMapper {

    private static final ObjectMapper OBJECT_MAPPER = initMapper();

    private static ObjectMapper initMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

    /**
     * Converts {@link MCRORCIDCredential} to a JSON string.
     *
     * @param credential the {@link MCRORCIDCredential} to be converted
     * @return the JSON string representation of the credential
     * @throws IllegalArgumentException if the conversion to JSON fails
     */
    public static String credentialToJSON(MCRORCIDCredential credential) {
        try {
            return OBJECT_MAPPER.writeValueAsString(credential);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Converts a JSON string to an {@link MCRORCIDCredential} object.
     *
     * @param credentialString the JSON string representing the credential
     * @return the {@link MCRORCIDCredential} object
     * @throws IllegalArgumentException if the conversion from JSON fails
     */
    public static MCRORCIDCredential jsonToCredential(String credentialString) {
        try {
            return OBJECT_MAPPER.readValue(credentialString, MCRORCIDCredential.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Converts an {@link MCRORCIDUserProperties} object to its JSON string representation.
     *
     * @param userProperties the {@link MCRORCIDUserProperties} to be converted
     * @return the JSON string representation of the user properties
     * @throws IllegalArgumentException if the conversion to JSON fails
     */
    public static String userPropertiesToString(MCRORCIDUserProperties userProperties) {
        try {
            return OBJECT_MAPPER.writeValueAsString(userProperties);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Converts a JSON string to an {@link MCRORCIDUserProperties} object.
     *
     * @param userPropertiesString the JSON string representing the user properties
     * @return the {@link MCRORCIDUserProperties} object
     * @throws IllegalArgumentException if the conversion from JSON fails
     */
    public static MCRORCIDUserProperties jsonToUserProperties(String userPropertiesString) {
        try {
            return OBJECT_MAPPER.readValue(userPropertiesString, MCRORCIDUserProperties.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
