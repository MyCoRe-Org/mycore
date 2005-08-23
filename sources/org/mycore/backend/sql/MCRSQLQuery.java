/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
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
 *
 **/
package org.mycore.backend.sql;

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

public class MCRSQLQuery implements MCRQueryConditionVisitor{
    
    public static Logger LOGGER = Logger.getLogger(MCRSQLQuery.class.getName());

    private StringBuffer sbquery = new StringBuffer();
    private String type = "";
    private int count = 0;
    private int closer = 0;
    private String close = "";
    private Document querydoc;
    MCRQueryCondition cond;
    
    /**
     * initialise query with xml-document containing complete query
     * @param document xml query docuement
     */
    public MCRSQLQuery(Document document){
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
    public MCRSQLQuery(String xmlString){
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
        querydoc = doc;
        cond = MCRQueryParser.parse( (Element) querydoc.getRootElement().getChild("conditions").getChildren().get(0) );
        cond.accept(this);
    }
    
    
    /**
     * interface implementation (visitor pattern) for condition types
     */
    public void visitType(Element element) {        
        type = element.getAttributeValue("type");
        count = Integer.parseInt(element.getAttributeValue("children"));
        closer += 1;
        sbquery.append("(");
        close += ")";
    }

    
    /**
     * interface implementation (visitor pattern) for field type
     */
    public void visitQuery(MCRQueryCondition entry) {
        Element el = entry.info();
        String fieldtype = MCRQueryManager.getInstance().getField(el.getAttributeValue("field")).getAttributeValue("type");
        String fieldname = el.getAttributeValue("field");
        String operator = el.getAttributeValue("operator");
        String value = el.getAttributeValue("value");
        
        /** transform values and operators **/
        if (fieldtype.equals("text") || fieldtype.equals("name") || fieldtype.equals("identifier")){
            if (operator.equals("=") || operator.equals("contains")){
                operator = "like";
            }
            value = "\'%" + value + "%\'";
        }

        sbquery.append("`"+ fieldname + "` " + operator + " " + value );
        
        if (count > 1)
            sbquery.append(" " + type + " ");
        count -= 1;
    }
    
    
    /**
     * method creates where clause for given xml query
     * @return sql where clause as string
     */
    private String getWhereClause(){
        return sbquery.toString() + close;
    }
    
    
    /**
     * method creates order by clause for given xml query
     * @return sql order by clause as string
     */
    private String getOrderClause(){
        StringBuffer sb = new StringBuffer();
        List l = querydoc.getRootElement().getChild("sortby").getChildren();
        for(int i=0; i<l.size(); i++){
            sb.append("`" + ((Element) l.get(i)).getAttributeValue("field") + "`");
            if (((Element) l.get(i)).getAttributeValue("order").equals("descending")){
                sb.append(" desc ");
            }else{
                sb.append(" asc ");
            }
            if (i<l.size()-1)
                sb.append(", ");
        }
        return sb.toString();
    }
    
    
    /**
     * method creates complete sql string for given query 
     * @return sql as string
     */
    public String getSQLQuery(){
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT MCRID FROM ");
        sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
        sb.append(" WHERE ");
        sb.append(getWhereClause());
        if (! getOrderClause().equals("")){
            sb.append(" ORDER BY ");
            sb.append(getOrderClause());
        }
        return sb.toString();
    }
    
    
    /**
     * method returns the string reresentation of given query
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT MCRID \nFROM ");
        sb.append(MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery"));
        sb.append("\nWHERE ");
        sb.append(getWhereClause());
        if (! getOrderClause().equals("")){
            sb.append("\nORDER BY ");
            sb.append(getOrderClause());
        }
        return sb.toString();
    }

}
