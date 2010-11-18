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
        if (input == null)
            input = new Object[] { null };

        Class[] argTypes = buildArgTypes(input);
        Method m = getMethod(argTypes);
        return invokeMethod(m, input);
    }

    @SuppressWarnings("unchecked")
    protected Class getArgumentType() {
        return String.class;
    }

    @SuppressWarnings("unchecked")
    private Class[] buildArgTypes(Object... input) {
        Class[] argTypes = new Class[input.length];
        for (int i = 0; i < argTypes.length; i++)
            argTypes[i] = getArgumentType();
        return argTypes;
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(Class[] argTypes) throws NoSuchMethodException, ClassNotFoundException {
        String clazz = getProperty("class");
        String method = getProperty("method");
        return Class.forName(clazz).getMethod(method, argTypes);
    }

    private boolean invokeMethod(Method m, Object... input) throws IllegalAccessException, InvocationTargetException {
        Object result = m.invoke(null, input);
        return result.equals(new Boolean(true));
    }
}
