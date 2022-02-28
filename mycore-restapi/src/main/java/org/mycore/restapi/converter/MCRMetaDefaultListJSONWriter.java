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

package org.mycore.restapi.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.mycore.datamodel.metadata.MCRMetaDefault;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces({ MediaType.APPLICATION_JSON + ";charset=UTF-8" })
public class MCRMetaDefaultListJSONWriter implements MessageBodyWriter<List<? extends MCRMetaDefault>> {

    public static final String PARAM_XMLWRAPPER = "xmlWrapper";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType) && List.class.isAssignableFrom(type)
            && MCRConverterUtils.isType(genericType, MCRMetaDefault.class);
    }

    @Override
    public void writeTo(List<? extends MCRMetaDefault> mcrMetaDefaults, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {
        LogManager.getLogger().info("Writing JSON array of size: " + mcrMetaDefaults.size());
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_JSON_TYPE.withCharset(StandardCharsets.UTF_8.toString()));
        JsonArray array = new JsonArray();
        mcrMetaDefaults.stream()
            .map(MCRMetaDefault::createJSON)
            .forEach(array::add);
        try (OutputStreamWriter streamWriter = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);) {
            Gson gson = new Gson();
            gson.toJson(array, streamWriter);
        }
    }
}
