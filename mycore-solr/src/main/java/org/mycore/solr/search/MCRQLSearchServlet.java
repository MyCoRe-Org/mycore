/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.search;

import java.io.IOException;
import java.io.Serial;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.solr.proxy.MCRSolrProxyServlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MCRQLSearchServlet extends MCRServlet {//extends MCRSearchServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    /** Default search field */
    private String defaultSearchField;

    @Override
    public void init() throws ServletException {
        super.init();
        String prefix = "MCR.SearchServlet.";
        defaultSearchField = MCRConfiguration2.getString(prefix + "DefaultSearchField").orElse("allMeta");
    }

    @Override
    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String searchString = getReqParameter(request, "search", null);
        if (searchString != null) {
            searchString = ClientUtils.escapeQueryChars(searchString);
        }
        String queryString = getReqParameter(request, "query", null);
        if (queryString != null) {
            queryString = ClientUtils.escapeQueryChars(queryString);
        }

        Document input = (Document) request.getAttribute("MCRXEditorSubmission");
        MCRQuery query;

        if (input != null) {
            //xeditor input
            query = MCRQLSearchUtils.buildFormQuery(input.getRootElement());
        } else {
            if (queryString != null) {
                query = MCRQLSearchUtils.buildComplexQuery(queryString);
            } else if (searchString != null) {
                query = MCRQLSearchUtils.buildDefaultQuery(searchString, defaultSearchField);
            } else {
                query = MCRQLSearchUtils.buildNameValueQuery(request);
            }

            input = MCRQLSearchUtils.setQueryOptions(query, request);
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
        SolrQuery mergedSolrQuery = MCRSolrSearchUtils.getSolrQuery(query, input, request);
        request.setAttribute(MCRSolrProxyServlet.QUERY_KEY, mergedSolrQuery);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    protected void sendRedirect(HttpServletRequest req, HttpServletResponse res, MCRQuery query, Document input)
        throws IOException {
        SolrQuery mergedSolrQuery = MCRSolrSearchUtils.getSolrQuery(query, input, req);
        String selectProxyURL = getServletBaseURL() + "SolrSelectProxy" + mergedSolrQuery.toQueryString()
            + getReservedParameterString(req.getParameterMap());
        res.sendRedirect(res.encodeRedirectURL(selectProxyURL));
    }

    /**
     * This method is used to convert all parameters which starts with XSL. to a {@link String}.
     * @param requestParameter the map of parameters (not XSL parameter will be skipped)
     * @return a string which contains all parameters with a leading &amp;
     */
    protected String getReservedParameterString(Map<String, String[]> requestParameter) {
        StringBuilder sb = new StringBuilder();

        Set<Entry<String, String[]>> requestEntrys = requestParameter.entrySet();
        for (Entry<String, String[]> entry : requestEntrys) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith("XSL.")) {
                for (String parameterValue : entry.getValue()) {
                    sb.append('&');
                    sb.append(parameterName);
                    sb.append('=');
                    sb.append(parameterValue);
                }
            }
        }

        return sb.toString();
    }

    protected String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        } else {
            return value.trim();
        }
    }
}
