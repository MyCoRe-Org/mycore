package org.mycore.iiif.common;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

import static org.mycore.frontend.MCRFrontendUtil.BASE_URL_ATTRIBUTE;

public class MCRIIIFBaseURLFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest httpRequest;


    @Override public void filter(ContainerRequestContext requestContext) throws IOException {
        // set BASE_URL_ATTRIBUTE to MCRSession
        if (httpRequest.getAttribute(BASE_URL_ATTRIBUTE) != null) {
            final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            if (currentSession != null) {
                currentSession.put(BASE_URL_ATTRIBUTE, httpRequest.getAttribute(BASE_URL_ATTRIBUTE));
            }
        }
    }
}
