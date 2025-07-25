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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrUtils;

/**
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 */
public class MCRConditionTransformer {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * If a condition references fields from multiple indexes, this constant is
     * returned
     */
    protected static final String MIXED = "--mixed--";

    private static volatile Set<String> joinFields;

    public static String toSolrQueryString(@SuppressWarnings("rawtypes") MCRCondition condition,
        Set<String> usedFields) {
        return toSolrQueryString(condition, usedFields, false).toString();
    }

    public static boolean explicitAndOrMapping() {
        return MCRConfiguration2.getBoolean("MCR.Solr.ConditionTransformer.ExplicitAndOrMapping").orElse(false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StringBuilder toSolrQueryString(MCRCondition condition, Set<String> usedFields,
        boolean subCondition) {
        if (condition instanceof MCRQueryCondition qCond) {
            return handleQueryCondition(qCond, usedFields);
        }
        if (condition instanceof MCRSetCondition) {
            MCRSetCondition<MCRCondition> setCond = (MCRSetCondition<MCRCondition>) condition;
            return handleSetCondition(setCond, usedFields, subCondition);
        }
        if (condition instanceof MCRNotCondition notCond) {
            return handleNotCondition(notCond, usedFields);
        }
        throw new MCRException("Cannot handle MCRCondition class: " + condition.getClass().getCanonicalName());
    }

    private static StringBuilder handleQueryCondition(MCRQueryCondition qCond, Set<String> usedFields) {
        String field = qCond.getFieldName();
        String value = qCond.getValue();
        String operator = qCond.getOperator();
        usedFields.add(field);
        return switch (operator) {
            case "like", "contains" -> getTermQuery(field, value.trim());
            case "=", "phrase" -> getPhraseQuery(field, value);
            case "<" -> getLTQuery(field, value);
            case "<=" -> getLTEQuery(field, value);
            case ">" -> getGTQuery(field, value);
            case ">=" -> getGTEQuery(field, value);
            default -> throw new UnsupportedOperationException("Do not know how to handle operator: " + operator);
        };

    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleSetCondition(MCRSetCondition<MCRCondition> setCond, Set<String> usedFields,
        boolean subCondition) {
        if (explicitAndOrMapping()) {
            return handleSetConditionExplicit(setCond, usedFields);
        } else {
            return handleSetConditionDefault(setCond, usedFields, subCondition);
        }
    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleSetConditionExplicit(MCRSetCondition<MCRCondition> setCond,
        Set<String> usedFields) {
        List<MCRCondition<MCRCondition>> children = setCond.getChildren();
        if (children.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        Iterator<MCRCondition<MCRCondition>> iterator = children.iterator();
        StringBuilder subSb = toSolrQueryString(iterator.next(), usedFields, true);
        sb.append(subSb);
        while (iterator.hasNext()) {
            sb.append(' ').append(setCond.getOperator().toUpperCase(Locale.ROOT)).append(' ');
            subSb = toSolrQueryString(iterator.next(), usedFields, true);
            sb.append(subSb);
        }
        sb.append(')');
        return sb;
    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleSetConditionDefault(MCRSetCondition<MCRCondition> setCond,
        Set<String> usedFields,
        boolean subCondition) {
        boolean stripPlus;
        if (setCond instanceof MCROrCondition) {
            stripPlus = true;
        } else if (setCond instanceof MCRAndCondition) {
            stripPlus = false;
        } else {
            throw new UnsupportedOperationException("Do not know how to handle "
                + setCond.getClass().getCanonicalName() + " set operation.");
        }
        List<MCRCondition<MCRCondition>> children = setCond.getChildren();
        if (children.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean groupRequired = subCondition || setCond instanceof MCROrCondition;
        if (groupRequired) {
            sb.append("+(");
        }
        Iterator<MCRCondition<MCRCondition>> iterator = children.iterator();
        StringBuilder subSb = toSolrQueryString(iterator.next(), usedFields, true);
        sb.append(stripPlus ? stripPlus(subSb) : subSb);
        while (iterator.hasNext()) {
            sb.append(' ');
            subSb = toSolrQueryString(iterator.next(), usedFields, true);
            sb.append(stripPlus ? stripPlus(subSb) : subSb);
        }
        if (groupRequired) {
            sb.append(')');
        }
        return sb;
    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleNotCondition(MCRNotCondition notCond, Set<String> usedFields) {
        MCRCondition child = notCond.getChild();
        StringBuilder sb = new StringBuilder();
        sb.append('-');
        StringBuilder solrQueryString = toSolrQueryString(child, usedFields, true);
        if (!explicitAndOrMapping()) {
            stripPlus(solrQueryString);
        }
        if (solrQueryString == null || solrQueryString.isEmpty()) {
            return null;
        }
        sb.append(solrQueryString);
        return sb;
    }

    private static StringBuilder getRangeQuery(String field, String lowerTerm, boolean includeLower, String upperTerm,
        boolean includeUpper) {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        sb.append(field);
        sb.append(':');
        sb.append(includeLower ? '[' : '{');
        sb.append(
            lowerTerm != null ? (Objects.equals(lowerTerm, "*") ? "\\*" : MCRSolrUtils.escapeSearchValue(lowerTerm))
                : "*");
        sb.append(" TO ");
        sb.append(
            upperTerm != null ? (Objects.equals(upperTerm, "*") ? "\\*" : MCRSolrUtils.escapeSearchValue(upperTerm))
                : "*");
        sb.append(includeUpper ? ']' : '}');
        return sb;
    }

    public static StringBuilder getLTQuery(String field, String value) {
        return getRangeQuery(field, null, true, value, false);
    }

    public static StringBuilder getLTEQuery(String field, String value) {
        return getRangeQuery(field, null, true, value, true);
    }

    public static StringBuilder getGTQuery(String field, String value) {
        return getRangeQuery(field, value, false, null, true);
    }

    public static StringBuilder getGTEQuery(String field, String value) {
        return getRangeQuery(field, value, true, null, true);
    }

    public static StringBuilder getTermQuery(String field, String value) {
        if (value.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!explicitAndOrMapping()) {
            sb.append('+');
        }
        sb.append(field);
        sb.append(':');
        String replaced = value.replaceAll("\\s+", " AND ");
        if (value.length() == replaced.length()) {
            sb.append(MCRSolrUtils.escapeSearchValue(value));
        } else {
            sb.append('(');
            sb.append(MCRSolrUtils.escapeSearchValue(replaced));
            sb.append(')');
        }
        return sb;
    }

    public static StringBuilder getPhraseQuery(String field, String value) {
        StringBuilder sb = new StringBuilder();
        if (!explicitAndOrMapping()) {
            sb.append('+');
        }
        sb.append(field);
        sb.append(':');
        sb.append('"');
        sb.append(MCRSolrUtils.escapeSearchValue(value));
        sb.append('"');
        return sb;
    }

    private static StringBuilder stripPlus(StringBuilder sb) {
        if (sb == null || sb.isEmpty()) {
            return sb;
        }
        if (sb.charAt(0) == '+') {
            sb.deleteCharAt(0);
        }
        return sb;
    }

    public static SolrQuery getSolrQuery(@SuppressWarnings("rawtypes") MCRCondition condition, List<MCRSortBy> sortBy,
        int maxResults, List<String> returnFields) {
        String queryString = getQueryString(condition);
        SolrQuery q = applySortOptions(new SolrQuery(queryString), sortBy);
        q.setIncludeScore(true);
        q.setRows(maxResults == 0 ? Integer.MAX_VALUE : maxResults);

        if (returnFields != null) {
            q.setFields(!returnFields.isEmpty() ? returnFields.stream().collect(Collectors.joining(",")) : "*");
        }
        String sort = q.getSortField();
        LOGGER.info("MyCoRe Query transformed to: {}{} {}", q::getQuery, () -> sort != null ? " " + sort : "",
            q::getFields);
        return q;
    }

    public static String getQueryString(@SuppressWarnings("rawtypes") MCRCondition condition) {
        Set<String> usedFields = new HashSet<>();
        return toSolrQueryString(condition, usedFields);
    }

    public static SolrQuery applySortOptions(SolrQuery q, List<MCRSortBy> sortBy) {
        for (MCRSortBy option : sortBy) {
            SortClause sortClause = new SortClause(option.getFieldName(), option.getSortOrder() ? ORDER.asc
                : ORDER.desc);
            q.addSort(sortClause);
        }
        return q;
    }

    /**
     * Builds SOLR query.
     * <p>
     * Automatically builds JOIN-Query if content search fields are used in query.
     * @param sortBy sort criteria
     * @param not true, if all conditions should be negated
     * @param and AND or OR connective between conditions
     * @param table conditions per "content" or "metadata"
     * @param maxHits maximum hits
     */
    @SuppressWarnings("rawtypes")
    public static SolrQuery buildMergedSolrQuery(List<MCRSortBy> sortBy, boolean not, boolean and,
        Map<String, List<MCRCondition>> table, int maxHits, List<String> returnFields) {
        List<MCRCondition> queryConditions = table.get("metadata");
        MCRCondition combined = buildSubCondition(queryConditions, and, not);
        SolrQuery solrRequestQuery = getSolrQuery(combined, sortBy, maxHits, returnFields);

        for (Map.Entry<String, List<MCRCondition>> mapEntry : table.entrySet()) {
            if (!mapEntry.getKey().equals("metadata")) {
                MCRCondition combinedFilterQuery = buildSubCondition(mapEntry.getValue(), and, not);
                SolrQuery filterQuery = getSolrQuery(combinedFilterQuery, sortBy, maxHits, returnFields);
                solrRequestQuery.addFilterQuery(MCRSolrConstants.SOLR_JOIN_PATTERN + filterQuery.getQuery());
            }
        }
        return solrRequestQuery;
    }

    /** Builds a new condition for all fields from one single index */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static MCRCondition buildSubCondition(List<MCRCondition> conditions, boolean and, boolean not) {
        MCRCondition subCond;
        if (conditions.size() == 1) {
            subCond = conditions.getFirst();
        } else if (and) {
            subCond = new MCRAndCondition().addAll(conditions);
        } else {
            subCond = new MCROrCondition().addAll(conditions);
        }
        if (not) {
            subCond = new MCRNotCondition<>(subCond);
        }
        return subCond;
    }

    /**
     * Build a table from index ID to a List of conditions referencing this
     * index
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, List<MCRCondition>> groupConditionsByIndex(MCRSetCondition cond) {
        Map<String, List<MCRCondition>> table = new HashMap<>();
        List<MCRCondition> children = cond.getChildren();

        for (MCRCondition child : children) {
            String index = getIndex(child);
            table.computeIfAbsent(index, k -> new ArrayList<>()).add(child);
        }
        return table;
    }

    /**
     * Returns the ID of the index of all fields referenced in this condition.
     * If the fields come from multiple indexes, the constant mixed is returned.
     */
    @SuppressWarnings("rawtypes")
    private static String getIndex(MCRCondition cond) {
        if (cond instanceof MCRQueryCondition queryCondition) {
            String fieldName = queryCondition.getFieldName();
            return getIndex(fieldName);
        } else if (cond instanceof MCRNotCondition) {
            return getIndex(((MCRNotCondition) cond).getChild());
        }

        @SuppressWarnings("unchecked")
        List<MCRCondition> children = ((MCRSetCondition) cond).getChildren();

        // mixed indexes here!
        return children.stream()
            .map(MCRConditionTransformer::getIndex)
            .reduce((l, r) -> l.equals(r) ? l : MIXED)
            .get();
    }

    public static String getIndex(String fieldName) {
        return getJoinFields().contains(fieldName) ? "content" : "metadata";
    }

    private static Set<String> getJoinFields() {
        if (joinFields == null) {
            synchronized (MCRConditionTransformer.class) {
                if (joinFields == null) {
                    joinFields = MCRConfiguration2.getString(SOLR_CONFIG_PREFIX + "JoinQueryFields")
                        .stream()
                        .flatMap(MCRConfiguration2::splitValue)
                        .collect(Collectors.toCollection(HashSet::new));
                }
            }
        }
        return joinFields;
    }

}
