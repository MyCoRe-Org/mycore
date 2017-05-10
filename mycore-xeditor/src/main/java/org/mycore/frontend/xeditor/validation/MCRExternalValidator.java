package org.mycore.frontend.xeditor.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

public class MCRExternalValidator extends MCRValidator {

    private String className;

    private String methodName;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue("class") && hasAttributeValue("method");
    }

    @Override
    public void configure() {
        className = getAttributeValue("class");
        methodName = getAttributeValue("method");
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        boolean isValid = true; // all nodes must validate
        for (Object node : binding.getBoundNodes()) {
            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) // do not validate already invalid nodes
                continue;

            Boolean result = isValid(node);
            if (result == null)
                continue;

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }

    protected Boolean isValid(Object node) {
        Method method = findMethod(node.getClass());
        if (method != null)
            return invokeMethod(method, node);
        else {
            method = findMethod(String.class);
            if (method != null) {
                String value = MCRBinding.getValue(node);
                return value.isEmpty() ? null : invokeMethod(method, value);
            } else
                throw new MCRConfigurationException(
                    "Method configured for external validation not found: " + className + "#" + methodName);
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
            Boolean b = (Boolean) result;
            return b;
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }
}
