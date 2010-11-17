package org.mycore.frontend.editor.validation.pair;

import java.lang.reflect.Method;

public class MCRExternalPairValidator extends MCRPairValidatorBase implements MCRPairValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("class") && hasProperty("method");
    }

    @Override
    public boolean isValidPairOrDie(String valueA, String valueB) throws Exception {
        String clazz = getProperty("class");
        String method = getProperty("method");
        Method m = getMethod(clazz, method);
        return invokeMethod(m, valueA, valueB);
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(String clazz, String method) throws Exception {
        Class[] argTypes = { String.class, String.class };
        Method m = Class.forName(clazz).getMethod(method, argTypes);
        return m;
    }

    private boolean invokeMethod(Method m, String valueA, String valueB) throws Exception {
        Object[] args = { valueA, valueB };
        Object result = m.invoke(null, args);
        return result.equals(new Boolean(true));
    }

}
