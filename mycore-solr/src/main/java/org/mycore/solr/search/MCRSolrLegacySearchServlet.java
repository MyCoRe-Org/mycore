package org.mycore.solr.search;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRSearchServlet;
import org.mycore.solr.legacy.MCRLuceneSolrAdapter;

public class MCRSolrLegacySearchServlet extends MCRSearchServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void showResults(HttpServletRequest request, HttpServletResponse response, MCRCachedQueryData qd) throws IOException, ServletException {
        MCRQuery query = qd.getQuery();
        SolrQuery solrQuery = MCRLuceneSolrAdapter.getSolrQuery(query.getCondition(), query.getSortBy(), query.getMaxResults());
        request.setAttribute(MCRSolrSelectProxyServlet.QUERY_KEY, solrQuery);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

}
