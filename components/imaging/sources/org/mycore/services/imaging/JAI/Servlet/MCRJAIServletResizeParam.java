package org.mycore.services.imaging.JAI.Servlet;

import javax.servlet.http.HttpServletRequest;

import org.mycore.services.imaging.JAI.MCRJAIManipBean;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIResizeOp;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIScaleOp;

public class MCRJAIServletResizeParam implements MCRJAIServletParam {
    private static final String PARAM_NAME = "rsz";

    public String getParamName() {
        return PARAM_NAME;
    }

    public void initParam(HttpServletRequest request, MCRJAIManipBean manipBean) {
        String paramValue = request.getParameter(PARAM_NAME);

        if (paramValue != null) {
            if (paramValue.contains("x")) {
                String[] dimension = paramValue.split("x");
                if (dimension.length == 2) {
                    int width = (dimension[0].equals("_")) ? -1 : Integer.parseInt(dimension[0]);
                    int height = (dimension[1].equals("_")) ? -1 : Integer.parseInt(dimension[1]);

                    manipBean.addManipOp(new MCRJAIResizeOp(width, height));
                }
            } else {
                try {
                    float magFactor = Float.parseFloat(paramValue) / 100;
                    manipBean.addManipOp(new MCRJAIScaleOp(magFactor));
                } catch (NumberFormatException e) {
                    // when the param value is no number do nothing
                }
            }
        }
    }

}
