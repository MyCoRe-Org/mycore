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

package org.mycore.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mycore.common.config.MCRConfigurationException;

/**
 * This annotation is used to mark fields or methods that should be set to or called with a value of type
 * {@link String} from the configuration properties.
 * <p>
 * The field or method needs to be public.
  *
 * @author Sebastian Hofmann
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRProperty {

    /**
     * @return The name of property, <code>*</code> for a map of all properties or a prefix,
     * followed by <code>.*</code> for a map of all properties whose name starts with that prefix.
     * Support for maps will be removed in a future release of MyCoRe. {@link MCRRawProperties}
     * should be used instead.
     */
    String name();

    /**
     * @return true if the property specified by {@link MCRProperty#name()} has to be present in the properties.
     * {@link MCRConfigurationException} is thrown if the property is required but not present.
     */
    boolean required() default true;

    /**
     * @return true if the property name specified by {@link MCRProperty#name()} is absolute and not specific for
     * this instance e.g. MCR.NameOfProject.
     */
    boolean absolute() default false;

    /**
     * @return The name for a default property that should be used as a default value if the property
     * specified by {@link MCRProperty#name()} is not present in the properties. {@link MCRConfigurationException} is
     * thrown if the default property is not present. The default property must be absolute, e.g. MCR.NameOfProject.
     */
    String defaultName() default "";

    /**
     * @return The order in which the annotated fields or methods are processed. The higher the value, the later the
     * field or method is processed. All fields are processed first, then all methods are processed.
     */
    int order() default 0;

}
