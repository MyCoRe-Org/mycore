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

package org.mycore.restapi.v2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mycore.restapi.v2.access.MCRRestRequiredPermissionDefaultResolver;
import org.mycore.restapi.v2.access.MCRRestRequiredPermissionResolver;

/**
 * Defines a required permission for a Rest API path.
 * This can be a MyCoRe standard permission.
 *
 * @see org.mycore.access.MCRAccessManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MCRRestRequiredPermission {

    /**
     * Returns the permission value.
     *
     * @return the permission value
     */
    String value() default "";

    /**
     * Resolves the required permission.
     *
     * @return the permission value
     */
    Class<? extends MCRRestRequiredPermissionResolver> resolver()
        default MCRRestRequiredPermissionDefaultResolver.class;
}
