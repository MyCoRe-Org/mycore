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

package org.mycore.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mycore.common.config.MCRConfigurationException;

/**
 * This annotation is used to mark fields or methods that should be set to or called with
 * a map of configured instances of a type compatible with {@link MCRInstanceMap#valueClass()}
 * which are configured from the configuration properties.
 * <p>
 * The field or method needs to be public.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRInstanceMap {

    /**
     * @return The prefix for names of properties containing the class names.
     */
    String name() default "";

    /**
     * @return The class or a superclass of the configured instances.
     */
    Class<?> valueClass();

    /**
     * @return true if the at least one sub-property of the property specified by {@link MCRInstanceMap#name()}
     * has to be present in the properties. {@link MCRConfigurationException} is thrown if the property is required
     * but not present.
     */
    boolean required() default true;

    /**
     * @return the order in which the annotated fields or methods are processed. The higher the value, the later the
     * field or method is processed. All fields are processed first, then all methods are processed.
     */
    int order() default 0;

}
