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

package org.mycore.frontend.xeditor.validation;

import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Validates edited xml using an external method. 
 * The method must be "public static boolean ..." 
 * and should accept a String, Element, Attribute or Object as argument.
 *   
 * Example: &lt;xed:validate class="foo.bar.MyValidator" method="validateISBN" ... /&gt;
 *
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRExternalValidator extends MCRValidator {

    private static final String ATTR_METHOD = "method";

    private static final String ATTR_CLASS = "class";

    private String className;

    private String methodName;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue(ATTR_CLASS) && hasAttributeValue(ATTR_METHOD);
    }

    @Override
    public void configure() {
        className = getAttributeValue(ATTR_CLASS);
        methodName = getAttributeValue(ATTR_METHOD);
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = true; // all nodes must validate
        for (Object node : binding.getBoundNodes()) {
            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) {
                continue;
            }

            Boolean result = isValid(node);
            if (result == null) {
                continue;
            }

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }

    protected Boolean isValid(Object node) {
        Method method = findMethod(node.getClass());
        if (method != null) {
            return invokeMethod(method, node);
        } else {
            method = findMethod(String.class);
            if (method != null) {
                String value = MCRBinding.getValue(node);
                return value.isEmpty() ? null : invokeMethod(method, value);
            } else {
                throw new MCRConfigurationException(
                    "Method configured for external validation not found: " + className + "#" + methodName);
            }
        }
    }

    private Method findMethod(Class<?> argType) {
        try {
            Class<?> clazz = ClassUtils.getClass(className);
            Class<?>[] argTypes = { argType };
            return MethodUtils.getMatchingAccessibleMethod(clazz, methodName, argTypes);
        } catch (ClassNotFoundException ex) {
            throw new MCRConfigurationException("class configured for external validation not found: " + className);
        }
    }

    private Boolean invokeMethod(Method method, Object param) {
        try {
            Object[] params = { param };
            Object result = method.invoke(null, params);
            return (Boolean) result;
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }
}
