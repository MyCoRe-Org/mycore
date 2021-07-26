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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.datamodel.metadata.MCRMetaDefault;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.annotation.XmlElementWrapper;

@Provider
@Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML + ";charset=UTF-8" })
public class MCRMetaDefaultListXMLWriter implements MessageBodyWriter<List<? extends MCRMetaDefault>> {

    public static final String PARAM_XMLWRAPPER = "xmlWrapper";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Stream.of(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE)
            .anyMatch(t -> t.isCompatible(mediaType)) && List.class.isAssignableFrom(type)
            && MCRConverterUtils.isType(genericType, MCRMetaDefault.class) && getWrapper(annotations).isPresent();
    }

    private static Optional<String> getWrapper(Annotation... annotations) {
        return Stream.of(annotations)
            .filter(a -> XmlElementWrapper.class.isAssignableFrom(a.annotationType()))
            .findAny()
            .map(XmlElementWrapper.class::cast)
            .map(XmlElementWrapper::name);
    }

    @Override
    public void writeTo(List<? extends MCRMetaDefault> mcrMetaDefaults, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {
        Optional<String> wrapper = getWrapper(annotations);
        if (!wrapper.isPresent()) {
            throw new InternalServerErrorException("Could not get XML wrapping element from annotations.");
        }
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_TYPE);
        Element root = new Element(wrapper.get());
        root.addContent(mcrMetaDefaults.stream()
            .map(MCRMetaDefault::createXML)
            .collect(Collectors.toList()));
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(root, entityStream);
    }
}
