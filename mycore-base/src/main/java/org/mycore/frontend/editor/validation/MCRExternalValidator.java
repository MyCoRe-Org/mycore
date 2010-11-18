package org.mycore.frontend.editor.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MCRExternalValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("class") && hasProperty("method");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean isValidOrDie(Object... input) throws Exception {
        Class[] argTypes = buildArgTypes(input);
        Method m = getMethod(argTypes);
        return invokeMethod(m, input);
    }

    @SuppressWarnings("unchecked")
    private Class[] buildArgTypes(Object... input) {
        Class[] argTypes = new Class[input.length];
        for (int i = 0; i < input.length; i++)
            argTypes[i] = input[i].getClass();
        return argTypes;
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(Class[] argTypes) throws NoSuchMethodException, ClassNotFoundException {
        String clazz = getProperty("class");
        String method = getProperty("method");
        Method m = Class.forName(clazz).getMethod(method, argTypes);
        return m;
    }

    private boolean invokeMethod(Method m, Object... input) throws IllegalAccessException, InvocationTargetException {
        Object result = m.invoke(null, input);
        return result.equals(new Boolean(true));
    }
}
