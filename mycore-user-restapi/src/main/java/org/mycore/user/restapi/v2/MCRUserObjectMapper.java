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

package org.mycore.user.restapi.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Holds the shared {@link ObjectMapper} instance for the user REST API.
 *
 * <p>The mapper is configured with:
 * <ul>
 *   <li>Java time module for {@link java.time.Instant} and other {@code java.time} types</li>
 *   <li>ISO-8601 date format instead of Unix timestamps</li>
 *   <li>Null fields are excluded from serialization</li>
 * </ul>
 */
public final class MCRUserObjectMapper {

    /**
     * The shared {@link ObjectMapper} instance.
     */
    public static final ObjectMapper INSTANCE = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .defaultPropertyInclusion(JsonInclude.Value.ALL_NON_NULL)
        .build();

    private MCRUserObjectMapper() {
    }
}
