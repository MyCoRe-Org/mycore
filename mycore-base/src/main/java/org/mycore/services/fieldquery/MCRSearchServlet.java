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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.xml.sax.SAXException;

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
 * @author A. Schaar
 * 
 */
public class MCRSearchServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

    /** Default search field */
    private String defaultSearchField;

    /** Maximum number of hits to display per page (numPerPage) */
    private int maxPerPage;

    private static HashSet<String> SEARCH_PARAMETER = new HashSet<>(Arrays.asList(new String[] { "search", "query",
            "maxResults", "numPerPage", "page", "mask", "mode", "redirect" }));;

    @Override
    public void init() throws ServletException {
        super.init();
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.SearchServlet.";
        defaultSearchField = config.getString(prefix + "DefaultSearchField", "allMeta");
        maxPerPage = config.getInt(prefix + "MaxPerPage", 0);
    }

    /**
     * Search in default search field specified by MCR.SearchServlet.DefaultSearchField
     */
    protected MCRQuery buildDefaultQuery(String search) {
        String[] fields = defaultSearchField.split(" *, *");
        MCROrCondition queryCondition = new MCROrCondition();

        for (String fDef : fields) {
            MCRFieldDef field = MCRFieldDef.getDef(fDef);
            String operator = MCRFieldType.getDefaultOperator(field.getDataType());
            MCRCondition condition = new MCRQueryCondition(fDef, operator, search);
            queryCondition.addChild(condition);
        }

        return new MCRQuery(MCRQueryParser.normalizeCondition(queryCondition));
    }

    /**
     * Search using complex query expression given as text string
     */
    protected MCRQuery buildComplexQuery(String query) {
        return new MCRQuery(new MCRQueryParser().parse(query));
    }

    /**
     * Search using name=value pairs from HTTP request
     */
    protected MCRQuery buildNameValueQuery(HttpServletRequest req) {
        MCRAndCondition condition = new MCRAndCondition();

        for (Enumeration names = req.getParameterNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            if (name.endsWith(".operator")) {
                continue;
            }
            if (name.contains(".sortField")) {
                continue;
            }
            if (SEARCH_PARAMETER.contains(name)) {
                continue;
            }
            if (name.startsWith("XSL.")) {
                continue;
            }

            String[] values = req.getParameterValues(name);
            MCRSetCondition parent = condition;

            if ((values.length > 1) || name.contains(",")) {
                // Multiple fields with same name, combine with OR
                parent = new MCROrCondition();
                condition.addChild(parent);
            }

            for (String fieldName : name.split(",")) {
                MCRFieldDef fieldDefinition = MCRFieldDef.getDef(fieldName);
                String defaultOperator = MCRFieldType.getDefaultOperator(fieldDefinition.getDataType());
                String operator = getReqParameter(req, fieldName + ".operator", defaultOperator);
                for (String value : values) {
                    parent.addChild(new MCRQueryCondition(fieldName, operator, value));
                }
            }
        }

        if (condition.getChildren().isEmpty()) {
            throw new MCRUsageException("Missing query condition");
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

            String field = new StringTokenizer(element.getAttributeValue("field"), " -,").nextToken();
            MCRFieldDef fd = MCRFieldDef.getDef(field);
            String defaultOperator = MCRFieldType.getDefaultOperator(fd.getDataType());
            String operator = element.getAttributeValue("operator", defaultOperator);
            element.setAttribute("operator", operator);

            @SuppressWarnings("unchecked")
            List<Element> values = element.getChildren("value");
            if (values != null && values.size() > 0) {
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
            for (Object child : element.getChildren()) {
                if (child instanceof Element) {
                    renameElements((Element) child);
                }
            }
        }
    }

    /**
     * Build MCRQuery from editor XML input
     */
    protected MCRQuery buildFormQuery(Element root) {
        Element conditions = root.getChild("conditions");

        if (conditions.getAttributeValue("format", "xml").equals("xml")) {
            Element condition = (Element) conditions.getChildren().get(0);
            renameElements(condition);

            // Remove conditions without values
            List<Element> empty = new ArrayList<Element>();
            for (@SuppressWarnings("unchecked")
            Iterator<Element> it = conditions.getDescendants(new ElementFilter("condition")); it.hasNext();) {
                Element cond = it.next();
                if (cond.getAttribute("value") == null) {
                    empty.add(cond);
                }
            }

            // Remove empty sort conditions
            Element sortBy = root.getChild("sortBy");
            if (sortBy != null) {
                for (@SuppressWarnings("unchecked")
                Iterator<Element> iterator = sortBy.getChildren("field").iterator(); iterator.hasNext();) {
                    Element field = iterator.next();
                    if (field.getAttributeValue("name", "").length() == 0) {
                        empty.add(field);
                    }
                }
            }

            for (int i = empty.size() - 1; i >= 0; i--) {
                empty.get(i).detach();
            }

            if (sortBy != null && sortBy.getChildren().size() == 0) {
                sortBy.detach();
            }
        }

        return MCRQuery.parseXML(root.getDocument());
    }

    protected String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        } else {
            return value.trim();
        }
    }

    protected Document setQueryOptions(MCRQuery query, HttpServletRequest req) {
        String maxResults = getReqParameter(req, "maxResults", "0");
        query.setMaxResults(Integer.parseInt(maxResults));

        List<String> sortFields = new ArrayList<String>();
        for (@SuppressWarnings("unchecked")
        Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            if (name.contains(".sortField")) {
                sortFields.add(name);
            }
        }

        if (sortFields.size() > 0) {
            Collections.sort(sortFields, new Comparator<String>() {
                public int compare(String arg0, String arg1) {
                    String s0 = arg0.substring(arg0.indexOf(".sortField"));
                    String s1 = arg1.substring(arg1.indexOf(".sortField"));
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

    @Override
    public void doGetPost(MCRServletJob job) throws IOException, ServletException, TransformerException, SAXException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        String mode = job.getRequest().getParameter("mode");
        if ("results".equals(mode)) {
            showResults(request, response);
        } else if ("load".equals(mode)) {
            loadQuery(request, response);
        } else {
            doQuery(request, response);
        }
    }

    /**
     * Returns a query that was previously submitted, to reload it into the
     * editor search mask. Usage: MCRSearchServlet?mode=load&id=XXXXX
     */
    @Deprecated
    protected void loadQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String msg = "MCRSearchServlet?mode=load is not supported any more. Please change your search masks to use source uri=\"searchInput:{id}\" ";
        throw new MCRException(msg);
    }

    /**
     * Shows a results page. Usage:
     * MCRSearchServlet?mode=results&numPerPage=10&page=1
     */
    protected void showResults(HttpServletRequest request, HttpServletResponse response) throws IOException,
        ServletException, TransformerException, SAXException {
        // Get cached results
        String id = request.getParameter("id");
        MCRCachedQueryData qd = MCRCachedQueryData.getData(id);
        if (qd == null) {
            // Try to re-run original query, if in request parameter
            doQuery(request, response);
            return;
        }

        showResults(request, response, qd);
    }

    protected void showResults(HttpServletRequest request, HttpServletResponse response, MCRQuery query, Document input)
        throws IOException, ServletException, TransformerException, SAXException {
        MCRCachedQueryData qd = MCRCachedQueryData.cache(query, input);
        showResults(request, response, qd);
    }

    private void showResults(HttpServletRequest request, HttpServletResponse response, MCRCachedQueryData qd)
        throws IOException, TransformerException, SAXException {
        MCRResults results = qd.getResults();

        // Number of hits per page
        int npp = getNumPerPage(request, results);

        // Current page number
        String spage = request.getParameter("page");
        if (spage == null) {
            spage = "1";
        }
        int page = Integer.parseInt(spage);

        // Total number of pages
        int numHits = Math.max(1, results.getNumHits());
        int numPages = 1;
        if (npp > 0) {
            numPages = (int) Math.ceil((double) numHits / (double) npp);
        }

        if (npp == 0) {
            page = 1;
        } else if (page > numPages) {
            page = numPages;
        } else if (page < 1) {
            page = 1;
        }

        // Number of first and last hit to be shown
        int first = (page - 1) * npp;
        int last = results.getNumHits() - 1;
        if (npp > 0) {
            last = Math.min(results.getNumHits(), first + npp) - 1;
        }

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
     * Returns the number of hits to display per results page, as requests by the parameter numPerPage.
     * A value of numPerPage=0 will display all hits. 
     * if set, the configuration property MCR.SearchServlet.MaxPerPage limits 
     * the maximum number of hits to display per result page.  
     */
    protected int getNumPerPage(HttpServletRequest req, MCRResults results) {
        int npp = getNumPerPage(req);
        if ((npp > results.getNumHits()) || (npp <= 0)) {
            npp = results.getNumHits();
        }
        return npp;
    }

    protected int getNumPerPage(HttpServletRequest req) {
        String snpp = req.getParameter("numPerPage");
        if (snpp == null) {
            snpp = "10";
        }
        int npp = Integer.parseInt(snpp);
        if (maxPerPage > 0) {
            npp = Math.min(maxPerPage, Math.max(0, npp));
        }
        return npp;
    }

    /**
     * Executes a query that comes from editor search mask, and redirects the
     * browser to the first results page
     */
    protected void doQuery(HttpServletRequest request, HttpServletResponse response) throws IOException,
        ServletException, TransformerException, SAXException {
        MCREditorSubmission sub = (MCREditorSubmission) request.getAttribute("MCREditorSubmission");
        String searchString = getReqParameter(request, "search", null);
        String queryString = getReqParameter(request, "query", null);

        Document input;
        MCRQuery query;

        if (sub != null) {
            input = (Document) sub.getXML().clone();
            query = buildFormQuery(sub.getXML().getRootElement());
        } else {
            if (queryString != null) {
                query = buildComplexQuery(queryString);
            } else if (searchString != null) {
                query = buildDefaultQuery(searchString);
            } else {
                query = buildNameValueQuery(request);
            }

            input = setQueryOptions(query, request);
        }

        // Show incoming query document
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
            LOGGER.debug(out.outputString(input));
        }

        boolean doNotRedirect = "false".equals(getReqParameter(request, "redirect", null));

        if (doNotRedirect) {
            showResults(request, response, query, input);
        } else {
            sendRedirect(request, response, query, input);
        }
    }

    /**      
     * Redirect browser to results page     
     *      
     *   
     * see its overwritten in jspdocportal     
     */
    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, MCRQuery query, Document input)
        throws IOException {

        MCRCachedQueryData qd = MCRCachedQueryData.cache(query, input);
        // Redirect browser to first results page   
        StringBuilder sb = new StringBuilder();
        sb.append("MCRSearchServlet?mode=results&id=").append(qd.getResults().getID());

        String numPerPage = input.getRootElement().getAttributeValue("numPerPage", "0");
        sb.append("&numPerPage=").append(numPerPage);

        String mask = input.getRootElement().getAttributeValue("mask", "-");
        sb.append("&mask=").append(mask);

        String queryString = qd.getQuery().getCondition().toString();
        sb.append("&query=").append(queryString);

        int maxResults = qd.getQuery().getMaxResults();
        sb.append("&maxResults=").append(maxResults);

        List<MCRSortBy> list = qd.getQuery().getSortBy();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                MCRSortBy sortBy = list.get(i);
                sb.append("&").append(sortBy.getFieldName());
                sb.append(".sortField.").append(i + 1);
                sb.append("=").append(sortBy.getSortOrder() ? "ascending" : "descending");
            }
        }

        sb.append(passXSLParameter("XSL.Style", req));
        sb.append(passXSLParameter("XSL.Transformer", req));

        res.sendRedirect(res.encodeRedirectURL(sb.toString()));
    }

    private String passXSLParameter(String name, HttpServletRequest req) {
        String value = req.getParameter(name);
        return (value != null) && !value.trim().isEmpty() ? "&" + name + "=" + value : "";
    }

    /**
      * Forwards the document to the output
      * 
      * see its overwritten in jspdocportal
      */
    protected void sendToLayout(HttpServletRequest req, HttpServletResponse res, Document jdom) throws IOException, TransformerException, SAXException {
        getLayoutService().doLayout(req, res, new MCRJDOMContent(jdom));
    }
}
