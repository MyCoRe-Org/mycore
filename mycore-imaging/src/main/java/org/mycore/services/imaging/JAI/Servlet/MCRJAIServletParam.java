package org.mycore.services.imaging.JAI.Servlet;

import javax.servlet.http.HttpServletRequest;

import org.mycore.services.imaging.JAI.MCRJAIManipBean;

public interface MCRJAIServletParam {
    public String getParamName();
    public void initParam(HttpServletRequest request, MCRJAIManipBean manipBean);
}
