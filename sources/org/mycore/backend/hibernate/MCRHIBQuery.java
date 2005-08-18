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

import org.apache.log4j.Logger;

/**
 * Helper class for easy use of refelction given by hibernate.
 * Getter and Setter methods will give a standard interface for saving values in
 * querytable
 * 
 * @author Arne Seifert
 *
 */
public class MCRHIBQuery {

    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRHIBIndexer.class.getName());
    
    private Object queryobject;
    private Class queryclass;
    private Method[] querymethods;
    
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

}
