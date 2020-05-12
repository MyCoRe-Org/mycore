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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.mycore.datamodel.classifications2.MCRCategoryID;

@Consumes({ MediaType.TEXT_PLAIN, MediaType.WILDCARD })
@Provider
public class MCRCategoryIDMessageBodyReader implements MessageBodyReader<MCRCategoryID> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return genericType == MCRCategoryID.class;
    }

    @Override
    public MCRCategoryID readFrom(Class<MCRCategoryID> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {
        Charset charset = getCharset(mediaType);
        String categoryID = new String(entityStream.readAllBytes(), charset);
        try {
            return MCRCategoryID.fromString(categoryID);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private static Charset getCharset(MediaType mediaType) {
        return Optional.ofNullable(mediaType)
            .map(m -> m.getParameters().get(MediaType.CHARSET_PARAMETER))
            .map(Charset::forName)
            .orElse(StandardCharsets.UTF_8);
    }

}
