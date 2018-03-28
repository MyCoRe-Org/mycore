/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/** Represents a query with its condition and optional parameters */
public class MCRQuery {

    /** The query condition */
    private MCRCondition cond;

    /** The maximum number of results, default is 0 = unlimited */
    private int maxResults = 0;

    /** A list of MCRSortBy criteria, may be empty */
    private List<MCRSortBy> sortBy = new ArrayList<>();

    /** A List of SOLR fields they should be return in the response */
    private List<String> returnFields = new ArrayList<>();

    /** A cached xml representation of the query */
    private Document doc = null;

    /**
     * Builds a new MCRQuery object without sort criteria and unlimited results.
     * 
     * @param cond
     *            the query conditions
     */
    public MCRQuery(MCRCondition<Void> cond) {
        this.cond = MCRQueryParser.normalizeCondition(cond);
    }

    /**
     * Builds a new MCRQuery object with sort criteria and limited number of
     * results.
     * 
     * @param cond
     *            the query conditions
     * @param sortBy
     *            a list of MCRSortBy criteria for sorting the results
     * @param maxResults
     *            the maximum number of results to return
     * @param returnFields
     *            the return fields for the SOLR parameter fl
     */
    public MCRQuery(MCRCondition<Void> cond, List<MCRSortBy> sortBy, int maxResults, List<String> returnFields) {
        this.cond = MCRQueryParser.normalizeCondition(cond);
        this.setSortBy(sortBy);
        setMaxResults(maxResults);
        this.setReturnFields(returnFields);
    }

    /**
     * Returns the query condition
     * 
     * @return the query condition
     */
    public MCRCondition getCondition() {
        return cond;
    }

    /**
     * Returns the maximum number of results the query should return
     * 
     * @return the maximum number of results, or 0
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results the query should return. Default is 0
     * which means "return all results".
     * 
     * @param maxResults
     *            the maximum number of results
     */
    public void setMaxResults(int maxResults) {
        if (maxResults < 0) {
            maxResults = 0;
        }
        this.maxResults = maxResults;
        doc = null;
    }

    /**
     * Returns the list of MCRSortBy criteria for sorting query results
     * 
     * @return a list of MCRSortBy objects, may be empty
     */
    public List<MCRSortBy> getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort criteria for the query results
     * 
     * @param sortBy
     *            a list of MCRSortBy objects, may be empty
     */
    public void setSortBy(List<MCRSortBy> sortBy) {
        if (sortBy == null) {
            sortBy = new ArrayList<>();
        }
        this.sortBy = sortBy;
        doc = null;
    }

    /**
     * Sets the sort criteria for the query results
     * 
     * @param sortBy
     *            a MCRSortBy object
     */
    public void setSortBy(MCRSortBy sortBy) {
        this.sortBy = new ArrayList<>();
        if (sortBy != null) {
            this.sortBy.add(sortBy);
        }
        doc = null;
    }

    /**
     * Returns the list of SOLR-fields they should return for a query
     * results
     * 
     * @return a list of field names, may be empty
     */
    public List<String> getReturnFields() {
        return returnFields;
    }

    /**
     * Returns the CSV-list of SOLR-fields they should return for a query
     * results
     * 
     * @return a list of field names, may be empty
     */
    public String getReturnFieldsAsString() {
        return returnFields.stream().collect(Collectors.joining(","));
    }

    /**
     * Sets the return fields list for the query results
     * 
     * @param returnFields
     *            a list of SOLR return fields, may be empty
     */
    public void setReturnFields(List<String> returnFields) {
        this.returnFields = returnFields == null ? new ArrayList<>() : returnFields;
    }

    /**
     * Sets the return fields as String for the query results
     * 
     * @param returnFields
     *            a CSV-list of SOLR return fields, may be empty
     */
    public void setReturnFields(String returnFields) {
        if (returnFields == null || returnFields.length() == 0) {
            this.returnFields = new ArrayList<>();
        }
        this.returnFields = Arrays.asList(returnFields.split(","));
    }

    /**
     * Builds a XML representation of the query
     * 
     * @return a XML document containing all query parameters
     */
    public synchronized Document buildXML() {
        if (doc == null) {
            Element query = new Element("query");
            query.setAttribute("maxResults", String.valueOf(maxResults));

            if (sortBy != null && sortBy.size() > 0) {
                Element sortByElem = new Element("sortBy");
                query.addContent(sortByElem);
                for (MCRSortBy sb : sortBy) {
                    Element ref = new Element("field");
                    ref.setAttribute("name", sb.getFieldName());
                    ref.setAttribute("order", sb.getSortOrder() ? "ascending" : "descending");
                    sortByElem.addContent(ref);
                }
            }

            if (returnFields != null && returnFields.size() > 0) {
                Element returns = new Element("returnFields");
                returns.setText(returnFields.stream().collect(Collectors.joining(",")));
                query.addContent(returns);
            }

            Element conditions = new Element("conditions");
            query.addContent(conditions);
            conditions.setAttribute("format", "xml");
            conditions.addContent(cond.toXML());
            doc = new Document(query);
        }
        return doc;
    }

    /**
     * Parses a XML representation of a query.
     * 
     * @param doc
     *            the XML document
     * @return the parsed MCRQuery
     */
    public static MCRQuery parseXML(Document doc) {
        Element xml = doc.getRootElement();
        Element conditions = xml.getChild("conditions");
        MCRQuery query = null;
        if (conditions.getAttributeValue("format", "xml").equals("xml")) {
            Element condElem = conditions.getChildren().get(0);
            query = new MCRQuery(new MCRQueryParser().parse(condElem));
        } else {
            String queryString = conditions.getTextTrim();
            query = new MCRQuery(new MCRQueryParser().parse(queryString));
        }

        String max = xml.getAttributeValue("maxResults", "");
        if (max.length() > 0) {
            query.setMaxResults(Integer.parseInt(max));
        }

        List<MCRSortBy> sortBy = null;
        Element sortByElem = xml.getChild("sortBy");

        if (sortByElem != null) {
            List children = sortByElem.getChildren();
            sortBy = new ArrayList<>(children.size());

            for (Object aChildren : children) {
                Element sortByChild = (Element) aChildren;
                String name = sortByChild.getAttributeValue("name");
                String ad = sortByChild.getAttributeValue("order");

                boolean direction = "ascending".equals(ad) ? MCRSortBy.ASCENDING : MCRSortBy.DESCENDING;
                sortBy.add(new MCRSortBy(name, direction));
            }
        }

        if (sortBy != null) {
            query.setSortBy(sortBy);
        }

        Element returns = xml.getChild("returnFields");
        if (returns != null) {
            query.setReturnFields(returns.getText());
        }

        return query;
    }
}
