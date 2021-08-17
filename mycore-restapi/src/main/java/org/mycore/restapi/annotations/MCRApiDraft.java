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

package org.mycore.restapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark REST methods that are not stable for production.
 *
 * Those endpoints maybe changed or removed at a later date.
 * @author Thomas Scheffler (yagee)
 * @see org.mycore.restapi.MCRApiDraftFilter
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)

public @interface MCRApiDraft {

    /**
     * A simple value to mark a group of endpoints.
     *
     * Endpoints belong to the same group if this value is the same.
     */
    String value();
}
