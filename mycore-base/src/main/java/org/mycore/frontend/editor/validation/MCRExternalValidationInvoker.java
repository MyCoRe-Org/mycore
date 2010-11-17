package org.mycore.frontend.editor.validation;

import java.lang.reflect.Method;

public class MCRExternalValidationInvoker {

    private Method m = null;

    @SuppressWarnings("unchecked")
    public MCRExternalValidationInvoker(MCRConfigurable validator, Class[] argTypes) throws ClassNotFoundException, NoSuchMethodException {
        String clazz = validator.getProperty("class");
        String method = validator.getProperty("method");
        m = Class.forName(clazz).getMethod(method, argTypes);
    }

    public boolean validateExternally(Object[] args) throws Exception {
        Object result = m.invoke(null, args);
        return result.equals(new Boolean(true));
    }
}
