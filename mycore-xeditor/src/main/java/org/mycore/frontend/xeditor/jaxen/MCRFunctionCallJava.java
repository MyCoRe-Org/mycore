package org.mycore.frontend.xeditor.jaxen;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;

class MCRFunctionCallJava implements org.jaxen.Function {

    private final static Logger LOGGER = LogManager.getLogger(MCRFunctionCallJava.class);

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        try {
            String clazzName = (String) (args.get(0));
            String methodName = (String) (args.get(1));
            LOGGER.info("XEditor extension function calling " + clazzName + " " + methodName);

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
