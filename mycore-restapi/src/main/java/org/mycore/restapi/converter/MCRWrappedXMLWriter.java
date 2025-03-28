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

package org.mycore.restapi.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@Provider
@Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public class MCRWrappedXMLWriter implements MessageBodyWriter<Object> {

    private static final Map<Class, JAXBContext> CTX_MAP = new ConcurrentHashMap<>();

    private static final Predicate<Class> JAXB_CHECKER = type -> type.isAnnotationPresent(XmlRootElement.class)
        || type.isAnnotationPresent(XmlType.class);

    private static boolean verifyArrayType(Class type) {
        Class componentType = type.getComponentType();
        return JAXB_CHECKER.test(componentType) || JAXBElement.class.isAssignableFrom(componentType);
    }

    private static boolean verifyGenericType(Type genericType) {
        if (!(genericType instanceof ParameterizedType pt) ||
            pt.getActualTypeArguments().length > 1) {
            return false;
        }

        final Type ta = pt.getActualTypeArguments()[0];

        if (ta instanceof ParameterizedType lpt) {
            return (lpt.getRawType() instanceof Class rawType)
                && JAXBElement.class.isAssignableFrom(rawType);
        }

        return pt.getActualTypeArguments()[0] instanceof Class listClass && JAXB_CHECKER.test(listClass);
    }

    private static Class getElementClass(Class<?> type, Type genericType) {
        Type ta;
        if (genericType instanceof ParameterizedType parameterizedType) {
            ta = parameterizedType.getActualTypeArguments()[0];
        } else if (genericType instanceof GenericArrayType arrayType) {
            ta = arrayType.getGenericComponentType();
        } else {
            ta = type.getComponentType();
        }
        if (ta instanceof ParameterizedType parameterizedType) {
            //JAXBElement
            ta = parameterizedType.getActualTypeArguments()[0];
        }
        return (Class) ta;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (Stream.of(annotations).noneMatch(a -> XmlElementWrapper.class.isAssignableFrom(a.annotationType()))) {
            return false;
        }
        if (Collection.class.isAssignableFrom(type)) {
            return verifyGenericType(genericType) && Stream.of(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE)
                .anyMatch(t -> t.isCompatible(mediaType));
        } else {
            return type.isArray() && verifyArrayType(type)
                && Stream.of(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE)
                    .anyMatch(t -> t.isCompatible(mediaType));
        }
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream) throws IOException, WebApplicationException {
        Collection collection = (type.isArray()) ? Arrays.asList((Object[]) t) : (Collection) t;
        Class elementType = getElementClass(type, genericType);
        Supplier<Marshaller> m = () -> {
            try {
                JAXBContext ctx = CTX_MAP.computeIfAbsent(elementType, et -> {
                    try {
                        return JAXBContext.newInstance(et);
                    } catch (JAXBException e) {
                        throw new InternalServerErrorException(e);
                    }
                });
                Marshaller marshaller = ctx.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
                return marshaller;
            } catch (JAXBException e) {
                throw new InternalServerErrorException(e);
            }
        };
        try {
            XmlElementWrapper wrapper = Stream.of(annotations)
                .filter(a -> XmlElementWrapper.class.isAssignableFrom(a.annotationType()))
                .map(XmlElementWrapper.class::cast)
                .findAny()
                .get();
            writeCollection(wrapper, collection, StandardCharsets.UTF_8, m, entityStream);
        } catch (JAXBException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    public final void writeCollection(XmlElementWrapper wrapper, Collection<?> t, Charset c,
        Supplier<Marshaller> m, OutputStream entityStream)
        throws JAXBException, IOException {
        final String rootElement = wrapper.name();

        entityStream.write(
            String.format(Locale.ROOT, "<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>", c.name())
                .getBytes(c));
        if (t.isEmpty()) {
            entityStream.write(String.format(Locale.ROOT, "<%s />", rootElement).getBytes(c));
        } else {
            entityStream.write(String.format(Locale.ROOT, "<%s>", rootElement).getBytes(c));
            Marshaller marshaller = m.get();
            for (Object o : t) {
                marshaller.marshal(o, entityStream);
            }
            entityStream.write(String.format(Locale.ROOT, "</%s>", rootElement).getBytes(c));
        }
    }
}
