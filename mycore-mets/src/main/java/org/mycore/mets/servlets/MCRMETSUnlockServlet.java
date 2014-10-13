package org.mycore.mets.servlets;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.tools.MCRMetsLock;

public class MCRMETSUnlockServlet extends MCRServlet {

    private static final long serialVersionUID = -5456313294869486351L;
    private static final Logger LOGGER = Logger.getLogger(MCRMetsLock.class);
    
    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String derivate = job.getRequest().getParameter("derivate");
        
        if(derivate != null){
            MCRMetsLock.doUnlock(derivate);
        } else {
            LOGGER.warn("Derivate is null!");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"derivate\" isn't set!");
        }
        
    }
    
}
