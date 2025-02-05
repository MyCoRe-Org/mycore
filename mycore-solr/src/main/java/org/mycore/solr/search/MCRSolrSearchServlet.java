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

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.search.MCRSolrParameter.FILTER_QUERY;
import static org.mycore.solr.search.MCRSolrParameter.QUERY;
import static org.mycore.solr.search.MCRSolrParameter.SORT;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.proxy.MCRSolrProxyServlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Used to map a formular-post to a solr request.
 * <p>
 * <b>Parameters</b>
 * </p>
 * <dl>
 * <dt><strong>Solr reserved parameters</strong></dt>
 * <dd>They will directly forwarded to the server.</dd>
 * <dt><strong>Type parameters</strong></dt>
 * <dd>They are used to join other documents in the search. They start with
 * "solr.type.".</dd>
 * <dt><strong>Sort parameters</strong></dt>
 * <dd>They are used to sort the results in the right order. They start with
 * "sort."</dd>
 * <dt><strong>Query parameters</strong></dt>
 * <dd>They are used to build the query for solr. All parameters which are not
 * reserved, type or sort parameters will be stored here.</dd>
 * </dl>
 * @author mcrshofm
 * @author mcrsherm
 */

public class MCRSolrSearchServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private enum QueryType {
        PHRASE, TERM
    }

    private enum SolrParameterGroup {
        QUERY_PARAMETER, SOLR_PARAMETER, SORT_PARAMETER, TYPE_PARAMETER
    }

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrSearchServlet.class);

    private static final String JOIN_PATTERN = "{!join from=returnId to=id}";

    private static final String PHRASE_QUERY_PARAM = "solr.phrase";

    /** Parameters that can be used within a select request to solr */
    static final List<String> RESERVED_PARAMETER_KEYS;

    static {
        String[] parameter = { "q", "qt", "sort", "start", "rows", "pageDoc", "pageScore", "fq", "cache",
            "fl", "glob",
            "debug", "explainOther", "defType", "timeAllowed", "omitHeader", "sortOrder", "sortBy", "wt", "qf", "q.alt",
            "mm", "pf",
            "ps", "qs", "tie", "bq", "bf", "lang", "facet", "facet.field", "facet.sort", "facet.method",
            PHRASE_QUERY_PARAM, };

        RESERVED_PARAMETER_KEYS = List.of(parameter);
    }

    /**
     * Adds a field with all values to a {@link StringBuilder} An empty field
     * value will be skipped.
     *
     * @param query
     *            represents a solr query
     * @param fieldValues
     *            containing all the values for the field
     * @param fieldName
     *            the name of the field
     */
    private void addFieldToQuery(StringBuilder query, String[] fieldValues, String fieldName, QueryType queryType)
        throws ServletException {
        for (String fieldValue : fieldValues) {
            if (fieldValue.isEmpty()) {
                continue;
            }
            switch (queryType) {
                case TERM -> query.append(MCRConditionTransformer.getTermQuery(fieldName, fieldValue));
                case PHRASE -> query.append(MCRConditionTransformer.getPhraseQuery(fieldName, fieldValue));
                default -> throw new ServletException("Query type is unsupported: " + queryType);
            }
            query.append(' ');
        }
    }

    /**
     * @param queryParameters
     *            all parameter where
     *            <code>getParameterGroup.equals(QueryParameter)</code>
     * @param typeParameters
     *            all parameter where
     *            <code>getParameterGroup.equals(TypeParameter)</code>
     * @return a map which can be forwarded to {@link MCRSolrProxyServlet}
     */
    protected Map<String, String[]> buildSelectParameterMap(Map<String, String[]> queryParameters,
        Map<String, String[]> typeParameters,
        Map<String, String[]> sortParameters, Set<String> phraseQuery) throws ServletException {
        Map<String, String[]> queryParameterMap = new HashMap<>();

        Map<String, String> fieldTypeMap = createFieldTypeMap(typeParameters);

        Map<String, StringBuilder> filterQueryMap = new HashMap<>();
        StringBuilder query = new StringBuilder();
        for (Entry<String, String[]> queryParameter : queryParameters.entrySet()) {
            String fieldName = queryParameter.getKey();
            String[] fieldValues = queryParameter.getValue();
            QueryType queryType = phraseQuery.contains(fieldName) ? QueryType.PHRASE : QueryType.TERM;
            // Build the q parameter without solr.type.fields
            if (!fieldTypeMap.containsKey(fieldName)) {
                addFieldToQuery(query, fieldValues, fieldName, queryType);

            } else {
                String fieldType = fieldTypeMap.get(fieldName);
                StringBuilder filterQueryBuilder = getFilterQueryBuilder(filterQueryMap, fieldType);
                addFieldToQuery(filterQueryBuilder, fieldValues, fieldName, queryType);
            }
        }

        // put query and all filterquery´s to the map
        queryParameterMap.put(QUERY.getValue(), new String[] { query.toString().trim() });

        for (StringBuilder filterQueryBuilder : filterQueryMap.values()) {
            // skip the whole query if no field has been added
            if (filterQueryBuilder.length() > JOIN_PATTERN.length()) {
                queryParameterMap.put(FILTER_QUERY.getValue(), new String[] { filterQueryBuilder.toString() });
            }
        }

        queryParameterMap.put(SORT.getValue(), new String[] { buildSolrSortParameter(sortParameters) });

        return queryParameterMap;
    }

    private String buildSolrSortParameter(Map<String, String[]> sortParameters) {
        Set<Entry<String, String[]>> sortParameterEntrys = sortParameters.entrySet();
        Map<Integer, String> positionOrderMap = new HashMap<>();
        Map<Integer, String> positionFieldMap = new HashMap<>();

        for (Entry<String, String[]> sortParameterEntry : sortParameterEntrys) {
            StringTokenizer st = new StringTokenizer(sortParameterEntry.getKey(), ".");
            st.nextToken(); // skip sort.
            Integer position = Integer.parseInt(st.nextToken());
            String type = st.nextToken();
            String[] valueArray = sortParameterEntry.getValue();
            if (valueArray.length > 0) {
                String value = valueArray[0];
                if (Objects.equals(type, "order")) {
                    positionOrderMap.put(position, value);
                } else if (Objects.equals(type, "field")) {
                    positionFieldMap.put(position, value);
                }
            }
        }

        List<Integer> sortedPositions = new ArrayList<>(positionFieldMap.keySet());
        Collections.sort(sortedPositions);

        StringBuilder sortBuilder = new StringBuilder();
        for (Integer position : sortedPositions) {
            sortBuilder.append(',');
            sortBuilder.append(positionFieldMap.get(position));
            String order = positionOrderMap.get(position);
            sortBuilder.append(' ');
            if (order == null) {
                order = "asc";
                LOGGER.warn("No sort order found for field with number ''{}'' use default value : ''{}''", position,
                    order);
            }
            sortBuilder.append(order);
        }
        if (!sortBuilder.isEmpty()) {
            sortBuilder.deleteCharAt(0);
        }

        return sortBuilder.toString();
    }

    /**
     * This method is used to create a map which contains all fields as key and
     * the type of the field as value.
     */
    private Map<String, String> createFieldTypeMap(Map<String, String[]> typeParameters) {
        Map<String, String> fieldTypeMap = new HashMap<>();

        for (Entry<String, String[]> currentType : typeParameters.entrySet()) {
            for (String typeMember : currentType.getValue()) {
                fieldTypeMap.put(typeMember, currentType.getKey());
            }
        }
        return fieldTypeMap;
    }

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        Map<String, String[]> solrParameters = new HashMap<>();
        Map<String, String[]> queryParameters = new HashMap<>();
        Map<String, String[]> typeParameters = new HashMap<>();
        Map<String, String[]> sortParameters = new HashMap<>();
        Set<String> phraseQuery = new HashSet<>();
        String[] phraseFields = job.getRequest().getParameterValues(PHRASE_QUERY_PARAM);
        if (phraseFields != null) {
            phraseQuery.addAll(Arrays.asList(phraseFields));
        }

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        extractParameterList(request.getParameterMap(), queryParameters, solrParameters, typeParameters,
            sortParameters);
        Map<String, String[]> buildedSolrParameters = buildSelectParameterMap(queryParameters, typeParameters,
            sortParameters, phraseQuery);
        buildedSolrParameters.putAll(solrParameters);

        request.setAttribute(MCRSolrProxyServlet.MAP_KEY, buildedSolrParameters);
        LOGGER.info("Forward SOLR Parameters: {}", buildedSolrParameters);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher("/servlets/SolrSelectProxy");
        requestDispatcher.forward(request, response);
    }

    /**
     * Splits the parameters into three groups.
     *
     * @param requestParameter
     *            the map of parameters to split.
     * @param queryParameter
     *            all query's will be stored here.
     * @param solrParameter
     *            all solr-parameters will be stored here.
     * @param typeParameter
     *            all type-parameters will be stored here.
     * @param sortParameter
     *            all sort-parameters will be stored here.
     */
    protected void extractParameterList(Map<String, String[]> requestParameter, Map<String, String[]> queryParameter,
        Map<String, String[]> solrParameter, Map<String, String[]> typeParameter, Map<String, String[]> sortParameter) {
        for (Entry<String, String[]> currentEntry : requestParameter.entrySet()) {
            String parameterName = currentEntry.getKey();
            if (PHRASE_QUERY_PARAM.equals(parameterName)) {
                continue;
            }
            SolrParameterGroup parameterGroup = getParameterType(parameterName);

            switch (parameterGroup) {
                case SOLR_PARAMETER -> solrParameter.put(parameterName, currentEntry.getValue());
                case TYPE_PARAMETER -> typeParameter.put(parameterName, currentEntry.getValue());
                case QUERY_PARAMETER -> {
                    String[] strings = currentEntry.getValue();
                    for (String v : strings) {
                        if (v != null && !v.isEmpty()) {
                            queryParameter.put(parameterName, currentEntry.getValue());
                        }
                    }
                }
                case SORT_PARAMETER -> sortParameter.put(parameterName, currentEntry.getValue());
                default -> {
                    LOGGER.warn("Unknown parameter group. That should not happen.");
                }
            }
        }
    }

    /**
     * @param filterQueryMap
     *            a map which contains all {@link StringBuilder}
     * @return a {@link StringBuilder} for the specific fieldType
     */
    private StringBuilder getFilterQueryBuilder(Map<String, StringBuilder> filterQueryMap, String fieldType) {
        if (!filterQueryMap.containsKey(fieldType)) {
            filterQueryMap.put(fieldType, new StringBuilder(JOIN_PATTERN));
        }
        return filterQueryMap.get(fieldType);
    }

    /**
     * Returns the {@link SolrParameterGroup} for a specific parameter name.
     *
     * @param parameterName
     *            the name of the parameter
     * @return the parameter group enum
     */
    private SolrParameterGroup getParameterType(String parameterName) {
        String debugMessage = "Parameter {} is a {}";
        if (isTypeParameter(parameterName)) {
            LOGGER.debug(debugMessage, parameterName, SolrParameterGroup.TYPE_PARAMETER);
            return SolrParameterGroup.TYPE_PARAMETER;
        } else if (isSolrParameter(parameterName)) {
            LOGGER.debug(debugMessage, parameterName, SolrParameterGroup.SOLR_PARAMETER);
            return SolrParameterGroup.SOLR_PARAMETER;
        } else if (isSortParameter(parameterName)) {
            LOGGER.debug(debugMessage, parameterName, SolrParameterGroup.SOLR_PARAMETER);
            return SolrParameterGroup.SORT_PARAMETER;
        } else {
            LOGGER.debug(debugMessage, parameterName, SolrParameterGroup.QUERY_PARAMETER);
            return SolrParameterGroup.QUERY_PARAMETER;
        }
    }

    /**
     * Detects if a parameter is a solr parameter
     *
     * @param parameterName
     *            the name of the parameter
     * @return true if the parameter is a solr parameter
     */
    private boolean isSolrParameter(String parameterName) {
        boolean reservedCustomKey;
        try {
            reservedCustomKey = MCRConfiguration2
                .getOrThrow(SOLR_CONFIG_PREFIX + "ReservedParameterKeys", MCRConfiguration2::splitValue)
                .anyMatch(parameterName::equals);
        } catch (MCRConfigurationException e) {
            reservedCustomKey = false;
        }
        return parameterName.startsWith("XSL.") || RESERVED_PARAMETER_KEYS.contains(parameterName) || reservedCustomKey;
    }

    /**
     * Detects if a parameter is a sort parameter
     *
     * @param parameterName
     *            the name of the parameter
     * @return true if the parameter is a sort parameter
     */
    private boolean isSortParameter(String parameterName) {
        return parameterName.startsWith("sort.");
    }

    /**
     * Detects if a parameter is a type parameter
     *
     * @param parameterName
     *            the name of the parameter
     * @return true if the parameter is a type parameter
     */
    private boolean isTypeParameter(String parameterName) {
        return parameterName.startsWith("solr.type.");
    }

}
