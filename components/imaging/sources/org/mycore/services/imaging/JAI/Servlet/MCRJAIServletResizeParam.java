package org.mycore.services.imaging.JAI.Servlet;

import javax.servlet.http.HttpServletRequest;

import org.mycore.services.imaging.JAI.MCRJAIManipBean;
import org.mycore.services.imaging.JAI.imgOperation.MCRJAIResizeOp;

public class MCRJAIServletResizeParam implements MCRJAIServletParam {
    private static final String PARAM_NAME = "rsz";

    public String getParamName() {
        return PARAM_NAME;
    }

    public void initParam(HttpServletRequest request, MCRJAIManipBean manipBean) {
        String siz = request.getParameter(PARAM_NAME);
        
        if (siz != null && siz.contains("x")) {
          String[] dimension = siz.split("x");

          if (dimension.length == 2) {
              int width = Integer.parseInt(dimension[0]);
              int height = Integer.parseInt(dimension[1]);

              manipBean.addManipOp(new MCRJAIResizeOp(width, height));
          }
      }
    }

}
