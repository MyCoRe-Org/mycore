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

    private enum SolrParameterGroup {
        QueryParameter, SolrParameter, TypeParameter
    }

    private static final String JOIN_PATTERN = "{!join from=returnId to=id}";

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

    /**
     * Adds a field  with all values to a {@link StringBuilder}
     * A empty field value will be skipped.
     * @param query represents a solr query
     * @param fieldValues containing all the values for the field
     * @param fieldName the name of the field
     */
    private void addFieldToQuery(StringBuilder query, String[] fieldValues, String fieldName) {
        for (String fieldValue : fieldValues) {
            try {
                if (fieldValue.length() == 0) {
                    continue;
                }
                query.append(buildQueryPart(fieldName, fieldValue));
            } catch (UnsupportedEncodingException e) {
            }// impossible(UTF-8)
        }
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

    /**
     * @param queryParameters all parameter where <code>getParameterGroup.equals(QueryParameter)</code> 
     * @param typeParameters all parameter where <code>getParameterGroup.equals(TypeParameter)</code>
     * @return a map wich can be forwarded to {@link MCRSolrSelectProxyServlet}
     */
    protected Map<String, String[]> buildSelectParameterMap(Map<String, String[]> queryParameters, Map<String, String[]> typeParameters) {
        HashMap<String, String[]> queryParameterMap = new HashMap<String, String[]>();

        // and the sor.type as value
        HashMap<String, String> fieldTypeMap = createFieldTypeMap(typeParameters);

        HashMap<String, StringBuilder> filterQueryMap = new HashMap<String, StringBuilder>();
        StringBuilder query = new StringBuilder();
        for (Entry<String, String[]> queryParameter : queryParameters.entrySet()) {
            String fieldName = queryParameter.getKey();
            String[] fieldValues = queryParameter.getValue();
            // Build the q parameter without solr.type.fields
            if (!fieldTypeMap.containsKey(fieldName)) {
                addFieldToQuery(query, fieldValues, fieldName);
            } else {
                String fieldType = fieldTypeMap.get(fieldName);
                StringBuilder filterQueryBuilder = getFilterQueryBuilder(filterQueryMap, fieldType);
                addFieldToQuery(filterQueryBuilder, fieldValues, fieldName);
            }
        }

        // put query and all filterquery´s to the map
        queryParameterMap.put("q", new String[] { query.toString() });

        for (StringBuilder filterQueryBuilder : filterQueryMap.values()) {
            // skip the whole query if no field has been added
            if (filterQueryBuilder.length() > JOIN_PATTERN.length()) {
                queryParameterMap.put("fq", new String[] { filterQueryBuilder.toString() });
            }
        }

        return queryParameterMap;
    }

    private HashMap<String, String> createFieldTypeMap(Map<String, String[]> typeParameters) {
        HashMap<String, String> fieldTypeMap = new HashMap<String, String>();

        for (Entry<String, String[]> currentType : typeParameters.entrySet()) {
            for (String typeMember : currentType.getValue()) {
                fieldTypeMap.put(typeMember, currentType.getKey());
            }
        }
        return fieldTypeMap;
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
        Map<String, String[]> buildedSolrParameters = buildSelectParameterMap(queryParameters, typeParameters);
        buildedSolrParameters.putAll(solrParameters);

        request.setAttribute(MCRSolrSelectProxyServlet.MAP_KEY, buildedSolrParameters);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    /**
     * Splits the parameters into three groups.
     * @param requestParameter the map of parameters to split.
     * @param queryParameter all querys will be stored here.
     * @param solrParameter all solr-parameters will be stored here.
     * @param typeParameter all type-parameters will be stored here.
     */
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
     * @param filterQueryMap a map wich contains all {@link StringBuilder}
     * @param fieldType 
     * @return a {@link StringBuilder} for the specific fieldType
     */
    private StringBuilder getFilterQueryBuilder(HashMap<String, StringBuilder> filterQueryMap, String fieldType) {
        if (!filterQueryMap.containsKey(fieldType)) {
            filterQueryMap.put(fieldType, new StringBuilder(JOIN_PATTERN));
        }
        StringBuilder filterQueryBuilder = filterQueryMap.get(fieldType);
        return filterQueryBuilder;
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
