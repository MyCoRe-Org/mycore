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

package org.mycore.common.config.instantiator.target;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Common abstraction for components of a class (i.e., a {@link Field} or a {@link Method}) that
 * is, in fact, being used for injection. It provides an abstraction for injecting obtained values.
 */
public sealed interface MCRTarget permits MCRFieldTarget, MCRMethodTarget {

    Type type();

    Class<?> declaringClass();

    String name();

    boolean isAssignableFrom(Class<?> valueClass);

    void set(Object instance, Object value);

    enum Type {

        FIELD,

        METHOD;

        public int order() {
            return ordinal();
        }

    }

    final class Types {

        public static final Set<Type> FIELD = Set.of(Type.FIELD);

        public static final Set<Type> METHOD = Set.of(Type.METHOD);

        public static final Set<Type> ALL = Collections.unmodifiableSet(EnumSet.allOf(Type.class));

        private Types() {
        }

    }

}
