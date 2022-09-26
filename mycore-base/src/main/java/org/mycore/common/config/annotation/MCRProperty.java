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

import org.mycore.common.config.MCRConfigurationException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation tells which properties need to be assigned to which field
 * or method. All annotated members need to be public. The fields should always have the type {@link String} and if you
 * need a custom type, then you can annotate a method with a single parameter of type {@link String}, which can then
 * create/retrieve the object and assign it to your field.
 *
 * @author Sebastian Hofmann
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRProperty {

    /**
     * @return The name of property
     */
    String name();

    /**
     * @return true if the property has to be present in the properties. {@link MCRConfigurationException} is thrown
     * if the property is required but not present.
     */
    boolean required() default true;

    /**
     * @return true if the property is absolute and not specific for this instance e.G. MCR.NameOfProject
     */
    boolean absolute() default false;
}
