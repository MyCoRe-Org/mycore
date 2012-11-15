package org.mycore.solr.search;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author mcrshofm
 *
 */
public class MCRSolrSearchServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSearchServlet.class);

    private List<String> reservedParameterKeys;

    @Override
    public void init() throws ServletException {
        super.init();

        reservedParameterKeys = createReservedParameterKeys();
    }

    /**
     * Creates a list with all parameters that can be used within a select request to solr.
     * @return the list.
     */
    private List<String> createReservedParameterKeys() {
        String[] params = new String[] { "q", "sort", "start", "rows", "pageDoc", "pageScore", "fq", "cache", "fl", "glob", "debug",
                "explainOther", "defType", "timeAllowed", "omitHeader", "sortOrder", "sortBy" };
        return Collections.unmodifiableList(Arrays.asList(params));
    }

    /**
     * 
     * @return A list of all Parameters that can be used within a select request to solr.
     *          <strong>WARNING: the list should not be modified</strong>
     */
    protected List<String> getReservedParameters() {
        return reservedParameterKeys;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        // "split of" reserved parameters
        Map<String, String[]> reservedParameters = getFilteredParameterList(job.getRequest().getParameterMap(), false);
        Map<String, String[]> queryParameters = getFilteredParameterList(job.getRequest().getParameterMap(), true);

        String q = URLEncoder.encode(buildQueryParameter(queryParameters), "UTF-8");
        LOGGER.info("Generated Query is : " + q);

        String otherParameters = buildParameterString(reservedParameters);
        LOGGER.info("SolrReserved Parameter Query is : " + otherParameters);

        String url = MCRServlet.getServletBaseURL() + "SolrSelectProxy?q=" + q + otherParameters;

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));

    }

    protected String buildParameterString(Map<String, String[]> parameters) {
        StringBuilder qBuilder = new StringBuilder();

        for (Entry<String, String[]> currentParameter : parameters.entrySet()) {
            for (String parameterValue : currentParameter.getValue()) {
                qBuilder.append("&").append(currentParameter.getKey()).append("=").append(parameterValue);
            }
        }

        return qBuilder.toString();

    }

    /**
     * Builds the Query for the solr request (without q=)
     * @param parametersForQuery the parameter that should appear in the query
     * @return the value of q
     */
    protected String buildQueryParameter(Map<String, String[]> parametersForQuery) {
        StringBuilder qBuilder = new StringBuilder();

        for (Entry<String, String[]> currentParameter : parametersForQuery.entrySet()) {
            for (String parameterValue : currentParameter.getValue()) {
                if (parameterValue.length() > 0) {
                    qBuilder.append(" +");
                    qBuilder.append(currentParameter.getKey());
                    qBuilder.append(":");
                    qBuilder.append(parameterValue);
                }
            }
        }

        if (qBuilder.length() > 0) {
            qBuilder.deleteCharAt(0);
        }

        return qBuilder.toString();
    }

    /**
     * Filters the requestParameter for the generation of q= or for the generation of the reserved Parameters.
     * @param requestParameter the parameters of the Request. The map wont modified.
     * @param queryGeneration <strong>true</strong> map filtered for generation of q= 
     *                         <strong>false</strong> for the generation of reserved parameters
     * @return a filtered list
     */
    protected Map<String, String[]> getFilteredParameterList(Map<String, String[]> requestParameter, boolean queryGeneration) {
        Map<String, String[]> filteredParameterList = new HashMap<String, String[]>();
        List<String> reservedParameters = getReservedParameters();

        for (Entry<String, String[]> currentEntry : requestParameter.entrySet()) {
            String parameterKey = currentEntry.getKey();
            boolean reservedParameter = reservedParameters.contains(parameterKey) || parameterKey.startsWith("XSL.");
            if (!reservedParameter && queryGeneration || reservedParameter && !queryGeneration) {
                filteredParameterList.put(parameterKey, currentEntry.getValue());
            }
        }

        return filteredParameterList;
    }
}
