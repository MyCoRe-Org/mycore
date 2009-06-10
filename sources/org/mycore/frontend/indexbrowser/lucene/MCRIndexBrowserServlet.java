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

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Servlet to create an xml document which is parsed by xsl
 * to display the index browser results of the current request.
 * 
 * @author Anja Schaar, Andreas Trappe, Matthias Eichner
 */
public class MCRIndexBrowserServlet extends MCRServlet {

    private static final long serialVersionUID = 4963472470316616461L;

    protected MCRIndexBrowserIncomingData incomingBrowserData;

    protected MCRIndexBrowserConfig config;
    
    protected void doGetPost(MCRServletJob job) throws Exception {
        @SuppressWarnings("unchecked")
        Enumeration<String> ee = job.getRequest().getParameterNames();
        while (ee.hasMoreElements()) {
            String param = ee.nextElement();
            System.out.println("PARAM: " + param + " VALUE: " + job.getRequest().getParameter(param));
        }

        incomingBrowserData = getIncomingBrowserData(job.getRequest());
        config = new MCRIndexBrowserConfig(incomingBrowserData.getSearchclass());

        Document pageContent = null;
        // if init is true, then create an empty document, otherwise create
        // the result list
        if(!incomingBrowserData.isInit()) {
            pageContent = createResultListDocument();
        } else {
            pageContent = createEmptyDocument();
        }

        if (getProperty(job.getRequest(), "XSL.Style") == null) {
            job.getRequest().setAttribute("XSL.Style", job.getRequest().getParameter("searchclass"));
        }
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), pageContent);
    }

    /**
     * Creates a xml document with the results of the index browser.
     * @return 
     */
    protected Document createResultListDocument() {
        List<MCRIndexBrowserEntry> resultList = null;
        String index = config.getIndex();
        if(MCRIndexBrowserCache.isCached(index, incomingBrowserData)) {
            resultList = MCRIndexBrowserCache.getFromCache(index, incomingBrowserData);
        } else {
            MCRIndexBrowserSearcher searcher = new MCRIndexBrowserSearcher(incomingBrowserData, config);
            resultList = searcher.doSearch();
            MCRIndexBrowserCache.addToCache(incomingBrowserData, index, resultList);
        }
        MCRIndexBrowserXmlGenerator xmlGen = new MCRIndexBrowserXmlGenerator(resultList, incomingBrowserData, config);
        return xmlGen.getXMLContent();
    }

    /**
     * Creates an empty xml index browser document.
     * @return a new empty document
     */
    protected Document createEmptyDocument() {
        Element rootElement = MCRIndexBrowserXmlGenerator.buildPageElement(incomingBrowserData);
        MCRIndexBrowserXmlGenerator.buildResultsElement(rootElement, incomingBrowserData);
        return new Document(rootElement);
    }

    protected MCRIndexBrowserIncomingData getIncomingBrowserData(HttpServletRequest request) {
        String search = request.getParameter("search");
        String mode = getMode(request);
        String searchclass = request.getParameter("searchclass");
        String fromTo = request.getParameter("fromTo");
        String init = request.getParameter("init");

        return new MCRIndexBrowserIncomingData(search, mode, searchclass, fromTo, init);
    }

    private String getMode(HttpServletRequest request) {
        if (request.getParameter("mode") != null && !request.getParameter("mode").trim().equals("")) {
            return request.getParameter("mode").toLowerCase().trim();
        } else
            return "prefix";
    }
}