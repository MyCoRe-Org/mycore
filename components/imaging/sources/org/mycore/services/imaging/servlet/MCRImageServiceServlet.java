package org.mycore.services.imaging.servlet;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRImageServiceServlet extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRImageServiceServlet.class);
    private MCRServletJobProcessing jobProcessing = (MCRServletJobProcessing) MCRConfiguration.instance().getInstanceOf("MCR.Imaging.Servlet.Processing.Class");

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        jobProcessing.process(job.getRequest(), job.getResponse());
    }
}
