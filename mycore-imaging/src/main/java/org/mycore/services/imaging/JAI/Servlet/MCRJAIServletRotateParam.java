package org.mycore.services.imaging.JAI.Servlet;

import javax.servlet.http.HttpServletRequest;

import org.mycore.services.imaging.JAI.MCRJAIManipBean;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIRotateOp;

public class MCRJAIServletRotateParam implements MCRJAIServletParam {
    private static final String PARAM_NAME = "rot";

    public void initParam(HttpServletRequest request, MCRJAIManipBean manipBean) {
        String paramVal = request.getParameter(PARAM_NAME);
        
        if (paramVal != null) {
            float rotAngle = (float) Math.toRadians(Float.parseFloat(paramVal));
            manipBean.addManipOp(new MCRJAIRotateOp(rotAngle));
        }
    }

    public String getParamName() {
        return PARAM_NAME;
    }

}
