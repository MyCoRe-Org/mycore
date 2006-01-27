/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.sql;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.backend.query.MCRQueryManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRConditionVisitor;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * 
 * @author Arne Seifert
 * 
 */
public class MCRSQLQuery implements MCRConditionVisitor {
    public static Logger LOGGER = Logger.getLogger(MCRSQLQuery.class.getName());

    private StringBuffer sbquery = new StringBuffer();

    private String type = ""; // variable for type (and/or/not)

    private int count = 0; // number of children to proceed

    private List elList = new LinkedList(); // stack for type-elements

    private int bracket = 0; // counts correct number of ')'
 
    private List order;
    
    /**
     * initialise query 
     * 
     * @param document
     *            xml query docuement
     */
    public MCRSQLQuery(MCRCondition condition, List order, int maxResults) {
        try{
            condition.accept(this);
            this.order = order;
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    
    /**
     * interface implementation (visitor pattern) for condition types: on each
     * new type a xml-element will be added to an internal stack which holds the
     * number of children to process
     */
    public void visitType(Element element) {
        try {
            /** update child status * */
            if (elList.size() > 0) {
                int intel = elList.size() - 1;
                Element tmp = (Element) elList.get(intel);
                tmp.setAttribute("children", "" + (count - 1));
                elList.remove(intel);
                elList.add(intel, tmp);
            }

            sbquery.append(" (");
            bracket += 1;

            /** set new values * */
            type = element.getAttributeValue("type");
            count = Integer.parseInt(element.getAttributeValue("children"));
            elList.add(element);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * interface implementation (visitor pattern) for field type
     */
    public void visitQuery(MCRCondition entry) {
        try {
            Element el = entry.info();
            String fieldtype = MCRQueryManager.getInstance().getField(el.getAttributeValue("field")).getAttributeValue("type");
            String fieldname = el.getAttributeValue("field");
            String operator = el.getAttributeValue("operator");
            String value = el.getAttributeValue("value");

            /** transform values and operators * */
            if (fieldtype.equals("text") || fieldtype.equals("name") || fieldtype.equals("identifier")) {
                if (operator.equals("=") || operator.equals("contains")) {
                    operator = "like";
                }

                value = "\'%" + value + "%\'";
            }

            if (type.equals("not")) {
                sbquery.append("`" + fieldname + "` not " + operator + " " + value);
            } else {
                sbquery.append("`" + fieldname + "` " + operator + " " + value);
            }

            count -= 1;

            if (count >= 1) {
                sbquery.append(" " + type + " ");
            }

            if (count == 0) {
                sbquery.append(") ");
                bracket -= 1;

                if (elList.size() > 1) {
                    elList.remove(elList.size() - 1);
                    type = ((Element) elList.get(elList.size() - 1)).getAttributeValue("type");
                    count = Integer.parseInt(((Element) elList.get(elList.size() - 1)).getAttributeValue("children"));

                    if ((elList.size() > 0) && (count > 0)) {
                        sbquery.append(" " + type + " ");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }


    /**
     * method creates where clause for given xml query
     * 
     * @return sql where clause as string
     */
    private String getWhereClause() {
        for (int i = 0; i < bracket; i++) {
            sbquery.append(")");
        }

        bracket = 0;
        return sbquery.toString();
    }

    /**
     * method creates order by clause for given order list
     * 
     * @return sql order by clause as string
     */
    private String getOrderClause() {
        StringBuffer sb = new StringBuffer();

        try {

            for (int i = 0; i < order.size(); i++) {
                MCRSortBy field = (MCRSortBy) order.get(i);
                
                sb.append(field.getField().getName());
                
                if (field.getSortOrder() == MCRSortBy.ASCENDING ){
                    sb.append(" asc ");
                }else{
                    sb.append(" desc ");
                }

                if (i < (order.size() - 1)) {
                    sb.append(", ");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            LOGGER.error(e);

            return "";
        }
    }

    /**
     * method creates complete sql string for given query
     * 
     * @return sql as string
     */
    public String getSQLQuery() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("SELECT MCRID");

            for (int i = 0; i < order.size(); i++) {
                sb.append(", " + ((MCRSortBy) order.get(i)).getField().getName());
            }

            sb.append(" FROM ");
            sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
            sb.append(" WHERE ");
            sb.append(getWhereClause());

            if (!getOrderClause().equals("")) {
                sb.append(" ORDER BY ");
                sb.append(getOrderClause());
            }

            return sb.toString();
        } catch (Exception e) {
            LOGGER.error(e);

            return "";
        }
    }

    /**
     * method returns the string reresentation of given query
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT MCRID");

        for (int i = 0; i < order.size(); i++) {
            sb.append(", " + ((MCRSortBy) order.get(i)).getField().getName());
        }

        sb.append("\nFROM ");
        sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
        sb.append("\nWHERE ");
        sb.append(getWhereClause());

        if (!getOrderClause().equals("")) {
            sb.append("\nORDER BY ");
            sb.append(getOrderClause());
        }

        return sb.toString();
    }
}
