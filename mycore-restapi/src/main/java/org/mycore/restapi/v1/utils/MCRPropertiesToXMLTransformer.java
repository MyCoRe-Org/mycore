package org.mycore.restapi.v1.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Produces(MediaType.APPLICATION_XML)
@Provider
public class MCRPropertiesToXMLTransformer implements MessageBodyWriter<Properties> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Properties.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public long getSize(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_XML_TYPE.withCharset(StandardCharsets.UTF_8.name()));
        t.storeToXML(entityStream, null);
    }

}
