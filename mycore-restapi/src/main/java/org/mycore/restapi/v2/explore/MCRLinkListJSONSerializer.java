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

package org.mycore.restapi.v2.explore;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * a JSON serializer which converts a list of links to a JSON object with properties for each link.
 *
 * @author Robert Stephan
 *
 */
public class MCRLinkListJSONSerializer extends StdSerializer<List<MCRRestExploreResponseLink>> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public MCRLinkListJSONSerializer() {
        super((Class<List<MCRRestExploreResponseLink>>) ((Class<?>) List.class));
    }

    @Override
    public void serialize(List<MCRRestExploreResponseLink> value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeStartObject();
        for (MCRRestExploreResponseLink link : value) {
            generator.writeStringField(link.getRel(), link.getUrl());
        }
        generator.writeEndObject();
    }
}
