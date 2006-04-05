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

package org.mycore.services.fieldquery;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRCache;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.parsers.bool.MCRCondition;

/**
 * This servlet executes queries and presents result pages.
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 */
public class MCRSearchServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

    /** Cached query results */
    private static MCRCache results = new MCRCache(50);

    /** Cached queries as XML, for re-use in editor form */
    private static MCRCache queries = new MCRCache(50);

    /** Cached queries as parsed MCRCondition, for output with results */
    private static MCRCache conds = new MCRCache(50);

    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        String mode = job.getRequest().getParameter("mode");
        if ("results".equals(mode))
            showResults(request, response);
        else if ("load".equals(mode))
            loadQuery(request, response);
        else
            doQuery(request, response);
    }

    /**
     * Returns a query that was previously submitted, 
     * to reload it into the editor search mask. 
     * Usage: MCRSearchServlet?mode=load&id=XXXXX
     */
    private void loadQuery(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        Document query = (Document) (queries.get(id));

        // Send query XML to editor
        request.setAttribute("MCRLayoutServlet.Input.JDOM", query);
        request.setAttribute("XSL.Style", "xml");
        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(request, response);
    }

    /**
     * Shows a results page.
     * Usage: MCRSearchServlet?mode=results&numPerPage=10&page=1
     */
    private void showResults(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get cached results
        String id = request.getParameter("id");
        MCRResults result = (MCRResults) (results.get(id));
        Document query = (Document) (queries.get(id));

        // Effective number of hits per page
        String snpp = request.getParameter("numPerPage");
        if (snpp == null)
            snpp = "0";
        int npp = Integer.parseInt(snpp);
        if (npp > result.getNumHits())
            npp = 0;

        // Total number of pages
        int numHits = Math.max(1, result.getNumHits());
        int numPages = 1;
        if (npp > 0)
            numPages = (int) (Math.ceil((double) numHits / (double) npp));

        // Effective current page number
        String spage = request.getParameter("page");
        if (spage == null)
            spage = "1";
        int page = Integer.parseInt(spage);
        if (npp == 0)
            page = 1;
        else if (page > numPages)
            page = numPages;

        // Number of first and last hit to be shown
        int first = (page - 1) * npp;
        int last = result.getNumHits() - 1;
        if (npp > 0)
            last = Math.min(result.getNumHits(), first + npp) - 1;

        // Build result hits as XML document
        Element xml = result.buildXML(first, last);
        
        // Add additional data for output
        xml.setAttribute("numPerPage", String.valueOf(npp));
        xml.setAttribute("numPages", String.valueOf(numPages));
        xml.setAttribute("page", String.valueOf(page));
        
        // The URL of the search mask that was used
        xml.setAttribute("mask", query.getRootElement().getAttributeValue("mask"));

        // The query condition, to show together with the results
        MCRCondition cond = (MCRCondition) (conds.get(id));
        xml.addContent(new Element("condition").setAttribute("format", "xml").setText(cond.toString()));
        xml.addContent(new Element("condition").setAttribute("format", "text").addContent(cond.toXML()));

        // Send output to LayoutServlet
        request.setAttribute("MCRLayoutServlet.Input.JDOM", new Document(xml));
        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(request, response);
    }

    /**
     * Executes a query that comes from editor search mask, and redirects the
     * browser to the first results page
     */
    private void doQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Document input = ((MCREditorSubmission) (request.getAttribute("MCREditorSubmission"))).getXML();
        Object clonedQuery = input.clone(); // Keep for later re-use

        // Show incoming query document
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(input));
        }

        org.jdom.Element root = input.getRootElement();

        // Rename condition elements from search mask: condition1 -> condition
        List ch = root.getChild("conditions").getChild("boolean").getChildren();
        for (int i = 0; i < ch.size(); i++) {
            Element condition = (Element) (ch.get(i));
            condition.setName("condition");
        }

        // Find condition fields without values
        Iterator it = root.getDescendants(new ElementFilter("condition"));
        Vector help = new Vector();
        while (it.hasNext()) {
            Element condition = (Element) it.next();
            if (condition.getAttribute("value") == null) {
                help.add(condition);
            }
        }

        // Remove found conditions without values
        for (int i = help.size() - 1; i >= 0; i--)
            ((Element) (help.get(i))).detach();

        // Cache parsed query for later output together with results
        Element condElem = (Element) (root.getChild("conditions").getChildren().get(0));
        MCRCondition cond = new MCRQueryParser().parse(condElem);

        // Execute query
        long start = System.currentTimeMillis();
        MCRResults result = MCRQueryManager.search(input);
        long qtime = System.currentTimeMillis() - start;
        LOGGER.debug("MCRSearchServlet total query time: " + qtime);

        String npp = root.getAttributeValue("numPerPage", "0");

        // Store query and results in cache
        results.put(result.getID(), result);
        queries.put(result.getID(), clonedQuery);
        conds.put(result.getID(), cond);

        // Redirect browser to first results page
        String url = "MCRSearchServlet?mode=results&id=" + result.getID() + "&numPerPage=" + npp;
        response.sendRedirect(response.encodeRedirectURL(url));
    }
}
