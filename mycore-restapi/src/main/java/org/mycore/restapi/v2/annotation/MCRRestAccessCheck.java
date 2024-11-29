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

package org.mycore.restapi.v2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mycore.restapi.v2.access.MCRRestAccessCheckStrategy;

/**
 * Annotation for specifying custom authorization checks for methods.
 * <p>
 * This annotation can be used to mark methods that require a specific access check
 * defined by a strategy. The {@code strategy} attribute should be set to a class that
 * implements {@link MCRRestAccessCheckStrategy}. The specified strategy will be used to perform
 * the authorization check when the annotated method is invoked.
 * </p>
 *
 * @see MCRRestAccessCheckStrategy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MCRRestAccessCheck {

    /**
     * Specifies the class that implements a access strategy.
     *
     * @return the class that implements {@link MCRRestAccessCheckStrategy}
     */
    Class<? extends MCRRestAccessCheckStrategy> strategy();
}
