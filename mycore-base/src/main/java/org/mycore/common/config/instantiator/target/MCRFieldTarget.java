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

import org.mycore.common.config.MCRConfigurationException;

public final class MCRFieldTarget implements MCRTarget {

    private final Field field;

    public MCRFieldTarget(Field field) {
        this.field = field;
    }

    @Override
    public MCRTargetType type() {
        return MCRTargetType.FIELD;
    }

    @Override
    public Class<?> declaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public String name() {
        return field.getName();
    }

    @Override
    public boolean isAssignableFrom(Class<?> valueClass) {
        return valueClass.isAssignableFrom(field.getType());
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (Exception e) {
            throw new MCRConfigurationException("Failed to set target field '" + name() + "' to '" + value
                + "' in configurable class " + field.getDeclaringClass().getName(), e);
        }
    }

    @Override
    public String toString() {
        return field.getDeclaringClass().getSimpleName() + "#" + field.getName();
    }

}
