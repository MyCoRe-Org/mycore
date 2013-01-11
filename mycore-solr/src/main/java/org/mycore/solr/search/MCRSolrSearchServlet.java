package org.mycore.solr.search;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author mcrshofm
 *
 */
public class MCRSolrSearchServlet extends MCRServlet {

    private static final String JOIN_FROM_RETURN_ID_TO_ID = "{!join from=returnId to=id}";

    private enum SolrParameterGroup {
        SolrParameter, QueryParameter, TypeParameter
    }

    private static final long serialVersionUID = 1L;

    /**
     * Creates a list with all parameters that can be used within a select request to solr.
     * @return the list.
     */
    private static List<String> solrParameterKeys() {
        String[] params = new String[] { "q", "sort", "start", "rows", "pageDoc", "pageScore", "fq", "cache", "fl", "glob", "debug",
                "explainOther", "defType", "timeAllowed", "omitHeader", "sortOrder", "sortBy", "XSL.Style" };
        return Collections.unmodifiableList(Arrays.asList(params));
    }

    private List<String> reservedParameterKeys;

    protected Map<String, String[]> buildQueryParameterMap(Map<String, String[]> queryParameters, Map<String, String[]> typeParameters) {
        HashMap<String, String[]> queryParameterMap = new HashMap<String, String[]>();

        // this Map contains all solr fields that are a solr.type.* as key
        // and the sor.type as value
        HashMap<String, String> memberTypeMap = new HashMap<String, String>();

        // fill the member type map
        for (Entry<String, String[]> currentType : typeParameters.entrySet()) {
            for (String typeMember : currentType.getValue()) {
                memberTypeMap.put(typeMember, currentType.getKey());
            }
        }

        HashMap<String, StringBuilder> filterQueryMap = new HashMap<String, StringBuilder>();
        StringBuilder query = new StringBuilder();
        for (Entry<String, String[]> queryParameter : queryParameters.entrySet()) {
            String fieldName = queryParameter.getKey();

            // Build the q parameter without solr.type.fields
            if (!memberTypeMap.containsKey(fieldName)) {
                for (String fieldValue : queryParameter.getValue()) {
                    try {
                        if (fieldValue.length() == 0) {
                            continue;
                        }
                        query.append(buildQueryPart(fieldName, fieldValue));
                    } catch (UnsupportedEncodingException e) {
                    }// impossible(UTF-8)
                }
            } else {
                // a stringbuilder for each solr.type
                String fieldType = memberTypeMap.get(fieldName);
                if (!filterQueryMap.containsKey(fieldType)) {
                    filterQueryMap.put(fieldType, new StringBuilder(JOIN_FROM_RETURN_ID_TO_ID));
                }

                StringBuilder filterQueryBuilder = filterQueryMap.get(fieldType);
                for (String fieldValue : queryParameter.getValue()) {
                    try {
                        if (fieldValue.length() == 0){
                            continue;
                        }
                        filterQueryBuilder.append(buildQueryPart(fieldName, fieldValue));
                    } catch (UnsupportedEncodingException e) {
                    }// impossible(UTF-8)
                }
            }
        }

        // put query and all filterquery´s to the map
        String[] queryAsArray = { query.toString() };
        queryParameterMap.put("q", queryAsArray);

        for (StringBuilder filterQueryBuilder : filterQueryMap.values()) {
            if (filterQueryBuilder.length() <= JOIN_FROM_RETURN_ID_TO_ID.length()) {
                continue;
            }

            String[] filterQueryAsArray = { filterQueryBuilder.toString() };
            queryParameterMap.put("fq", filterQueryAsArray);
        }

        return queryParameterMap;
    }

    private String buildQueryPart(String fieldName, String value) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        sb.append(fieldName);
        sb.append(":");
        sb.append(value);
        sb.append(" ");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        Map<String, String[]> solrParameters = new HashMap<String, String[]>();
        Map<String, String[]> queryParameters = new HashMap<String, String[]>();
        Map<String, String[]> typeParameters = new HashMap<String, String[]>();

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        extractParameterList(request.getParameterMap(), queryParameters, solrParameters, typeParameters);
        Map<String, String[]> buildedSolrParameters = buildQueryParameterMap(queryParameters, typeParameters);
        buildedSolrParameters.putAll(solrParameters);
        
        request.setAttribute(MCRSolrSelectProxyServlet.MAP_KEY, buildedSolrParameters);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
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
