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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.restapi.v2.MCRErrorResponse;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ParamConverterProvider} for {@link MCRCategoryID}.
 * <p>
 * This provider enables the use of {@code MCRCategoryID} as a parameter type in JAX-RS
 * resource methods (for example, with {@code @PathParam} or {@code @QueryParam}).
 * The expected string representation is the standard {@code MCRCategoryID} format,
 * typically {@code "classid:categid"} as parsed by {@link MCRCategoryID#ofString(String)}.
 * </p>
 * <p>
 * When the incoming parameter value is {@code null}, an {@link IllegalArgumentException}
 * is thrown. If {@link MCRCategoryID#ofString(String)} fails (for example, due to an
 * invalid format or unknown category), this provider throws a JAX-RS
 * {@link jakarta.ws.rs.WebApplicationException} created by {@link MCRErrorResponse} with
 * HTTP status code {@value #CODE_INVALID} (Bad Request), error code
 * {@code "MCRCATEGORYID_INVALID"} and message {@value #MSG_INVALID}.
 * </p>
 *
 * @author Thomas Scheffler (yagee)
 * @since 2024.06.2, 2025.06.2, 2025.12.2
 */
@Provider
public class MCRCategoryIDParamConverterProvider implements ParamConverterProvider {

    public static final String MSG_INVALID = "Invalid ID supplied";

    public static final int CODE_INVALID = 400; //Bad Request

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations)
        throws BadRequestException {
        if (MCRCategoryID.class.isAssignableFrom(rawType)) {
            return new ParamConverter<>() {
                @Override
                public T fromString(String value) {
                    if (value == null) {
                        throw new IllegalArgumentException("value may not be null");
                    }
                    try {
                        return rawType.cast(MCRCategoryID.ofString(value));
                    } catch (MCRException e) {
                        throw MCRErrorResponse.ofStatusCode(CODE_INVALID)
                            .withErrorCode("MCRCATEGORYID_INVALID")
                            .withMessage(MSG_INVALID)
                            .withDetail(e.getMessage())
                            .withCause(e)
                            .toException();
                    }
                }

                @Override
                public String toString(T value) {
                    if (value == null) {
                        throw new IllegalArgumentException("value may not be null");
                    }
                    return value.toString();
                }
            };
        }
        return null;
    }
}
