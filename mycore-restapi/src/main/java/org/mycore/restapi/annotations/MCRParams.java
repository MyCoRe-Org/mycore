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

package org.mycore.restapi.annotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public @interface MCRParams {

    MCRParam[] values();

    class Factory {
        public static MCRParams get(MCRParam... params) {
            return new MCRParams() {
                @Override
                public MCRParam[] values() {
                    return params;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return MCRParams.class;
                }

                @Override
                public String toString() {
                    return "@" + MCRParams.class.getName() + Arrays.toString(values());
                }

                @Override
                public int hashCode() {
                    return ("values".hashCode() * 127) ^ Arrays.hashCode(values());
                }

                @Override
                public boolean equals(Object obj) {
                    return obj instanceof MCRParams that && Arrays.equals(this.values(), that.values());
                }
            };
        }
    }

}
