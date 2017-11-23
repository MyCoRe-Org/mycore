package org.mycore.frontend.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mycore.frontend.MCRFrontendUtil;

public class MCRWebAppBaseFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException {
        // check if BASE_URL_ATTRIBUTE is present
        // for used proxy header use the first entry of list
        if (req.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE) == null) {
            String webappBase = MCRFrontendUtil.getBaseURL(req);
            req.setAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE, webappBase);
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }

}
