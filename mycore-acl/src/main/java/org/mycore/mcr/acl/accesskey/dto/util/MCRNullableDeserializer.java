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

package org.mycore.mcr.acl.accesskey.dto.util;

import java.io.IOException;
import java.io.Serial;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A custom deserializer for the {@link MCRNullable} class.
 * This deserializer handles JSON null values and wraps them in a {@link MCRNullable} object.
 *
 * @param <T> The type of the value being deserialized.
 */
public class MCRNullableDeserializer<T> extends StdDeserializer<MCRNullable<T>> implements ContextualDeserializer {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The type of the value to be deserialized.
     */
    private JavaType valueType;

    /**
     * Default constructor.
     */
    public MCRNullableDeserializer() {
        super(MCRNullable.class);
    }

    /**
     * Constructor with value type.
     *
     * @param valueType the type of the value to be deserialized.
     */
    public MCRNullableDeserializer(JavaType valueType) {
        super(MCRNullable.class);
        this.valueType = valueType;
    }

    @Override
    public MCRNullable<T> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
            return new MCRNullable<>();
        } else {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            T value = mapper.readValue(p, valueType);
            return new MCRNullable<>(value);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
        throws JsonMappingException {
        JavaType type = ctxt.getContextualType().containedType(0);
        return new MCRNullableDeserializer<>(type);
    }

    @Override
    public MCRNullable<T> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        return new MCRNullable<>(null);
    }
}
