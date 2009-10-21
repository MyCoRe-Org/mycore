package org.mycore.frontend.iview2;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.ifs.MCRDirectoryXML;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRDirectoryXMLServlet extends MCRServlet {
    private static final long serialVersionUID = -506031354704994142L;

    private static Logger LOGGER = Logger.getLogger(MCRDirectoryXMLServlet.class.getName());

    public void doGetPost(MCRServletJob job) throws IOException {
        LOGGER.info(job.getRequest().getPathInfo());
        //    	String[] data = job.getRequest().getPathInfo().split("/");
        MCRDirectoryXML dirXML = MCRDirectoryXML.getInstance();
        Document doc = dirXML.getDirectory(job.getRequest().getPathInfo(), null);
        MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), doc);
    }

}
