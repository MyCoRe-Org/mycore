package org.mycore.solr.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.OperationNotSupportedException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRDefaultQueryEngine;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRSearchServlet;
import org.mycore.solr.proxy.MCRSolrProxyServlet;

public class MCRSolrLegacySearchServlet extends MCRSearchServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRSolrLegacySearchServlet.class);

    /*
    @Override
    protected void showResults(HttpServletRequest request, HttpServletResponse response, MCRCachedQueryData qd) throws IOException, ServletException {
        MCRQuery query = qd.getQuery();
        
        SolrQuery solrQuery = MCRLuceneSolrAdapter.getSolrQuery(query.getCondition(), query.getSortBy(), query.getMaxResults());
        request.setAttribute(MCRSolrProxyServlet.QUERY_KEY, solrQuery);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }*/

    @Override
    protected void showResults(HttpServletRequest request, HttpServletResponse response, MCRQuery query, Document input)
        throws IOException, ServletException {
        SolrQuery mergedSolrQuery = getSolrQuery(query, input);
        request.setAttribute(MCRSolrProxyServlet.QUERY_KEY, mergedSolrQuery);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    @SuppressWarnings("rawtypes")
    private SolrQuery getSolrQuery(MCRQuery query, Document input) {
        int rows = Integer.parseInt(input.getRootElement().getAttributeValue("numPerPage", "10"));
        MCRCondition condition = query.getCondition();
        HashMap<String, List<MCRCondition>> table;

        if (condition instanceof MCRSetCondition) {
            table = MCRDefaultQueryEngine.groupConditionsByIndex((MCRSetCondition) condition);
        } else {
            // if there is only one condition its no set condition. we dont need to group
            LOGGER.warn("Condition is not SetCondition.");
            table = new HashMap<String, List<MCRCondition>>();

            ArrayList<MCRCondition> conditionList = new ArrayList<MCRCondition>();
            conditionList.add(condition);

            table.put("metadata", conditionList);

        }

        SolrQuery mergedSolrQuery = MCRSolrQueryEngine
            .buildMergedSolrQuery(query.getSortBy(), false, true, table, rows);
        String mask = input.getRootElement().getAttributeValue("mask");
        if (mask != null) {
            mergedSolrQuery.setParam("mask", mask);
        }
        return mergedSolrQuery;
    }

    @Override
    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, MCRQuery query, Document input)
        throws IOException {
        SolrQuery mergedSolrQuery = getSolrQuery(query, input);
        @SuppressWarnings("unchecked")
        String selectProxyURL = MCRServlet.getServletBaseURL() + "SolrSelectProxy?" + mergedSolrQuery.toString()
            + getReservedParameterString(req.getParameterMap());
        res.sendRedirect(res.encodeRedirectURL(selectProxyURL));
    }

    @Override
    protected void showResults(HttpServletRequest request, HttpServletResponse response) throws IOException,
        ServletException {
        throw new MCRException(new OperationNotSupportedException("showResults(request, response) is not supported by "
            + MCRSolrLegacySearchServlet.class.getCanonicalName()));
    }

    /**
     * This method is used to convert all parameters wich starts with XSL. to a {@link String}.
     * @param requestParameter the map of parameters (not XSL parameter will be skipped)
     * @return a string wich contains all parameters with a leading &
     */
    private String getReservedParameterString(Map<String, String[]> requestParameter) {
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
}
