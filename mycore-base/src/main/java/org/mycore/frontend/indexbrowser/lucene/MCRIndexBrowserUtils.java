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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;

/**
 * contains  utility functions for indexbrowser
 * 
 * @author Anja Schaar, Andreas Trappe, Matthias Eichner, Robert Stephan
 */
public class MCRIndexBrowserUtils{

    private static final long serialVersionUID = 4963472470316616461L;

    protected MCRIndexBrowserIncomingData incomingBrowserData;

    protected MCRIndexBrowserConfig config;
    
  

    /**
     * Creates a xml document with the results of the index browser.
     * @return 
     */
    public static Document createResultListDocument(MCRIndexBrowserIncomingData incomingBrowserData,
    		MCRIndexBrowserConfig config) {
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
    public static Document createEmptyDocument(MCRIndexBrowserIncomingData incomingBrowserData) {
        Element rootElement = MCRIndexBrowserXmlGenerator.buildPageElement(incomingBrowserData);
        MCRIndexBrowserXmlGenerator.buildResultsElement(rootElement, incomingBrowserData);
        return new Document(rootElement);
    }

    public static MCRIndexBrowserIncomingData getIncomingBrowserData(HttpServletRequest request) {
        String search = request.getParameter("search");
        String mode = getMode(request);
        String searchclass = request.getParameter("searchclass");
        String fromTo = request.getParameter("fromTo");
        String init = request.getParameter("init");

        return new MCRIndexBrowserIncomingData(search, mode, searchclass, fromTo, init);
    }

    private static String getMode(HttpServletRequest request) {
        if (request.getParameter("mode") != null && !request.getParameter("mode").trim().equals("")) {
            return request.getParameter("mode").toLowerCase().trim();
        } else
            return "prefix";
    }
}