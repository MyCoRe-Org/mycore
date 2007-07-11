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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
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

    protected static final Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

    /** Default search field */
    private String defaultSearchField;

    public void init() throws ServletException {
        super.init();
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.SearchServlet.";
        defaultSearchField = config.getString(prefix + "DefaultSearchField", "allMeta");
    }

    public void doGetPost(MCRServletJob job) throws IOException {
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
    
    protected MCRCachedQueryData getQueryData( HttpServletRequest request )
    {
      String id = request.getParameter( "id" );
      MCRCachedQueryData qd = MCRCachedQueryData.getData( id );
      if( qd == null )
      {
        throw new MCRException( "Result list is not in cache any more, please re-run query" );
      }
      return qd;
    }

    /**
     * Returns a query that was previously submitted, to reload it into the
     * editor search mask. Usage: MCRSearchServlet?mode=load&id=XXXXX
     */
    protected void loadQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        getLayoutService().sendXML(request, response, getQueryData(request).getQuery() );   
    }

    /**
     * Shows a results page. Usage:
     * MCRSearchServlet?mode=results&numPerPage=10&page=1
     */
    protected void showResults(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get cached results
        MCRCachedQueryData qd = getQueryData(request);
        MCRResults results = qd.getResults();

        // Effective number of hits per page
        String snpp = request.getParameter("numPerPage");
        if (snpp == null)
            snpp = "0";
        int npp = Integer.parseInt(snpp);
        if (npp > results.getNumHits())
            npp = 0;

        // Total number of pages
        int numHits = Math.max(1, results.getNumHits());
        int numPages = 1;
        if (npp > 0)
            numPages = (int) (Math.ceil((double) numHits / (double) npp));

        // Effective current page number
        String spage = request.getParameter("page");
        if (spage == null)
            spage = "1";
        int page = 0;
        try
        {
          page = Integer.parseInt(spage);
        } catch (NumberFormatException e)
        {
        }
        if (npp == 0)
            page = 1;
        else if (page > numPages)
          page = numPages;
        else if (page < 1)
          page = 1;

        // Number of first and last hit to be shown
        int first = (page - 1) * npp;
        int last = results.getNumHits() - 1;
        if (npp > 0)
            last = Math.min(results.getNumHits(), first + npp) - 1;

        // Build result hits as XML document
        Element xml = results.buildXML(first, last);

        // Add additional data for output
        xml.setAttribute("numPerPage", String.valueOf(npp));
        xml.setAttribute("numPages", String.valueOf(numPages));
        xml.setAttribute("page", String.valueOf(page));
        // save some parameters
        qd.setPage( page );
        qd.setNumPerPage( npp );

        // The URL of the search mask that was used
        xml.setAttribute("mask", qd.getQuery().getRootElement().getAttributeValue("mask"));

        // The query condition, to show together with the results
        MCRCondition condition = qd.getCondition();
        xml.addContent(new Element("condition").setAttribute("format", "text").setText(condition.toString()));
        xml.addContent(new Element("condition").setAttribute("format", "xml").addContent(condition.toXML()));

        // Send output to LayoutServlet
        sendToLayout(request, response, new Document(xml));        

    }

    private String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if ((value == null) || (value.trim().length() == 0))
            return defaultValue;
        else
            return value.trim();
    }

    /**
     * Executes a query that comes from editor search mask, and redirects the
     * browser to the first results page
     */
    protected void doQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MCREditorSubmission sub = (MCREditorSubmission) (request.getAttribute("MCREditorSubmission"));
        Document input = null;
        if (sub != null) // query comes from editor search mask
        {
            input = sub.getXML();
        } else // query comes from HTTP request parameters
        {
            Element query = new Element("query");
            query.setAttribute("mask", getReqParameter(request, "mask", "-"));
            query.setAttribute("maxResults", getReqParameter(request, "maxResults", "0"));
            query.setAttribute("numPerPage", getReqParameter(request, "numPerPage", "0"));
            input = new Document(query);

            
            Element sortBy = new Element("sortBy");
            query.addContent(sortBy);
            
            Enumeration sortNames = request.getParameterNames();
            while (sortNames.hasMoreElements()) {
                String name = (String) (sortNames.nextElement());
                if (name.endsWith(".sortField"))
                {
                  String sortField = name.substring(0, name.length()-10);
                  Element field = new Element("field");
                  field.setAttribute("name", sortField);
                  field.setAttribute("order", getReqParameter(request, name, "ascending"));
                  sortBy.addContent(field);
                }
            }
            
            
            Element conditions = new Element("conditions");
            query.addContent(conditions);

            if (request.getParameter("search") != null) {
                // Search in default field with default operator

                String defaultOperator = MCRFieldType.getDefaultOperator(MCRFieldDef.getDef(defaultSearchField).getDataType()); 
                Element cond = new Element("condition");
                cond.setAttribute("field", defaultSearchField);
                cond.setAttribute("operator", defaultOperator );
                cond.setAttribute("value", getReqParameter(request, "search", null));

                Element b = new Element("boolean");
                b.setAttribute("operator", "and");
                b.addContent(cond);

                conditions.setAttribute("format", "xml");
                conditions.addContent(b);
            } else if (request.getParameter("query") != null) {
                // Search for a complex query expression

                conditions.setAttribute("format", "text");
                conditions.addContent(request.getParameter("query"));
            } else {
                // Search for name-operator-value conditions given as request
                // parameters

                conditions.setAttribute("format", "xml");
                Element b = new Element("boolean");
                b.setAttribute("operator", "and");
                conditions.addContent(b);

                Enumeration names = request.getParameterNames();
                while (names.hasMoreElements()) {
                    String name = (String) (names.nextElement());
                    if (name.endsWith(".operator") || name.endsWith(".sortField"))
                        continue;
                    if (" maxResults numPerPage mask ".indexOf(" " + name + " ") >= 0)
                        continue;

                    String operator = request.getParameter(name + ".operator");
                    if (operator == null)
                        operator = MCRFieldType.getDefaultOperator(MCRFieldDef.getDef(name).getDataType()); 

                    Element parent = b;

                    String[] values = request.getParameterValues(name);
                    if (values.length > 1) // Multiple fields with same name,
                    // combine with OR
                    {
                        parent = new Element("boolean");
                        parent.setAttribute("operator", "or");
                        b.addContent(parent);
                    }
                    for (int i = 0; i < values.length; i++) {
                        Element cond = new Element("condition");
                        cond.setAttribute("field", name);
                        cond.setAttribute("operator", operator);
                        cond.setAttribute("value", values[i].trim());
                        parent.addContent(cond);
                    }
                }
            }
        }

        Document clonedQuery = (Document)(input.clone()); // Keep for later re-use

        // Show incoming query document
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(input));
        }

        org.jdom.Element root = input.getRootElement();
        MCRCondition cond = null;

        if (root.getChild("conditions").getAttributeValue("format", "xml").equals("xml")) {
            // Query is in XML format

            // Rename condition elements from search mask:
            // condition1 -> condition
            Iterator it = root.getDescendants(new ElementFilter());
            while (it.hasNext()) {
                Element elem = (Element) it.next();
                if ((!elem.getName().equals("conditions")) && elem.getName().startsWith("condition"))
                    elem.setName("condition");
            }

            // Find condition fields without values
            it = root.getDescendants(new ElementFilter("condition"));
            Vector<Element> help = new Vector<Element>();
            while (it.hasNext()) {
                Element condition = (Element) it.next();
                if (condition.getAttribute("value") == null) {
                    help.add(condition);
                }
            }

            // Remove found conditions without values
            for (int i = help.size() - 1; i >= 0; i--)
                help.get(i).detach();

            Element condElem = (Element) (root.getChild("conditions").getChildren().get(0));
            cond = new MCRQueryParser().parse(condElem);
        } else {
            // Query is in String format
            String query = root.getChild("conditions").getTextTrim();
            cond = new MCRQueryParser().parse(query);
        }

        Element sortBy = input.getRootElement().getChild("sortBy");
        if (sortBy != null) {
            // Remove empty sort fields from input
            List fields = sortBy.getChildren();
            for (int i = 0; i < fields.size(); i++) {
                Element field = (Element) (fields.get(i));
                if (field.getAttributeValue("name", "").length() == 0) {
                    i--;
                    field.detach();
                }
            }

            // Remove empty sort criteria list
            if (sortBy.getChildren().size() == 0)
                sortBy.detach();
        }

        // Execute query
        MCRResults result = MCRQueryManager.search(MCRQuery.parseXML(input));

        String npp = root.getAttributeValue("numPerPage", "0");

        // Store query and results in cache
        new MCRCachedQueryData( result, clonedQuery, cond );

        // Redirect browser to first results page
        sendRedirect(request, response, result.getID(), npp);
    }

   /**
     * Forwards the document to the output
     * 
     * @author A.Schaar
     * @see its overwritten in jspdocportal
     */
    protected void sendToLayout(HttpServletRequest req, HttpServletResponse res, Document jdom) throws IOException {
        getLayoutService().doLayout(req, res, jdom);
    }

    /**
     * Redirect browser to results page
     * 
     * @author A.Schaar
     * @see its overwritten in jspdocportal
     */
    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, String id, String numPerPage) throws IOException {
        // Redirect browser to first results page
        StringBuffer sb = new StringBuffer();
        sb.append("MCRSearchServlet?mode=results&id=").append(id).append("&numPerPage=").append(numPerPage);
        String style = req.getParameter("XSL.Style");
        if ((style != null) && (style.trim().length() != 0)){
            sb.append("&XSL.Style=").append(style);
        }
        res.sendRedirect(res.encodeRedirectURL(sb.toString()));
    }
}
