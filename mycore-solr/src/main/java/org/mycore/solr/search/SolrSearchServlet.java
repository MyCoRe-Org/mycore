package org.mycore.solr.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.SolrServerFactory;

/**
 * @author shermann
 *
 */
public class SolrSearchServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(SolrSearchServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        SolrQuery query = createQuery(job.getRequest());
        LOGGER.debug("Generated query of request is: " + query.getQuery());

        int start = 0;
        int rows = 10;

        try {
            start = Integer.valueOf(job.getRequest().getParameter("start"));
            rows = Integer.valueOf(job.getRequest().getParameter("rows"));
        } catch (Exception e) {
            LOGGER.error("Could not apply values for \"row\" and \"start\". Using defaults start=" + start + " and rows=" + rows, e);
        }

        long tStart = System.currentTimeMillis();

        SolrURL solrURL = new SolrURL(SolrServerFactory.getSolrServer(), true);
        solrURL.setQueryParamter(query.getQuery());
        solrURL.setStart(start);
        solrURL.setRows(rows);

        addSortOptions(solrURL, job);

        MCRContent streamContent = null;
        try {
            streamContent = new MCRStreamContent(solrURL.openStream(), solrURL.getUrl().toString());
            LOGGER.info("Getting query results took " + (System.currentTimeMillis() - tStart) + " ms");
            getLayoutService().doLayout(job.getRequest(), job.getResponse(), streamContent);
        } finally {
            try {
                if (streamContent != null) {
                    streamContent.getInputStream().close();
                }
            } catch (IOException ex) {
                LOGGER.warn("Could not close input stream", ex);
            }
        }
    }

    /**
     * @param solrURL
     * @param job
     */
    @SuppressWarnings("unchecked")
    private void addSortOptions(SolrURL solrURL, MCRServletJob job) {
        // set sort field and sort order
        Enumeration<String> parameterNames = job.getRequest().getParameterNames();

        List<String> keys = new Vector<String>();
        while (parameterNames.hasMoreElements()) {
            String p = (String) parameterNames.nextElement();
            if (p.startsWith("sortBy")) {
                keys.add(p);
            }
        }

        String[] sortedKeys = keys.toArray(new String[0]);
        Arrays.sort(sortedKeys);

        for (String key : sortedKeys) {
            String order = "sortBy".equals(key) ? "" : "_" + key.split("_")[1];

            String sortBy = job.getRequest().getParameter(key);
            String sortOrder = job.getRequest().getParameter("sortOrder" + order);

            if (sortBy != null && sortOrder != null) {
                solrURL.addSortOption(sortBy, ORDER.valueOf(sortOrder).toString());
            }
        }
    }

    /**
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    private SolrQuery createQuery(HttpServletRequest request) {
        String q = request.getParameter("q");
        // default query
        if (q != null && q.length() == 0) {
            return new SolrQuery("*:*");
        }

        if (q != null && q.length() > 0) {
            return new SolrQuery(q);
        }

        Enumeration<String> parameterNames = request.getParameterNames();
        StringBuilder qBuilder = new StringBuilder();

        while (parameterNames.hasMoreElements()) {
            String p = parameterNames.nextElement();
            // ignore these
            if ("rows".equals(p) || "start".equals(p) || p.startsWith("sort") || p.equals("XSL.Style")) {
                continue;
            }

            // get the value for the actual parameter and ensure it is not null
            String v = request.getParameter(p);
            if (v.length() == 0) {
                continue;
            }

            qBuilder.append(" +" + p + ":" + v);
        }
        return new SolrQuery(qBuilder.toString().trim());
    }
}
