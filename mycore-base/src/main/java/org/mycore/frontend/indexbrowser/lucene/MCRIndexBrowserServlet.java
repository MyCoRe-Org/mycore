/*
 * 
 * $Revision: 15105 $ $Date: 2009-04-23 11:23:28 +0200 (Do, 23. Apr 2009) $
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

package org.mycore.frontend.indexbrowser.lucene;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;

/**
 * Servlet to create an xml document which is parsed by xsl
 * to display the index browser results of the current request.
 * 
 * @author Anja Schaar, Andreas Trappe, Matthias Eichner
 */
public class MCRIndexBrowserServlet extends MCRServlet {

    /** The logger for this class*/
    private static final Logger LOGGER = Logger.getLogger(MCRIndexBrowserServlet.class);

    private static final long serialVersionUID = 4963472470316616461L;

    protected MCRIndexBrowserIncomingData incomingBrowserData;

    protected MCRIndexBrowserConfig config;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        @SuppressWarnings("unchecked")
        Enumeration<String> ee = job.getRequest().getParameterNames();
        while (ee.hasMoreElements()) {
            String param = ee.nextElement();
            LOGGER.info("PARAM: " + param + " VALUE: " + job.getRequest().getParameter(param));
        }

        incomingBrowserData = getIncomingBrowserData(job.getRequest());
        config = new MCRIndexBrowserConfig(incomingBrowserData.getSearchclass());

        Document pageContent = null;
        // if init is true, then create an empty document, otherwise create
        // the result list
        if (!incomingBrowserData.isInit()) {
            pageContent = createResultListDocument();
        } else {
            pageContent = createEmptyDocument();
        }

        if (getProperty(job.getRequest(), "XSL.Style") == null) {
            job.getRequest().setAttribute("XSL.Style", job.getRequest().getParameter("searchclass"));
        }
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(pageContent));
    }

    /**
     * Creates a xml document with the results of the index browser.
     * @return a new xml document with the result list
     */
    protected Document createResultListDocument() throws IOException, JDOMException, SAXException {
        return MCRIndexBrowserUtils.createResultListDocument(incomingBrowserData, config);
    }

    /**
     * Creates an empty xml index browser document.
     * @return a new empty document
     */
    protected Document createEmptyDocument() {
        return MCRIndexBrowserUtils.createEmptyDocument(incomingBrowserData);
    }

    protected MCRIndexBrowserIncomingData getIncomingBrowserData(HttpServletRequest request) {
        return MCRIndexBrowserUtils.getIncomingBrowserData(request);
    }
}