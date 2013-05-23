package org.mycore.solr.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

import org.apache.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Used to map a formular-post to a solr request. </br></br>
 * <b>Parameters</b>
 * <dl>
 *  <dt><strong>Solr reserved parameters</strong></dt>
 *      <dd>They will directly forwarded to the server.</dd>
 *  <dt><strong>Type parameters</strong></dt>
 *      <dd>They are used to join other documents in the search. They start with "solr.type.".</dd>
 *  <dt><strong>Sort parameters</strong></dt>
 *      <dd>They are used to sort the results in the right order. They start with "sort."</dd>
 *  <dt><strong>Query parameters</strong></dt>
 *      <dd>They are used to build the query for solr. All parameters wich arent reserved, type or sort parameters will be stored here.</dd>
 * </dl>
 * 
 * @author mcrshofm
 */
public class MCRSolrSearchServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private enum SolrParameterGroup {
        QueryParameter, SolrParameter, SortParameter, TypeParameter
    }

    private static final Logger LOGGER = Logger.getLogger(MCRSolrSearchServlet.class);

    private static final String JOIN_PATTERN = "{!join from=returnId to=id}";

    /** Parameters that can be used within a select request to solr*/
    static final List<String> RESERVED_PARAMETER_KEYS;

    static {
        String[] parameter = new String[] { "q", "sort", "start", "rows", "pageDoc", "pageScore", "fq", "cache", "fl",
                "glob", "debug", "explainOther", "defType", "timeAllowed", "omitHeader", "sortOrder", "sortBy", "wt",
                "qf", "q.alt", "mm", "pf", "ps", "qs", "tie", "bq", "bf" };
        RESERVED_PARAMETER_KEYS = Collections.unmodifiableList(Arrays.asList(parameter));
    }

    /**
     * Adds a field  with all values to a {@link StringBuilder}
     * A empty field value will be skipped.
     * @param query represents a solr query
     * @param fieldValues containing all the values for the field
     * @param fieldName the name of the field
     */
    private void addFieldToQuery(StringBuilder query, String[] fieldValues, String fieldName) {
        for (String fieldValue : fieldValues) {
            if (fieldValue.length() == 0) {
                continue;
            }
            query.append(buildQueryPart(fieldName, fieldValue));
        }
    }

    private String buildQueryPart(String fieldName, String value) {
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
    protected Map<String, String[]> buildSelectParameterMap(Map<String, String[]> queryParameters,
        Map<String, String[]> typeParameters, Map<String, String[]> sortParameters) {
        HashMap<String, String[]> queryParameterMap = new HashMap<String, String[]>();

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
        queryParameterMap.put("q", new String[] { query.toString().trim() });

        for (StringBuilder filterQueryBuilder : filterQueryMap.values()) {
            // skip the whole query if no field has been added
            if (filterQueryBuilder.length() > JOIN_PATTERN.length()) {
                queryParameterMap.put("fq", new String[] { filterQueryBuilder.toString() });
            }
        }

        queryParameterMap.put("sort", new String[] { buildSolrSortParameter(sortParameters) });

        return queryParameterMap;
    }

    /**
     * 
     * @param sortParameters
     * @return
     */
    private String buildSolrSortParameter(Map<String, String[]> sortParameters) {
        Set<Entry<String, String[]>> sortParameterEntrys = sortParameters.entrySet();
        Map<Integer, String> positionOrderMap = new HashMap<Integer, String>();
        Map<Integer, String> positionFieldMap = new HashMap<Integer, String>();

        for (Entry<String, String[]> sortParameterEntry : sortParameterEntrys) {
            StringTokenizer st = new StringTokenizer(sortParameterEntry.getKey(), ".");
            st.nextToken(); // skip sort.
            Integer position = new Integer(st.nextToken());
            String type = st.nextToken();
            String[] valueArray = sortParameterEntry.getValue();
            if (valueArray.length > 0) {
                String value = valueArray[0];
                if ("order".equals(type)) {
                    positionOrderMap.put(position, value);
                } else if ("field".equals(type)) {
                    positionFieldMap.put(position, value);
                }
            }
        }

        ArrayList<Integer> sortedPositions = new ArrayList<Integer>();

        sortedPositions.addAll(positionFieldMap.keySet());
        Collections.sort(sortedPositions);

        StringBuilder sortBuilder = new StringBuilder();
        for (Iterator<Integer> positionIterator = sortedPositions.iterator(); positionIterator.hasNext();) {
            Integer position = (Integer) positionIterator.next();
            sortBuilder.append(",");
            sortBuilder.append(positionFieldMap.get(position));
            String order = positionOrderMap.get(position);
            sortBuilder.append(" ");
            if (order == null) {
                order = "asc";
                LOGGER.warn(MessageFormat.format(
                    "No sort order found for field with number ''{0}'' use default value : ''{1}''", position, order));
            }
            sortBuilder.append(order);
        }
        if (sortBuilder.length() != 0) {
            sortBuilder.deleteCharAt(0);
        }

        return sortBuilder.toString();
    }

    /**
     * This method is used to create a map wich contains all fields as key and the type of the field as value.
     * @param typeParameters 
     * @return
     */
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
        Map<String, String[]> sortParameters = new HashMap<String, String[]>();

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        extractParameterList(request.getParameterMap(), queryParameters, solrParameters, typeParameters, sortParameters);
        Map<String, String[]> buildedSolrParameters = buildSelectParameterMap(queryParameters, typeParameters,
            sortParameters);
        buildedSolrParameters.putAll(solrParameters);

        request.setAttribute(MCRSolrSelectProxyServlet.MAP_KEY, buildedSolrParameters);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }

    /**
     * Splits the parameters into three groups.
     * @param requestParameter the map of parameters to split.
     * @param queryParameter all querys will be stored here.
     * @param solrParameter all solr-parameters will be stored here.
     * @param typeParameter all type-parameters will be stored here.
     * @param sortParameter all sort-parameters will be stored here.
     */
    protected void extractParameterList(Map<String, String[]> requestParameter, Map<String, String[]> queryParameter,
        Map<String, String[]> solrParameter, Map<String, String[]> typeParameter, Map<String, String[]> sortParameter) {
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
                case SortParameter:
                    sortParameter.put(parameterName, currentEntry.getValue());
                    break;
                default:
                    LOGGER.warn("Unknown parameter group. That should not happen.");
                    continue;
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
     * Returns the {@link SolrParameterGroup} for a specific parameter name.
     * @param parameterName the name of the parameter
     * @return the parameter group enum
     */
    private SolrParameterGroup getParameterType(String parameterName) {
        if (isTypeParameter(parameterName)) {
            LOGGER.debug(MessageFormat.format("Parameter {0} is a {1}", parameterName,
                SolrParameterGroup.TypeParameter.toString()));
            return SolrParameterGroup.TypeParameter;
        } else if (isSolrParameter(parameterName)) {
            LOGGER.debug(MessageFormat.format("Parameter {0} is a {1}", parameterName,
                SolrParameterGroup.SolrParameter.toString()));
            return SolrParameterGroup.SolrParameter;
        } else if (isSortParameter(parameterName)) {
            LOGGER.debug(MessageFormat.format("Parameter {0} is a {1}", parameterName,
                SolrParameterGroup.SolrParameter.toString()));
            return SolrParameterGroup.SortParameter;
        } else {
            LOGGER.debug(MessageFormat.format("Parameter {0} is a {1}", parameterName,
                SolrParameterGroup.QueryParameter.toString()));
            return SolrParameterGroup.QueryParameter;
        }
    }

    /**
     * Detects if a parameter is a solr parameter
     * @param parameterName the name of the parameter
     * @return true if the parameter is a solr parameter
     */
    private boolean isSolrParameter(String parameterName) {
        return parameterName.startsWith("XSL.") || RESERVED_PARAMETER_KEYS.contains(parameterName);
    }

    /**
     * Detects if a parameter is a sort parameter
     * @param parameterName the name of the parameter
     * @return true if the parameter is a sort parameter
     */
    private boolean isSortParameter(String parameterName) {
        return parameterName.startsWith("sort.");
    }

    /**
     * Detects if a parameter is a Type parameter
     * @param parameterName the name of the parameter
     * @return true if the parameter is a type parameter
     */
    private boolean isTypeParameter(String parameterName) {
        return parameterName.startsWith("solr.type.");
    }
}
