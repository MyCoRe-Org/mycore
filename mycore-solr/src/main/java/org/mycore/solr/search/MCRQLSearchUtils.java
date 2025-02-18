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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;
import org.mycore.common.MCRUsageException;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRSortBy;

import jakarta.servlet.http.HttpServletRequest;

public class MCRQLSearchUtils {

    private static final Logger LOGGER = LogManager.getLogger(MCRQLSearchUtils.class);

    private static final Set<String> SEARCH_PARAMETER = Set.of("search", "query", "maxResults",
        "numPerPage", "page", "mask", "mode", "redirect", "qt");

    private static final String ELEMENT_CONDITIONS = "conditions";

    private static final String ELEMENT_CONDITION = "condition";

    private static final String ELEMENT_FIELD = "field";

    private static final String ELEMENT_SORT_BY = "sortBy";

    private static final String ELEMENT_RETURN_FIELDS = "returnFields";

    private static final String ELEMENT_VALUE = "value";

    private static final String ATTRIBUTE_FIELD = "field";

    private static final String ATTRIBUTE_OPERATOR = "operator";

    private static final String ATTRIBUTE_VALUE = "value";

    private static final String REQUEST_PARAM_PART_OPERATOR = ".operator";

    private static final String REQUEST_PARAM_PART_SORT_FIELD = ".sortField";

    /**
     * Build MCRQuery from editor XML input
     */
    public static MCRQuery buildFormQuery(Element root) {
        Element conditions = root.getChild(ELEMENT_CONDITIONS);

        if (conditions.getAttributeValue("format", "xml").equals("xml")) {
            Element condition = conditions.getChildren().getFirst();
            renameElements(condition);

            // Remove conditions without values
            List<Element> empty = new ArrayList<>();
            IteratorIterable<Element> descendants = conditions.getDescendants(new ElementFilter(ELEMENT_CONDITION));
            while (descendants.hasNext()) {
                Element cond = descendants.next();
                if (cond.getAttribute(ATTRIBUTE_VALUE) == null) {
                    empty.add(cond);
                }
            }

            // Remove empty sort conditions
            Element sortBy = root.getChild(ELEMENT_SORT_BY);
            if (sortBy != null) {
                for (Element field : sortBy.getChildren(ELEMENT_FIELD)) {
                    if (field.getAttributeValue("name", "").isEmpty()) {
                        empty.add(field);
                    }
                }
                if (sortBy.getChildren().isEmpty()) {
                    sortBy.detach();
                }
            }

            // Remove collected empty elements
            for (Element e : empty) {
                e.detach();
            }

            // Remove empty returnFields
            Element returnFields = root.getChild(ELEMENT_RETURN_FIELDS);
            if (returnFields != null && returnFields.getText().isEmpty()) {
                returnFields.detach();
            }
        }

        return MCRQuery.parseXML(root.getDocument());
    }

    /**
     * Rename elements conditionN to condition. Transform condition with multiple child values to OR-condition.
     */
    protected static void renameElements(Element element) {
        if (element.getName().startsWith(ELEMENT_CONDITION)) {
            element.setName(ELEMENT_CONDITION);

            String field = new StringTokenizer(element.getAttributeValue(ATTRIBUTE_FIELD), " -,").nextToken();
            String operator = element.getAttributeValue(ATTRIBUTE_OPERATOR);
            if (operator == null) {
                LOGGER.warn("No operator defined for field: {}", field);
                operator = "=";
            }
            element.setAttribute(ATTRIBUTE_OPERATOR, operator);

            List<Element> values = element.getChildren(ELEMENT_VALUE);
            if (values != null && !values.isEmpty()) {
                element.removeAttribute(ATTRIBUTE_FIELD);
                element.setAttribute(ATTRIBUTE_OPERATOR, "or");
                element.setName("boolean");
                for (Element value : values) {
                    value.setName(ELEMENT_CONDITION);
                    value.setAttribute(ATTRIBUTE_FIELD, field);
                    value.setAttribute(ATTRIBUTE_OPERATOR, operator);
                    value.setAttribute(ATTRIBUTE_VALUE, value.getText());
                    value.removeContent();
                }
            }
        } else if (element.getName().startsWith("boolean")) {
            element.setName("boolean");
            element.getChildren().forEach(MCRQLSearchUtils::renameElements);
        }
    }

