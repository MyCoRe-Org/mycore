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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@Provider
@Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
public class MCRWrappedXMLWriter implements MessageBodyWriter<Object> {

    private static final ConcurrentHashMap<Class, JAXBContext> CTX_MAP = new ConcurrentHashMap<>();

    private static final Predicate<Class> JAXB_CHECKER = type -> type.isAnnotationPresent(XmlRootElement.class)
        || type.isAnnotationPresent(XmlType.class);

    private static boolean verifyArrayType(Class type) {
        Class componentType = type.getComponentType();
        return JAXB_CHECKER.test(componentType) || JAXBElement.class.isAssignableFrom(componentType);
    }

    private static boolean verifyGenericType(Type genericType) {
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        final ParameterizedType pt = (ParameterizedType) genericType;

        if (pt.getActualTypeArguments().length > 1) {
            return false;
        }

        final Type ta = pt.getActualTypeArguments()[0];

        if (ta instanceof ParameterizedType) {
            ParameterizedType lpt = (ParameterizedType) ta;
            return (lpt.getRawType() instanceof Class)
                && JAXBElement.class.isAssignableFrom((Class) lpt.getRawType());
        }

        if (!(pt.getActualTypeArguments()[0] instanceof Class)) {
            return false;
        }

        final Class listClass = (Class) pt.getActualTypeArguments()[0];

        return JAXB_CHECKER.test(listClass);
    }

    private static Class getElementClass(Class<?> type, Type genericType) {
        Type ta;
        if (genericType instanceof ParameterizedType) {
            ta = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } else if (genericType instanceof GenericArrayType) {
            ta = ((GenericArrayType) genericType).getGenericComponentType();
        } else {
            ta = type.getComponentType();
        }
        if (ta instanceof ParameterizedType) {
            //JAXBElement
            ta = ((ParameterizedType) ta).getActualTypeArguments()[0];
        }
        return (Class) ta;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (Stream.of(annotations).noneMatch(a -> XmlElementWrapper.class.isAssignableFrom(a.annotationType()))) {
            return false;
        }
        if (Collection.class.isAssignableFrom(type)) {
            return verifyGenericType(genericType) && Stream.of(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE).anyMatch(t -> t.isCompatible(mediaType));
        } else {
            return type.isArray() && verifyArrayType(type)
                && Stream.of(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE).anyMatch(t -> t.isCompatible(mediaType));
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
