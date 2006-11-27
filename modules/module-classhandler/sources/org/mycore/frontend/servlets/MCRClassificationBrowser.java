/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.servlets;

import org.mycore.datamodel.classifications.MCRClassificationBrowserData;
import org.mycore.frontend.servlets.MCRServlet;
import org.apache.log4j.Logger;
import org.mycore.common.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import javax.servlet.*;
import javax.servlet.http.*;

import java.io.File;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification, provides a link to show the documents in a
 * category and shows to number of documents per category.
 * 
 * @author Anja Schaar
 * 
 * @see org.mycore.datamodel.classifications.MCRClassificationBrowserData
 */
public class MCRClassificationBrowser extends MCRServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRClassificationBrowser.class);

    private static String lang = null;

    public void doGetPost(MCRServletJob job) throws ServletException, Exception {
        /*
         * default classification
         */
        LOGGER.debug("Start brwosing in classifications");
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        lang = mcrSession.getCurrentLanguage();

        /*
         * the urn with information abaut classification-propertie and category
         */
        String uri = "";
        if (job.getRequest().getPathInfo() != null)
            uri = job.getRequest().getPathInfo();

        if ("/".equals(uri))
            uri = "";

        String mode = "";
        if (job.getRequest().getParameter("mode") != null)
            mode = job.getRequest().getParameter("mode");
        String actclid = "";
        if (job.getRequest().getParameter("clid") != null)
            actclid = job.getRequest().getParameter("clid");
        String actcateg = "";
        if (job.getRequest().getParameter("categid") != null)
            actcateg = job.getRequest().getParameter("categid");

        LOGGER.debug("Browsing Path = " + uri);
        LOGGER.debug("Browsing  Mode = " + mode);

        try {
            mcrSession.BData = new MCRClassificationBrowserData(uri, mode, actclid, actcateg);
        } catch (MCRConfigurationException cErr) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cErr.getMessage(), cErr, false);
        }

        Document jdomFile = getEmbeddingPage(mcrSession.BData.getPageName());
        Document jdom = null;
        if (mode.equalsIgnoreCase("edit") && (actclid.length() == 0 || uri.length() == 0)) {
            // alle Klasifikationen auflisten (auch die nicht eingebundenen)
            jdom = mcrSession.BData.createXmlTreeforAllClassifications();

        } else {
            jdom = mcrSession.BData.createXmlTree(lang);
        }
        jdom = mcrSession.BData.loadTreeIntoSite(jdomFile, jdom);
        doLayout(job, mcrSession.BData.getXslStyle(), jdom); // use the
        // stylesheet-postfix
        // from properties
    }

    private org.jdom.Document getEmbeddingPage(String coverPage) throws Exception {
        String path = getServletContext().getRealPath(coverPage);
        File file = new File(path);
        if (!file.exists()) {
            LOGGER.debug("Did not find the CoverPage " + path);
            return null;
        }
        SAXBuilder sxbuild = new SAXBuilder();
        LOGGER.debug("Found CoverPage " + path);
        return sxbuild.build(file);

    }

    /**
     * Gather information about the XML document to be shown and the
     * corresponding XSLT stylesheet and redirect the request to the
     * LayoutServlet
     * 
     * @param job
     *            The MCRServletJob instance
     * @param styleBase
     *            String value to select the correct XSL stylesheet
     * @param jdomDoc
     *            The XML representation to be presented by the LayoutServlet
     * @throws ServletException
     *             for errors from the servlet engine.
     * @throws Exception
     */
    protected void doLayout(MCRServletJob job, String styleBase, Document jdomDoc) throws ServletException, Exception {
        if (getProperty(job.getRequest(), "XSL.Style") == null) {
            LOGGER.info("Set XSL.Style to: " + styleBase);
            job.getRequest().setAttribute("XSL.Style", styleBase);
        }
        LAYOUT.doLayout(job.getRequest(),job.getResponse(),jdomDoc);
    }

}