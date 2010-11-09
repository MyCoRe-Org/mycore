package org.mycore.frontend.editor.validation;

import java.lang.reflect.Method;

public class MCRExternalValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("class") && hasProperty("method");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        String clazz = getProperty("class");
        String method = getProperty("method");
        Method m = getMethod(clazz, method);
        return invokeMethod(m, input);
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(String clazz, String method) throws Exception {
        Class[] argTypes = { String.class };
        Method m = Class.forName(clazz).getMethod(method, argTypes);
        return m;
    }

    private boolean invokeMethod(Method m, String input) throws Exception {
        Object[] args = { input };
        Object result = m.invoke(null, args);
        return result.equals(new Boolean(true));
    }
}
