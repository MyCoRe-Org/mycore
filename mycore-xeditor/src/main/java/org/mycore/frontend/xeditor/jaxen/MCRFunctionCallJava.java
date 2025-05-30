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

package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.Context;

class MCRFunctionCallJava implements org.jaxen.Function {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Object call(Context context, List args) {
        try {
            String clazzName = (String) (args.get(0));
            String methodName = (String) (args.get(1));
            LOGGER.debug("XEditor extension function calling {} {}", clazzName, methodName);

            Class[] argTypes = new Class[args.size() - 2];
            Object[] params = new Object[args.size() - 2];
            for (int i = 0; i < argTypes.length; i++) {
                argTypes[i] = args.get(i + 2).getClass();
                params[i] = args.get(i + 2);
            }

            Class clazz = ClassUtils.getClass(clazzName);
            Method method = MethodUtils.getMatchingAccessibleMethod(clazz, methodName, argTypes);
            return method.invoke(null, params);
        } catch (Exception ex) {
            LOGGER.warn("Exception in call to external java method", ex);
            return ex.getMessage();
        }
    }
}
