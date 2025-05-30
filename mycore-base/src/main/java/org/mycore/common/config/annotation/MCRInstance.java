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
 * This annotation is used to mark fields or methods that should be set to or called with
 * a configured instance of a type compatible with {@link MCRInstance#valueClass()}
 * which is configured from the configuration properties.
 * <p>
 * The field or method needs to be public.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRInstance {

    /**
     * @return The name of property containing the class name.
     */
    String name();

    /**
     * @return The class or a superclass of the configured instance.
     */
    Class<?> valueClass();

    /**
     * @return true if the property specified by {@link MCRInstance#name()} has to be present in the properties.
     * {@link MCRConfigurationException} is thrown if a value is required but not present.
     */
    boolean required() default true;

    /**
     * @return The {@link MCRSentinel} for the configured instance.
     */
    MCRSentinel sentinel() default @MCRSentinel(enabled = false);

    /**
     * @return The order in which the annotated fields or methods are processed. The higher the value, the later the
     * field or method is processed. All fields are processed first, then all methods are processed.
     */
    int order() default 0;

}
