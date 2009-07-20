package org.mycore.services.imaging.JAI.Servlet;

import javax.servlet.http.HttpServletRequest;

import org.mycore.services.imaging.JAI.MCRJAIManipBean;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIScaleOp;

public class MCRJAIServletScaleParam implements MCRJAIServletParam {
    private static final String PARAM_NAME = "scl";

    public String getParamName() {
        return PARAM_NAME;
    }

    public void initParam(HttpServletRequest request, MCRJAIManipBean manipBean) {
        String scl = request.getParameter(PARAM_NAME);
        
        if (scl != null) {
            float magFactor = Float.parseFloat(scl) / 100;
            manipBean.addManipOp(new MCRJAIScaleOp(magFactor));
        }
    }

}
