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
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;

public abstract class MCRContentAbstractWriter implements MessageBodyWriter<MCRContent> {
    public static final String PARAM_OBJECTTYPE = "objectType";

    private static Optional<String> getTransformerId(Annotation[] annotations, String format, String detail) {
        return Stream.of(annotations)
            .filter(a -> MCRParams.class.isAssignableFrom(a.annotationType()))
            .map(MCRParams.class::cast)
            .flatMap(p -> Stream.of(p.values()))
            .filter(p -> p.name().equals(PARAM_OBJECTTYPE))
            .findAny()
            .map(MCRParam::value)
            .map(objectType -> objectType + "-" + format + "-" + detail);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return getTransfomerFormat().isCompatible(mediaType) && MCRContent.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(MCRContent content, Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {
        String format = getTransfomerFormat().getSubtype();
        String detail = mediaType.getParameters().getOrDefault(MCRDetailLevel.MEDIA_TYPE_PARAMETER,
            MCRDetailLevel.normal.toString());
        LogManager.getLogger().debug("MediaType={}, format={}, detail={}", mediaType, format, detail);
        Optional<String> transformerId = getTransformerId(annotations, format, detail);
        LogManager.getLogger().debug("Transformer for {} would be {}.", content.getSystemId(),
            transformerId.orElse(null));
        Optional<MCRContentTransformer> transformer = transformerId
            .map(MCRContentTransformerFactory::getTransformer);
        if (transformer.isPresent()) {
            transformer.get().transform(content, entityStream);
        } else if (MCRDetailLevel.normal.toString().equals(detail)) {
            handleFallback(content, entityStream);
        } else if (transformerId.isPresent()) {
            throw new InternalServerErrorException("MCRContentTransformer " + transformerId.get() + " is not defined.");
        }
        LogManager.getLogger().warn("Could not get MCRContentTransformer from request");
        handleFallback(content, entityStream);
    }

    protected abstract MediaType getTransfomerFormat();

    protected abstract void handleFallback(MCRContent content, OutputStream entityStream) throws IOException;
}
