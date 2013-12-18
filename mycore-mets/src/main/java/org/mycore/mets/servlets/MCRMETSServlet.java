/*
 * $Id$ $Revision:
 * 20489 $ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.mets.servlets;

import java.text.MessageFormat;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.model.MCRMETSGenerator;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMETSServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRMETSServlet.class);

    private boolean useExpire;

    private static int CACHE_TIME;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        LOGGER.info(request.getPathInfo());

        String derivate = getOwnerID(request.getPathInfo());
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);

        if (dir == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, MessageFormat.format("Derivate {0} does not exist.", derivate));
            return;
        }

        request.setAttribute("XSL.derivateID", derivate);
        request.setAttribute("XSL.objectID", MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());
        response.setContentType("text/xml");

        long lastModified = dir.getLastModified().getTimeInMillis();

        writeCacheHeaders(response, CACHE_TIME, lastModified, useExpire);
        long start = System.currentTimeMillis();
        MCRContent metsContent = getMetsSource(job, useExistingMets(request), derivate);
        MCRLayoutService.instance().doLayout(request, response, metsContent);
        LOGGER.info("Generation of code by " + this.getClass().getSimpleName() + " took " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Returns the mets document wrapped in a {@link MCRContent} object.
     * 
     * @param job
     * @param useExistingMets
     * @return
     * @throws Exception
     */
    static MCRContent getMetsSource(MCRServletJob job, boolean useExistingMets, String derivate) throws Exception {
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);

        MCRFilesystemNode metsFile = dir.getChildByPath("mets.xml");

        try {
            job.getRequest().setAttribute("XSL.derivateID", derivate);
            job.getRequest().setAttribute("XSL.objectID", MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());
        } catch (Exception x) {
            LOGGER.warn("Unable to set \"XSL.objectID\" attribute to current request", x);
        }

        if (metsFile != null && useExistingMets) {
            MCRContent content = ((MCRFile) metsFile).getContent();
            content.setDocType("mets");
            return content;
        } else {
            HashSet<MCRFilesystemNode> ignoreNodes = new HashSet<MCRFilesystemNode>();
            if (metsFile != null)
                ignoreNodes.add(metsFile);
            Document mets = MCRMETSGenerator.getGenerator().getMETS(dir, ignoreNodes).asDocument();

            return new MCRJDOMContent(mets);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        String cacheParam = getInitParameter("cacheTime");
        /* default is one day */
        CACHE_TIME = cacheParam != null ? Integer.parseInt(cacheParam) : (60 * 60 * 24);
        useExpire = MCRConfiguration.instance().getBoolean("MCR.Component.MetsMods.Servlet.UseExpire", true);
    }

    private boolean useExistingMets(HttpServletRequest request) {
        String useExistingMetsParam = request.getParameter("useExistingMets");
        if (useExistingMetsParam == null)
            return true;
        return Boolean.valueOf(useExistingMetsParam);
    }

    protected static String getOwnerID(String pathInfo) {
        StringBuilder ownerID = new StringBuilder(pathInfo.length());
        boolean running = true;
        for (int i = (pathInfo.charAt(0) == '/') ? 1 : 0; (i < pathInfo.length() && running); i++) {
            switch (pathInfo.charAt(i)) {
            case '/':
                running = false;
                break;
            default:
                ownerID.append(pathInfo.charAt(i));
                break;
            }
        }
        return ownerID.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.frontend.servlets.MCRServlet#getLastModified(javax.servlet
     * .http.HttpServletRequest)
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        String ownerID = getOwnerID(request.getPathInfo());
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            session.beginTransaction();
            MCRDirectory rootNode = MCRDirectory.getRootDirectory(ownerID);
            if (rootNode != null)
                return rootNode.getLastModified().getTimeInMillis();
            return -1l;
        } finally {
            session.commitTransaction();
            MCRSessionMgr.releaseCurrentSession();
            session.close(); // just created session for db transaction
        }
    }

}
