package org.mycore.iview2.frontend;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.ifs.MCRDirectoryXML;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Forwards {@link MCRDirectoryXML} to {@link MCRLayoutService}
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDirectoryXMLServlet extends MCRServlet {
    private static final long serialVersionUID = -506031354704994142L;

    private static Logger LOGGER = Logger.getLogger(MCRDirectoryXMLServlet.class.getName());

    /**
     * Reads path from {@link HttpServletRequest#getPathInfo()} and forwards {@link MCRDirectoryXML} to {@link MCRLayoutService}.
     */
    @Override
    public void doGetPost(MCRServletJob job) throws IOException {
        LOGGER.info(job.getRequest().getPathInfo());
        //    	String[] data = job.getRequest().getPathInfo().split("/");
        MCRDirectoryXML dirXML = MCRDirectoryXML.getInstance();
        Document doc = dirXML.getDirectory(job.getRequest().getPathInfo(), false);
        MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), doc);
    }

}
