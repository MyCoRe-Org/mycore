/**
 * * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.mycore.backend.query.MCRQueryManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryConditionVisitor;
import org.mycore.services.fieldquery.MCRQueryParser;

/**
 * Helper class for easy use of refelction given by hibernate.
 * Getter and Setter methods will give a standard interface for saving values in
 * querytable
 * HQL-queries will be created by given xml or Document
 * 
 * @author Arne Seifert
 *
 */
public class MCRHIBQuery implements MCRQueryConditionVisitor{

    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRHIBIndexer.class.getName());
    
    private Object queryobject;
    private Class queryclass;
    private Method[] querymethods;

    private StringBuffer sbquery = new StringBuffer();
    private String type = "";               //variable for type (and/or/not)
    private int count = 0;                  //number of children to proceed
    private Document querydoc;              //xmlQuery-Document
    private MCRQueryCondition cond;         //query-condition
    private List elList = new LinkedList(); //stack for type-elements
    private int bracket = 0;                //counts correct number of ')'
    
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
    
    
    /**
     * initialise query with xml-document containing complete query
     * @param document xml query docuement
     */
    public MCRHIBQuery(Document document){
        try{
            init(document);
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * initialise query with xml-string containing complete query
     * @param xmlString
     */
    public MCRHIBQuery(String xmlString){
        try{
            SAXBuilder builder = new SAXBuilder();
            init(builder.build(xmlString));
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * fill internal fields with values
     * @param doc document with xml query
     */
    private void init(Document doc){
        try{
            querydoc = doc;
            cond = MCRQueryParser.parse( (Element) querydoc.getRootElement().getChild("conditions").getChildren().get(0) );
            cond.accept(this);
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * constructor creates internal object for given object.
     * Method is needed for return values of queries
     * @param obj should be a MCRHIBQuery
     */
    public MCRHIBQuery(Object obj){
        queryclass = obj.getClass();
        queryobject = obj;
        querymethods = queryclass.getMethods();
    }
    
    
    /**
     * returns the object (reference on Hibernate mapping class)
     * @return
     */
    public Object getQueryObject(){
        return queryobject;
    }
    
    
    /**
     * Setter method for 'call-by-name'-use
     * (all setter methods need only one parameter)
     * @param methodname like 'setXY' as string
     * @param value value for given given parameter
     */
    public void setValue(String methodname, String value){
        Object[] values = {value};
        values[0] = value;

        for(int i = 0; i<querymethods.length; i++){
            if (querymethods[i].getName().toLowerCase().equals(methodname.toLowerCase()) && value != ""){
                try{
                    //System.out.println(querymethods[i].getParameterTypes()[0].getName());
                    if (querymethods[i].getParameterTypes()[0].getName().equals("java.sql.Date")){
                        // date format
                        values[0] = java.sql.Date.valueOf(value.split("[|]")[0]);
                    }else if(querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Integer")){
                        // integer
                        values[0] = new Integer(Integer.parseInt(value.split("[|]")[0]));
                    }else if(querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Boolean")){
                        // boolean
                        values[0] = new Boolean(value.toUpperCase());
                    }else if(querymethods[i].getParameterTypes()[0].getName().equals("java.lang.Double")){
                        try{
                            values[0] = new Double(Double.parseDouble(value.split("[|]")[0].replaceAll("[,]",".")));
                        }catch(Exception e){
                            values[0] = new Double(0);
                        }
                    }
                    
                    if (value != "" && querymethods[i].getParameterTypes()[0].getName().equals(values[0].getClass().getName())){
                        querymethods[i].invoke(queryobject, values);
                    }
                    
                }catch(Exception e){
                    System.out.println("value can't be imported: '"+ value + "'  " + methodname.substring(3));
                }
            }
        }
    }
    
 
    /**
     * Getter method for 'call-by-name'-use
     * (all getter methods return only on parameter)
     * @param methodname like 'getXY' as string
     * @return Object (String, int, ...)
     */
    public Object getValue(String methodname){
        Object ret = new Object();
        for(int i = 0; i<querymethods.length; i++){
            if (querymethods[i].getName().toLowerCase().equals(methodname.toLowerCase())){
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
     * interface implementation (visitor pattern) for condition types:
     * on each new type a xml-element will be added to an internal stack which
     * holds the number of children to process
     */
    public void visitType(Element element) {   
        
        try{
            /** update child status **/
            if (elList.size()>0){
                int intel = elList.size()-1;
                Element tmp= (Element) elList.get(intel);
                tmp.setAttribute("children", "" + (count-1) );
                elList.remove(intel);
                elList.add(intel, tmp);
            }
            sbquery.append(" (");
            bracket +=1;
            /** set new values **/
            type = element.getAttributeValue("type");
            count = Integer.parseInt(element.getAttributeValue("children"));
            elList.add(element);
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * interface implementation (visitor pattern) for field type
     */
    public void visitQuery(MCRQueryCondition entry) {
        try{
            Element el = entry.info();
            String fieldtype = MCRQueryManager.getInstance().getField(el.getAttributeValue("field")).getAttributeValue("type");
            String fieldname = el.getAttributeValue("field");
            String operator = el.getAttributeValue("operator");
            String value = el.getAttributeValue("value");
   
            /** transform values and operators **/
            if (fieldtype.equals("text") || fieldtype.equals("name") || fieldtype.equals("identifier"))
            {
                if (operator.equals("=") || operator.equals("contains"))
                {
                    operator = "like";
                }
                value = "\'%" + value + "%\'";
            }
            if (type.equals("not"))
                sbquery.append(fieldname + " not " + operator + " " + value );
            else
                sbquery.append(fieldname + " " + operator + " " + value );

            count -=1;
            if (count>=1)
                sbquery.append(" " + type + " ");

            if (count==0){
                sbquery.append(") ");
                bracket -=1;
                if (elList.size()>1)
                {
                    elList.remove(elList.size()-1);
                    type = ((Element) elList.get(elList.size()-1)).getAttributeValue("type");
                    count = Integer.parseInt(((Element) elList.get(elList.size()-1)).getAttributeValue("children"));
                    if (elList.size()>0 && count>0)
                        sbquery.append(" " + type + " ");
                }
            }
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * method uses the type definition and creates a string for the where clause
     * @return empty string for type * or no type restriction; ohterwise returns
     *         a sql where clause part to be included in the where clause
     */
    private String getTypeList(){
        StringBuffer sbtype = new StringBuffer();
        List l = querydoc.getRootElement().getChild("types").getChildren();
        if (l.size()<1) return "";
        
        for (int i=0; i<l.size(); i++){
            if (((Element)l.get(i)).getAttributeValue("field").equals("*")){
                return "";
            }
            sbtype.append("MCRTYPE = \'" + ((Element) l.get(i)).getAttributeValue("field") + "\'");
            
            if (i<l.size()-1)
                sbtype.append(" or ");
        }
        
        return sbtype.toString();
    }
    
    
    /**
     * method creates where clause for given xml query
     * @return sql where clause as string
     */
    private String getWhereClause(){
        for (int i=0; i< bracket; i++){
            sbquery.append(")");
        }
        bracket=0;
        if (getTypeList().equals("")){
            return sbquery.toString();
        }else{
           return "(" + sbquery.toString() + ") and (" +getTypeList() + ")";  
        }
    }
    
    
    /**
     * method creates order by clause for given xml query
     * @return sql order by clause as string
     */
    private String getOrderClause(){
        StringBuffer sb = new StringBuffer();
        try{
            List l = querydoc.getRootElement().getChild("sortby").getChildren();
            for(int i=0; i<l.size(); i++){
                sb.append(((Element) l.get(i)).getAttributeValue("field"));
                if (((Element) l.get(i)).getAttributeValue("order").equals("descending")){
                    sb.append(" desc ");
                }else{
                    sb.append(" asc ");
                }
                if (i<l.size()-1)
                    sb.append(", ");
            }
            return sb.toString();
        }catch(Exception e){
            LOGGER.error(e);
            return "";
        }
    }
    
    
    /**
     * method creates complete sql string for given query 
     * @return sql as string
     */
    public String getHIBQuery(){
        StringBuffer sb = new StringBuffer();
        try{
            sb.append("from ");
            sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
            sb.append(" where ");
            sb.append(getWhereClause());
            if (! getOrderClause().equals("")){
                sb.append(" order by ");
                sb.append(getOrderClause());
            }
            return sb.toString();
        }catch(Exception e){
            LOGGER.error(e);
            return "";
        }
    }
    
    
    /**
     * method returns the string reresentation of given query
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("from ");
        sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
        sb.append("\nwhere ");
        sb.append(getWhereClause());
        if (! getOrderClause().equals("")){
            sb.append("\norder by ");
            sb.append(getOrderClause());
        }
        return sb.toString();
    }
    
    

}
