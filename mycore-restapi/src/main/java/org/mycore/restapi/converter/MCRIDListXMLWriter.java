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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.mycore.datamodel.common.MCRObjectIDDate;

@Provider
@Produces(MediaType.TEXT_XML)
public class MCRIDListXMLWriter implements MessageBodyWriter<List<MCRObjectIDDate>> {

    private static final String XML_WRAPPER = "mycoreobjects";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.TEXT_XML_TYPE.isCompatible(mediaType) && List.class.isAssignableFrom(type)
            && MCRConverterUtils.isType(genericType, MCRObjectIDDate.class);
    }

    @Override
    public void writeTo(List<MCRObjectIDDate> mcrObjectIDDates, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {

        Charset charset = StandardCharsets.UTF_8;
        entityStream.write(String.format(Locale.ROOT, "<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>\n",
            charset).getBytes(charset));
        entityStream.write(String.format(Locale.ROOT, "<%s>", XML_WRAPPER).getBytes(charset));
        for (MCRObjectIDDate i : mcrObjectIDDates) {
            entityStream
                .write(String.format(Locale.ROOT, "\n  <%s id=\"%s\" lastModified=\"%s\">", "mycoreobject", i.getId(),
                    i.getLastModified().toInstant()).getBytes(charset));
        }
        entityStream.write(String.format(Locale.ROOT, "\n</%s>", XML_WRAPPER).getBytes(charset));
    }
}
