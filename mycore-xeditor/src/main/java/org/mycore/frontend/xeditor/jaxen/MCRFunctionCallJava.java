package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;

class MCRFunctionCallJava implements org.jaxen.Function {

    private final static Logger LOGGER = Logger.getLogger(MCRFunctionCallJava.class);

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        try {
            String clazz = (String) (args.get(0));
            String method = (String) (args.get(1));
            System.out.println(clazz + " " + method);

            Class[] argTypes = new Class[args.size() - 2];
            Object[] params = new Object[args.size() - 2];
            for (int i = 0; i < argTypes.length; i++) {
                argTypes[i] = Object.class;
                params[i] = args.get(i + 2);
            }

            Method m = Class.forName(clazz).getMethod(method, argTypes);
            return m.invoke(null, params);
        } catch (Exception ex) {
            LOGGER.warn("Exception in call to external java method", ex);
            return ex.getMessage();
        }
    }
}