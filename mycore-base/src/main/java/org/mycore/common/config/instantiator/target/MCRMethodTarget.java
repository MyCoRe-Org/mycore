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

import java.lang.reflect.Method;

import org.mycore.common.config.MCRConfigurationException;

public final class MCRMethodTarget implements MCRTarget {

    private final Method method;

    private final boolean hasParameter;

    public MCRMethodTarget(Method method) {
        this.method = method;
        int numberOfParameters = method.getParameterTypes().length;
        if (numberOfParameters > 1) {
            throw new MCRConfigurationException("Target method '" + method.getName() +
                "' has an unexpected number of parameters (" + numberOfParameters +
                ") in configured class " + method.getDeclaringClass().getName());
        }
        this.hasParameter = numberOfParameters == 1;
    }

    @Override
    public MCRTargetType type() {
        return MCRTargetType.METHOD;
    }

    @Override
    public Class<?> declaringClass() {
        return method.getDeclaringClass();
    }

    @Override
    public String name() {
        return method.getName();
    }

    @Override
    public boolean isAssignableFrom(Class<?> valueClass) {
        return !hasParameter || method.getParameterTypes()[0].isAssignableFrom(valueClass);
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            if (hasParameter) {
                method.invoke(instance, value);
            } else {
                method.invoke(instance);
            }
        } catch (Exception e) {
            throw new MCRConfigurationException("Failed to call target method '" + name() + "' " +
                (hasParameter ? ("with '" + value + "' ") : "") + "in configurable class "
                + method.getDeclaringClass().getName(), e);
        }
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "()";
    }

}
