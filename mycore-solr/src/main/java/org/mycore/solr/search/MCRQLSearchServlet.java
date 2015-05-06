package org.mycore.solr.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRSortBy;
//import org.mycore.services.fieldquery.MCRSearchServlet;
import org.mycore.solr.proxy.MCRSolrProxyServlet;
import org.xml.sax.SAXException;

public class MCRQLSearchServlet extends MCRServlet {//extends MCRSearchServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRQLSearchServlet.class);

    /** Default search field */
    private String defaultSearchField;

    private static HashSet<String> SEARCH_PARAMETER = new HashSet<>(Arrays.asList(new String[] { "search", "query",
        "maxResults", "numPerPage", "page", "mask", "mode", "redirect" }));

    @Override
    public void init() throws ServletException {
        super.init();
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.SearchServlet.";
        defaultSearchField = config.getString(prefix + "DefaultSearchField", "allMeta");
    }

    @Override
    public void doGetPost(MCRServletJob job) throws IOException, ServletException, TransformerException, SAXException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        MCREditorSubmission sub = (MCREditorSubmission) request.getAttribute("MCREditorSubmission");
        String searchString = getReqParameter(request, "search", null);
        String queryString = getReqParameter(request, "query", null);

        Document input = (Document) request.getAttribute("MCRXEditorSubmission");
        MCRQuery query;

        if (sub != null) {
            input = (Document) sub.getXML().clone();
            query = buildFormQuery(sub.getXML().getRootElement());
        } else {
            if (input != null) {
                //xeditor input
                query = buildFormQuery(input.getRootElement());
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

    protected void showResults(HttpServletRequest request, HttpServletResponse response, MCRQuery query, Document input)
        throws IOException, ServletException {
        SolrQuery mergedSolrQuery = getSolrQuery(query, input, request);
        request.setAttribute(MCRSolrProxyServlet.QUERY_KEY, mergedSolrQuery);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    @SuppressWarnings("rawtypes")
    protected SolrQuery getSolrQuery(MCRQuery query, Document input, HttpServletRequest request) {
        int rows = Integer.parseInt(input.getRootElement().getAttributeValue("numPerPage", "10"));
        MCRCondition condition = query.getCondition();
        HashMap<String, List<MCRCondition>> table;

        if (condition instanceof MCRSetCondition) {
            table = MCRConditionTransformer.groupConditionsByIndex((MCRSetCondition) condition);
        } else {
            // if there is only one condition its no set condition. we don't need to group
            LOGGER.warn("Condition is not SetCondition.");
            table = new HashMap<String, List<MCRCondition>>();

            ArrayList<MCRCondition> conditionList = new ArrayList<MCRCondition>();
            conditionList.add(condition);

            table.put("metadata", conditionList);

        }

        SolrQuery mergedSolrQuery = MCRConditionTransformer.buildMergedSolrQuery(query.getSortBy(), false, true, table,
            rows);
        String mask = input.getRootElement().getAttributeValue("mask");
        if (mask != null) {
            mergedSolrQuery.setParam("mask", mask);
            mergedSolrQuery.setParam("_session", request.getParameter("_session"));
        }
        return mergedSolrQuery;
    }

    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, MCRQuery query, Document input)
        throws IOException {
        SolrQuery mergedSolrQuery = getSolrQuery(query, input, req);
        String selectProxyURL = MCRServlet.getServletBaseURL() + "SolrSelectProxy?" + mergedSolrQuery.toString()
            + getReservedParameterString(req.getParameterMap());
        res.sendRedirect(res.encodeRedirectURL(selectProxyURL));
    }

    /**
     * This method is used to convert all parameters wich starts with XSL. to a {@link String}.
     * @param requestParameter the map of parameters (not XSL parameter will be skipped)
     * @return a string wich contains all parameters with a leading &
     */
    protected String getReservedParameterString(Map<String, String[]> requestParameter) {
        StringBuilder sb = new StringBuilder();

        Set<Entry<String, String[]>> requestEntrys = requestParameter.entrySet();
        for (Entry<String, String[]> entry : requestEntrys) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("XSL.")) {
                for (String parameterValue : entry.getValue()) {
                    sb.append("&");
                    sb.append(parameterName);
                    sb.append("=");
                    sb.append(parameterValue);
                }
            }
        }

        return sb.toString();
    }

    protected String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        } else {
            return value.trim();
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
            for (Iterator<Element> it = conditions.getDescendants(new ElementFilter("condition")); it.hasNext();) {
                Element cond = it.next();
                if (cond.getAttribute("value") == null) {
                    empty.add(cond);
                }
            }

            // Remove empty sort conditions
            Element sortBy = root.getChild("sortBy");
            if (sortBy != null) {
                for (Iterator<Element> iterator = sortBy.getChildren("field").iterator(); iterator.hasNext();) {
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

    /**
     * Rename elements conditionN to condition. 
     * Transform condition with multiple child values to OR-condition.
     */
    private void renameElements(Element element) {
        if (element.getName().startsWith("condition")) {
            element.setName("condition");

            String field = new StringTokenizer(element.getAttributeValue("field"), " -,").nextToken();
            String operator = element.getAttributeValue("operator");
            if (operator == null) {
                LOGGER.warn("No operator defined for field: " + field);
                operator = "=";
            }
            element.setAttribute("operator", operator);

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
     * Search using complex query expression given as text string
     */
    protected MCRQuery buildComplexQuery(String query) {
        return new MCRQuery(new MCRQueryParser().parse(query));
    }

    /**
     * Search in default search field specified by MCR.SearchServlet.DefaultSearchField
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected MCRQuery buildDefaultQuery(String search) {
        String[] fields = defaultSearchField.split(" *, *");
        MCROrCondition queryCondition = new MCROrCondition();

        for (String fDef : fields) {
            MCRCondition condition = new MCRQueryCondition(fDef, "=", search);
            queryCondition.addChild(condition);
        }

        return new MCRQuery(MCRQueryParser.normalizeCondition(queryCondition));
    }

    /**
     * Search using name=value pairs from HTTP request
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected MCRQuery buildNameValueQuery(HttpServletRequest req) {
        MCRAndCondition condition = new MCRAndCondition();

        for (Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();
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
                String operator = getReqParameter(req, fieldName + ".operator", "=");
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

    protected Document setQueryOptions(MCRQuery query, HttpServletRequest req) {
        String maxResults = getReqParameter(req, "maxResults", "0");
        query.setMaxResults(Integer.parseInt(maxResults));

        List<String> sortFields = new ArrayList<String>();
        for (Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
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
                sortBy.add(new MCRSortBy(name, order));
            }
            query.setSortBy(sortBy);
        }

        Document xml = query.buildXML();
        xml.getRootElement().setAttribute("numPerPage", getReqParameter(req, "numPerPage", "0"));
        xml.getRootElement().setAttribute("mask", getReqParameter(req, "mask", "-"));
        return xml;
    }
}
