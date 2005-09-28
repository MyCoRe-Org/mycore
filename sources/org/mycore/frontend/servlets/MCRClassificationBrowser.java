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

import java.io.File;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications.MCRClassificationBrowserData;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification, provides a link to show the documents in a
 * category and shows to number of documents per category.
 * 
 * @author Anja Schaar
 * 
 * @see org.mycore.frontend.servlets.MCRClassificationBrowseData
 */
public class MCRClassificationBrowser extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRClassificationBrowser.class);

    private static String lang = null;

    public void doGetPost(MCRServletJob job) throws ServletException, Exception {
        /*
         * default classification
         */
        LOGGER.debug(this.getClass() + " Start");

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        lang = mcrSession.getCurrentLanguage();

        /*
         * the urn with information abaut classification-propertie and category
         */
        String uri = new String(job.getRequest().getPathInfo());
        LOGGER.debug(this.getClass() + " Path = " + uri);

        try {
            mcrSession.BData = new MCRClassificationBrowserData(uri);
        } catch (MCRConfigurationException cErr) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cErr.getMessage(), cErr, false);
        }

        Document jdomFile = getEmbeddingPage(mcrSession.BData.getPageName());
        Document jdom = mcrSession.BData.createXmlTree(lang);

        jdom = mcrSession.BData.loadTreeIntoSite(jdomFile, jdom);
        doLayout(job, mcrSession.BData.getXslStyle(), jdom); // use the

        // stylesheet-postfix
        // from properties
    }

    private org.jdom.Document getEmbeddingPage(String coverPage) throws Exception {
        String path = getServletContext().getRealPath(coverPage);
        File file = new File(path);

        if (!file.exists()) {
            LOGGER.debug(this.getClass() + " did not find the CoverPage " + path);

            return null;
        }

        SAXBuilder sxbuild = new SAXBuilder();
        LOGGER.debug(this.getClass() + " found CoverPage " + path);

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
        String styleSheet = styleBase + "-" + lang;

        job.getRequest().getSession().setAttribute("mycore.language", lang);
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdomDoc);

        if (getProperty(job.getRequest(), "XSL.Style") == null) {
            job.getRequest().setAttribute("XSL.Style", styleSheet);
        }

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
