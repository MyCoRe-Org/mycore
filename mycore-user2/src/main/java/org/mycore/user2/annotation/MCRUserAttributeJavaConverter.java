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
package org.mycore.user2.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mycore.user2.MCRUserAttributeConverter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Retention(RUNTIME)
@Target({ METHOD, FIELD })
public @interface MCRUserAttributeJavaConverter {

    /**
     * Points to the class that converts a value type to a bound type or vice versa.
     * See {@link MCRUserAttributeConverter} for more details.
     */
    Class<? extends MCRUserAttributeConverter> value();

}
