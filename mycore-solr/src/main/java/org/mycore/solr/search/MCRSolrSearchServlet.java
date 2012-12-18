package org.mycore.solr.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
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

    private enum SolrParameterGroup {
        SolrParameter, QueryParameter, TypeParameter
    }

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSearchServlet.class);

    /**
     * Creates a list with all parameters that can be used within a select request to solr.
     * @return the list.
     */
    private static List<String> solrParameterKeys() {
        String[] params = new String[] { "q", "sort", "start", "rows", "pageDoc", "pageScore", "fq", "cache", "fl", "glob", "debug",
                "explainOther", "defType", "timeAllowed", "omitHeader", "sortOrder", "sortBy" };
        return Collections.unmodifiableList(Arrays.asList(params));
    }

    private List<String> reservedParameterKeys;

    protected String buildQueryParameter(Map<String, String[]> queryParameters, Map<String, String[]> typeParameters) {

        List<String> typeSelectedParameter = new ArrayList<String>();
        StringBuilder queryBuilder = new StringBuilder();

        //at first we add all parameters that are member of types
        for (Entry<String, String[]> currentTypeEntry : typeParameters.entrySet()) {
            StringBuilder subFqBuilder = new StringBuilder();

            subFqBuilder.append("&fq={!join from=returnId to=id}");

            for (String typeMember : currentTypeEntry.getValue()) {
                String[] memberValues = queryParameters.get(typeMember);
                typeSelectedParameter.add(typeMember);
                if (memberValues != null) {
                    for (String currentValue : memberValues) {
                        if (currentValue.length() == 0) {
                            currentValue = "*";
                        }
                        try {
                            subFqBuilder.append(buildQueryPart(typeMember, currentValue));
                        } catch (UnsupportedEncodingException e) {
                        } // impossible(UTF-8)

                    }
                }
            }

            queryBuilder.append(subFqBuilder.toString());
        }
        queryBuilder.append("&q=");

        // then we add all others that are not added yet
        for (Entry<String, String[]> currentParameter : queryParameters.entrySet()) {
            // check the parameter is not added yet
            if (!typeSelectedParameter.contains(currentParameter.getKey())) {
                for (String parameterValue : currentParameter.getValue()) {
                    if (parameterValue.length() > 0) {
                        try {
                            queryBuilder.append(buildQueryPart(currentParameter.getKey(), parameterValue));
                        } catch (UnsupportedEncodingException e) {
                        } // impossible(UTF-8)
                    }
                }
            }
        }

        return queryBuilder.toString();
    }

    private String buildQueryPart(String fieldName, String value) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("%2B");
        sb.append(URLEncoder.encode(fieldName, "UTF-8"));
        sb.append(":");
        sb.append(URLEncoder.encode(value, "UTF-8"));
        sb.append("%20");
        return sb.toString();
    }

    protected String buildSolrParameterString(Map<String, String[]> parameters) {
        StringBuilder qBuilder = new StringBuilder();

        for (Entry<String, String[]> currentParameter : parameters.entrySet()) {
            for (String parameterValue : currentParameter.getValue()) {
                qBuilder.append("&").append(currentParameter.getKey()).append("=").append(parameterValue);
            }
        }
        if (qBuilder.length() > 0) {
            qBuilder.deleteCharAt(0);
        }

        return qBuilder.toString();

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        Map<String, String[]> solrParameters = new HashMap<String, String[]>();
        Map<String, String[]> queryParameters = new HashMap<String, String[]>();
        Map<String, String[]> typeParameters = new HashMap<String, String[]>();

        extractParameterList(job.getRequest().getParameterMap(), queryParameters, solrParameters, typeParameters);

        String q = buildQueryParameter(queryParameters, typeParameters);
        LOGGER.info("Generated Query is : " + q);

        String otherParameters = buildSolrParameterString(solrParameters);
        LOGGER.info("SolrParameter Parameter Query is : " + otherParameters);

        String url = MCRServlet.getServletBaseURL() + "SolrSelectProxy?" + otherParameters + q;

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));

    }


    protected void extractParameterList(Map<String, String[]> requestParameter, Map<String, String[]> queryParameter,
            Map<String, String[]> solrParameter, Map<String, String[]> typeParameter) {
        for (Entry<String, String[]> currentEntry : requestParameter.entrySet()) {
            String parameterName = currentEntry.getKey();
            SolrParameterGroup parameterGroup = getParameterType(parameterName);

            switch (parameterGroup) {
            case SolrParameter:
                solrParameter.put(parameterName, currentEntry.getValue());
                break;
            case TypeParameter:
                typeParameter.put(parameterName, currentEntry.getValue());
                break;
            case QueryParameter:
                queryParameter.put(parameterName, currentEntry.getValue());
                break;
            }
        }

    }

    /**
     * Returns the {@link SolrParameterGroup} for a specific parameter name
     * @param parameterName the name of the parameter
     * @return 
     */
    private SolrParameterGroup getParameterType(String parameterName) {
        if (isTypeParameter(parameterName)) {
            return SolrParameterGroup.TypeParameter;
        } else if (isSolrParameter(parameterName)) {
            return SolrParameterGroup.SolrParameter;
        } else {
            return SolrParameterGroup.QueryParameter;
        }
    }

    /**
     * 
     * @return A list of all Parameters that can be used within a select request to solr.
     *          <strong>WARNING: the list should not be modified</strong>
     */
    protected List<String> getReservedParameters() {
        return reservedParameterKeys;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        reservedParameterKeys = solrParameterKeys();
    }

    /**
     * Detects if a parameter is a solr Parameter
     * @param parameterName the name of the parameter
     * @return true if the parameter is a solr parameter
     */
    private boolean isSolrParameter(String parameterName) {
        return reservedParameterKeys.contains(parameterName);
    }

    /**
     * Detects if a parameter is a Type Parameter
     * @param parameterName the name of the parameter
     * @return true if the parameter is a type parameter
     */
    private boolean isTypeParameter(String parameterName) {
        return parameterName.startsWith("solr.type.");
    }
}
