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

package org.mycore.backend.hibernate;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRConditionVisitor;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Helper class for easy use of refelction given by hibernate. Getter and Setter
 * methods will give a standard interface for saving values in querytable
 * HQL-queries will be created by given xml or Document
 * 
 * @author Arne Seifert
 * 
 */
public class MCRHIBQuery implements MCRConditionVisitor {
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRHIBIndexer.class.getName());

    private Object queryobject;

    private Class queryclass;

    private Method[] querymethods;

    private StringBuffer sbquery = new StringBuffer();

    private String type = ""; // variable for type (and/or/not)

    private int count = 0; // number of children to proceed

    private List elList = new LinkedList(); // stack for type-elements

    private int bracket = 0; // counts correct number of ')'

    private List order;

    /**
     * constructor creates a reference on the Hibernate Getter/Setter class by
     * an internal object
     */
    public MCRHIBQuery(){
        try{
            queryclass = Class.forName("org.mycore.backend.query.MCRQuery");
            queryobject = queryclass.newInstance();
            querymethods = queryclass.getMethods();
        }catch(Exception e){
            LOGGER.error(e);
        }
        
    }
    
    public MCRHIBQuery(MCRCondition condition, List order) {
        try {
            queryclass = Class.forName("org.mycore.backend.query.MCRQuery");
            queryobject = queryclass.newInstance();
            querymethods = queryclass.getMethods();
            condition.accept(this);
            this.order = order;
            
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * constructor creates internal object for given object. Method is needed
     * for return values of queries
     * 
     * @param obj
     *            should be a MCRHIBQuery
     */
    public MCRHIBQuery(Object obj) {
        queryclass = obj.getClass();
        queryobject = obj;
        querymethods = queryclass.getMethods();
    }

    /**
     * returns the object (reference on Hibernate mapping class)
     * 
     * @return
     */
    public Object getQueryObject() {
        return queryobject;
    }

    /**
     * Setter method for 'call-by-name'-use (all setter methods need only one
     * parameter)
     * 
     * @param methodname
     *            like 'setXY' as string
     * @param value
     *            value for given given parameter
     */
    public void setValue(String methodname, String value) {
        Object[] values = { value };
        values[0] = value;

        for (int i = 0; i < querymethods.length; i++) {
            if (querymethods[i].getName().toLowerCase().equals(methodname.toLowerCase()) && (value != "")) {
                try {
                    if (querymethods[i].getParameterTypes()[0].getName().equals("java.sql.Date")) {
                        // date format
                        values[0] = java.sql.Date.valueOf(value.split("[|]")[0]);
                    } else if (querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Integer")) {
                        // integer
                        values[0] = new Integer(Integer.parseInt(value.split("[|]")[0]));
                    } else if (querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Boolean")) {
                        // boolean
                        values[0] = new Boolean(value.toUpperCase());
                    } else if (querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Double")) {
                        try {
                            values[0] = new Double(Double.parseDouble(value.split("[|]")[0].replaceAll("[,]", ".")));
                        } catch (Exception e) {
                            values[0] = new Double(0);
                        }
                    }

                    if ((value != "") && querymethods[i].getParameterTypes()[0].getName().equals(values[0].getClass().getName())) {
                        querymethods[i].invoke(queryobject, values);
                    }
                } catch (Exception e) {
                    System.out.println("value can't be imported: '" + value + "'  " + methodname.substring(3));
                }
            }
        }
    }

    /**
     * Getter method for 'call-by-name'-use (all getter methods return only on
     * parameter)
     * 
     * @param methodname
     *            like 'getXY' as string
     * @return Object (String, int, ...)
     */
    public Object getValue(String methodname) {
        Object ret = new Object();

        for (int i = 0; i < querymethods.length; i++) {
            if (querymethods[i].getName().toLowerCase().equals(methodname.toLowerCase())) {
                try {
                    ret = querymethods[i].invoke(queryobject, null);

                    break;
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }
        
        return ret;
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
            MCRQueryCondition qc = (MCRQueryCondition)( entry );
            String fieldtype = qc.getField().getDataType();
            String operator = qc.getOperator();
            String value = qc.getValue();
            String fieldname = qc.getField().getName();
            
            String not = " ";
            if (type.equals("not")) {
            	not = " not ";
            }
            /** transform values and operators * */
            if (fieldtype.equals("text") || fieldtype.equals("name") || fieldtype.equals("identifier")) {
            	value = value.replaceAll("\\*","%");
                if (operator.equals("=") || operator.equals("like") ) {
                	operator = "like";
                	if(!fieldtype.equals("identifier")) {
                		value = "\'%" + value + "%\'";
                	}else {
                		value = "\'" + value + "\'";
                    }
                    sbquery.append(fieldname).append(not).append(operator).append(" ").append(value);
                } else if (operator.equals("contains")) {
                	sbquery.append(not).append(" match_against(").append(fieldname)
                		.append(",'").append(value).append("') > 0");
                }
            }else {
            	sbquery.append(fieldname).append(not).append(operator).append(" ").append(value);
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
     * method creates order by clause for given xml query
     * 
     * @return sql order by clause as string
     */
    private String getOrderClause() {
        StringBuffer sb = new StringBuffer();

        try {

            for (int i = 0; i < order.size(); i++) {
                MCRSortBy by = (MCRSortBy)(order.get(i));
                sb.append(by.getField().getName());
                sb.append( by.getSortOrder() == MCRSortBy.ASCENDING ? " asc " : " desc " );

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
    public String getHIBQuery() {
        StringBuffer sb = new StringBuffer();

        try {
            sb.append("from ");
            sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
            sb.append(" where ");
            sb.append(getWhereClause());

            if (!getOrderClause().equals("")) {
                sb.append(" order by ");
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
        sb.append("from ");
        sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
        sb.append("\nwhere ");
        sb.append(getWhereClause());

        if (!getOrderClause().equals("")) {
            sb.append("\norder by ");
            sb.append(getOrderClause());
        }

        return sb.toString();
    }
    
}