    /**
     * Search using complex query expression given as text string
     */
    public static MCRQuery buildComplexQuery(String query) {
        return new MCRQuery(new MCRQueryParser().parse(query));
    }

    /**
     * Search in default search field specified by MCR.SearchServlet.DefaultSearchField
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static MCRQuery buildDefaultQuery(String search, String defaultSearchField) {
        String[] fields = defaultSearchField.split(" *, *");
        MCROrCondition queryCondition = new MCROrCondition<>();

        for (String fDef : fields) {
            MCRCondition condition = new MCRQueryCondition(fDef, "=", search);
            queryCondition.addChild(condition);
        }

        return new MCRQuery(MCRQueryParser.normalizeCondition(queryCondition));
    }

    /**
     * Search using name=value pairs from HTTP request
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static MCRQuery buildNameValueQuery(HttpServletRequest req) {
        MCRAndCondition condition = new MCRAndCondition<>();

        for (Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();

            // Skip irrelevant parameters
            if (name.endsWith(REQUEST_PARAM_PART_OPERATOR)
                || name.contains(REQUEST_PARAM_PART_SORT_FIELD)
                || SEARCH_PARAMETER.contains(name)
                || name.startsWith("XSL.")) {
                continue;
            }

            String[] values = req.getParameterValues(name);
            MCRSetCondition parent = condition;

            if ((values.length > 1) || name.contains(",")) {
                // Multiple fields with same name, combine with OR
                parent = new MCROrCondition<>();
                condition.addChild(parent);
            }

            for (String fieldName : name.split(",")) {
                String operator = getReqParameter(req, fieldName + REQUEST_PARAM_PART_OPERATOR, "=");
                for (String value : values) {
                    parent.addChild(new MCRQueryCondition(fieldName, operator, value));
                }
            }
        }

        if (condition.getChildren().isEmpty()) {
            throw new MCRUsageException("Missing query condition");
        }

        return new MCRQuery(MCRQueryParser.normalizeCondition(condition));
    }

    protected static Document setQueryOptions(MCRQuery query, HttpServletRequest req) {
        String maxResults = getReqParameter(req, "maxResults", "0");
        query.setMaxResults(Integer.parseInt(maxResults));

        List<String> sortFields = new ArrayList<>();
        for (Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            if (name.contains(REQUEST_PARAM_PART_SORT_FIELD)) {
                sortFields.add(name);
            }
        }

        if (!sortFields.isEmpty()) {
            sortFields.sort((arg0, arg1) -> {
                String s0 = arg0.substring(arg0.indexOf(REQUEST_PARAM_PART_SORT_FIELD));
                String s1 = arg1.substring(arg1.indexOf(REQUEST_PARAM_PART_SORT_FIELD));
                return s0.compareTo(s1);
            });
            List<MCRSortBy> sortBy = new ArrayList<>();
            for (String name : sortFields) {
                String sOrder = getReqParameter(req, name, "ascending");
                boolean order = Objects.equals(sOrder, "ascending") ? MCRSortBy.ASCENDING : MCRSortBy.DESCENDING;
                String fieldName = name.substring(0, name.indexOf(REQUEST_PARAM_PART_SORT_FIELD));
                sortBy.add(new MCRSortBy(fieldName, order));
            }
            query.setSortBy(sortBy);
        }

        Document xml = query.buildXML();
        xml.getRootElement().setAttribute("numPerPage", getReqParameter(req, "numPerPage", "0"));
        xml.getRootElement().setAttribute("mask", getReqParameter(req, "mask", "-"));
        return xml;
    }

    protected static String getReqParameter(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        } else {
            return value.trim();
        }
    }
}
