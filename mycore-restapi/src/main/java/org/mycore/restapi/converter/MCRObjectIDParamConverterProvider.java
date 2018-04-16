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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObjectID;

@Provider
public class MCRObjectIDParamConverterProvider implements ParamConverterProvider {

    public static final String MSG_INVALID = "Invalid ID supplied";

    public static final int CODE_INVALID = 400; //Bad Request

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations)
        throws BadRequestException {
        if (MCRObjectID.class.isAssignableFrom(rawType)) {
            return new ParamConverter<T>() {
                @Override
                public T fromString(String value) {
                    try {
                        return rawType.cast(MCRObjectID.getInstance(value));
                    } catch (MCRException e) {
                        throw new BadRequestException(Response.status(CODE_INVALID)
                            .entity(MSG_INVALID)
                            .build());
                    }
                }

                @Override
                public String toString(T value) {
                    return value.toString();
                }
            };
        }
        return null;
    }
}
