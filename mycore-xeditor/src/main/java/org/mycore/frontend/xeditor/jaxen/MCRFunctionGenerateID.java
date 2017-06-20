package org.mycore.frontend.xeditor.jaxen;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;

class MCRFunctionGenerateID implements org.jaxen.Function {

    private final static Logger LOGGER = LogManager.getLogger(MCRFunctionGenerateID.class);

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        try {
            Object targetNode = args.isEmpty() ? context.getNodeSet().get(0) : ((List) args.get(0)).get(0);
            return "n" + System.identityHashCode(targetNode);
        } catch (Exception ex) {
            LOGGER.warn("Exception in call to generate-id", ex);
            return ex.getMessage();
        }
    }
}
