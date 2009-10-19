/*
 * 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

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
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;

/**
 * Executes queries and presents result pages. Queries can be submitted in four
 * ways, examples below:
 * 
 * 1. MCRSearchServlet?search=foo
 *   Searches for "foo" in default field, using default operator
 * 2. MCRSearchServlet?query=title contains Regenbogen
 *   Search using query condition given as text
 * 3. MCRSearchServlet?title=Regenbogen&title.operator=contains&author.sortField.1=ascending
 *   Search using name=value pairs
 * 4. MCRSearchServlet invocation from a search mask using editor XML input 
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 */
public class MCRSearchServlet extends MCRServlet 
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

    /** Default search field */
    private String defaultSearchField;

    public void init() throws ServletException {
        super.init();
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.SearchServlet.";
        defaultSearchField = config.getString(prefix + "DefaultSearchField", "allMeta");
    }

    /**
     * Search in default search field specified by MCR.SearchServlet.DefaultSearchField
     */
    private MCRQuery buildDefaultQuery(String search) {
        MCRFieldDef field = MCRFieldDef.getDef(defaultSearchField);
        String operator = MCRFieldType.getDefaultOperator(field.getDataType());
        MCRCondition condition = new MCRQueryCondition(field, operator, search);
        return new MCRQuery(MCRQueryParser.normalizeCondition(condition));
    }

    /**
     * Search using complex query expression given as text string
     */
    private MCRQuery buildComplexQuery(String query) {
        return new MCRQuery(new MCRQueryParser().parse(query));
    }

    /**
     * Search using name=value pairs from HTTP request
     */
    private MCRQuery buildNameValueQuery(HttpServletRequest req) {
        MCRAndCondition condition = new MCRAndCondition();

        for (Enumeration names = req.getParameterNames(); names.hasMoreElements();) {
            String name = (String) (names.nextElement());
            if (name.endsWith(".operator"))
                continue;
            if (name.contains(".sortField"))
                continue;
            if (name.equals("maxResults"))
                continue;
            if (name.equals("numPerPage"))
                continue;
            if (name.equals("mask"))
                continue;

            MCRFieldDef field = MCRFieldDef.getDef(name);
            String defaultOperator = MCRFieldType.getDefaultOperator(field.getDataType());
            String operator = getReqParameter(req, name + ".operator", defaultOperator);

            MCRSetCondition parent = condition;
            String[] values = req.getParameterValues(name);
            if (values.length > 1) {
                // Multiple fields with same name, combine with OR
                parent = new MCROrCondition();
                condition.addChild(parent);
            }
            for (String value : values)
                parent.addChild(new MCRQueryCondition(field, operator, value));
        }

        return new MCRQuery(MCRQueryParser.normalizeCondition(condition));
    }

    /**
     * Rename elements conditionN to condition. 
     * Transform condition with multiple child values to OR-condition.
     */
    private void renameElements(Element element) {
        if (element.getName().startsWith("condition")) {
            element.setName("condition");

            List<Element> values = element.getChildren("value");
            if ((values != null) && (values.size() > 0)) {
                String field = element.getAttributeValue("field");
                String operator = element.getAttributeValue("operator");
                element.removeAttribute("field");
                element.setAttribute("operator", "or");
                element.setName("boolean");
                for (Element value : values) {
                    value.setName("condition");
                    value.setAttribute("field", field);
                    value.setAttribute("operator", operator);
                    value.setAttribute("value", value.getText());
                    value.removeContent();
                }
            }
        } else if (element.getName().startsWith("boolean")) {
            element.setName("boolean");
            for (Object child : element.getChildren())
                if (child instanceof Element)
                    renameElements((Element) child);
        }
    }
  
    /**
     * Build MCRQuery from editor XML input
     */
    private MCRQuery buildFormQuery(Element root) {
        Element conditions = root.getChild("conditions");

        if (conditions.getAttributeValue("format", "xml").equals("xml")) {
            Element condition = (Element) (conditions.getChildren().get(0));
            renameElements(condition);

            // Remove conditions without values
            List<Element> empty = new ArrayList<Element>();
            for (Iterator it = conditions.getDescendants(new ElementFilter("condition")); it.hasNext();) {
                Element cond = (Element) it.next();
                if (cond.getAttribute("value") == null)
                    empty.add(cond);
            }

            // Remove empty sort conditions
            Element sortBy = root.getChild("sortBy");
            if (sortBy != null) {
                for (Element field : (List<Element>) (sortBy.getChildren("field")))
                    if (field.getAttributeValue("name", "").length() == 0)
                        empty.add(field);
            }

            for (int i = empty.size() - 1; i >= 0; i--)
                empty.get(i).detach();

            if ((sortBy != null) && (sortBy.getChildren().size() == 0))
                sortBy.detach();
        }

        return MCRQuery.parseXML(root.getDocument());
    }
    
    private String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if ((value == null) || (value.trim().length() == 0))
            return defaultValue;
        else
            return value.trim();
    }
  
    private Document setQueryOptions(MCRQuery query, HttpServletRequest req) {
        String maxResults = getReqParameter(req, "maxResults", "0");
        query.setMaxResults(Integer.parseInt(maxResults));

        List<String> sortFields = new ArrayList<String>();
        for (Enumeration names = req.getParameterNames(); names.hasMoreElements();) {
            String name = (String) (names.nextElement());
            if (name.contains(".sortField"))
                sortFields.add(name);
        }

        if (sortFields.size() > 0) {
            Collections.sort(sortFields, new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    String s0 = (String) arg0;
                    s0 = s0.substring(s0.indexOf(".sortField"));
                    String s1 = (String) arg1;
                    s1 = s1.substring(s1.indexOf(".sortField"));
                    return s0.compareTo(s1);
                }
            });
            List<MCRSortBy> sortBy = new ArrayList<MCRSortBy>();
            for (String name : sortFields) {
                String sOrder = getReqParameter(req, name, "ascending");
                boolean order = "ascending".equals(sOrder) ? MCRSortBy.ASCENDING : MCRSortBy.DESCENDING;
                name = name.substring(0, name.indexOf(".sortField"));
                sortBy.add(new MCRSortBy(MCRFieldDef.getDef(name), order));
            }
            query.setSortBy(sortBy);
        }

        Document xml = query.buildXML();
        xml.getRootElement().setAttribute("numPerPage", getReqParameter(req, "numPerPage", "0"));
        xml.getRootElement().setAttribute("mask", getReqParameter(req, "mask", "-"));
        return xml;
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
    
    /**
     * Returns a query that was previously submitted, to reload it into the
     * editor search mask. Usage: MCRSearchServlet?mode=load&id=XXXXX
     */
    protected void loadQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter( "id" );
        MCRCachedQueryData qd = MCRCachedQueryData.getData( id );
        if( qd == null )
        {
          throw new MCRException( "Result list is not in cache any more, please re-run query" );
        }
        getLayoutService().sendXML(request, response, qd.getInput() );   
    }

    /**
     * Shows a results page. Usage:
     * MCRSearchServlet?mode=results&numPerPage=10&page=1
     */
    protected void showResults(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get cached results
        String id = request.getParameter("id");
        MCRCachedQueryData qd = MCRCachedQueryData.getData(id);
        if (qd == null) {
            // Try to re-run original query, if in request parameter
            doQuery(request, response);
            return;
        }
        
        MCRResults results = qd.getResults();

        // Number of hits per page
        String snpp = request.getParameter("numPerPage");
        if (snpp == null)
            snpp = "0";
        int npp = Integer.parseInt(snpp);
        if (npp > results.getNumHits())
            npp = 0;

        // Current page number
        String spage = request.getParameter("page");
        if (spage == null)
            spage = "1";
        int page = Integer.parseInt(spage);
        
        // Total number of pages
        int numHits = Math.max(1, results.getNumHits());
        int numPages = 1;
        if (npp > 0)
            numPages = (int) (Math.ceil((double) numHits / (double) npp));

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
        qd.setPage(page);
        qd.setNumPerPage(npp);

        // The URL of the search mask that was used
        xml.setAttribute("mask", qd.getInput().getRootElement().getAttributeValue("mask"));

        // The query condition, to show together with the results
        MCRCondition condition = qd.getQuery().getCondition();
        xml.addContent(new Element("condition").setAttribute("format", "text").setText(condition.toString()));
        xml.addContent(new Element("condition").setAttribute("format", "xml").addContent(condition.toXML()));

        // Send output to LayoutServlet
        sendToLayout(request, response, new Document(xml));
    }

    /**
     * Executes a query that comes from editor search mask, and redirects the
     * browser to the first results page
     */
    protected void doQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {

        MCREditorSubmission sub = (MCREditorSubmission) (request.getAttribute("MCREditorSubmission"));
        String searchString = getReqParameter(request, "search", null);
        String queryString = getReqParameter(request, "query", null);

        Document input;
        MCRQuery query;

        if (sub != null) {
            input = (Document) (sub.getXML().clone());
            query = buildFormQuery(sub.getXML().getRootElement());
        } else {
            if (queryString != null)
                query = buildComplexQuery(queryString);
            else if (searchString != null)
                query = buildDefaultQuery(searchString);
            else
                query = buildNameValueQuery(request);

            input = setQueryOptions(query, request);
        }

        // Show incoming query document
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(input));
        }

        MCRCachedQueryData qd = MCRCachedQueryData.cache(query, input);
        sendRedirect(request,response,qd,input);
    }

    /**      
     * Redirect browser to results page     
     *      
     * @author A.Schaar   
     * @author Frank Lützenkirchen
     *   
     * @see its overwritten in jspdocportal     
     */     
    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, MCRCachedQueryData qd, Document query) throws IOException {
        
        // Redirect browser to first results page   
        StringBuffer sb = new StringBuffer();   
        sb.append("MCRSearchServlet?mode=results&id=").append(qd.getResults().getID());

        String numPerPage = query.getRootElement().getAttributeValue("numPerPage", "0");
        sb.append("&numPerPage=").append(numPerPage);
        
        String mask = query.getRootElement().getAttributeValue("mask", "-");
        sb.append("&mask=").append(mask);

        String queryString = qd.getQuery().getCondition().toString();
        sb.append("&query=").append(queryString);

        int maxResults = qd.getQuery().getMaxResults();
        sb.append("&maxResults=").append(maxResults);

        List<MCRSortBy> list = qd.getQuery().getSortBy();
        if( list != null ) for( int i = 0; i < list.size(); i++ )
        {
          MCRSortBy sortBy = list.get(i);
          sb.append( "&" ).append( sortBy.getField().getName() );
          sb.append( ".sortField." ).append( i + 1 );
          sb.append( "=" ).append( sortBy.getSortOrder() == MCRSortBy.ASCENDING ? "ascending" : "descending" );
        }
        
        String style = req.getParameter("XSL.Style");   
        if ((style != null) && (style.trim().length() != 0)){   
            sb.append("&XSL.Style=").append(style);     
        }
        
        res.sendRedirect(res.encodeRedirectURL(sb.toString()));     
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
}
