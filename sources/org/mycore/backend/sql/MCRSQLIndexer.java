/**
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

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.backend.query.MCRQueryIndexerInterface;
import org.mycore.backend.query.MCRQueryManager;
import org.mycore.backend.sql.MCRSQLColumn;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.backend.sql.MCRSQLStatement;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * 
 * @author Arne Seifert
 *
 */

public class MCRSQLIndexer implements MCRQueryIndexerInterface{
   
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRSQLIndexer.class.getName());
    
    private static String SQLQueryTable = "";
    private static String querytypes = "";
    private static MCRConfiguration config;
    
    protected MCRQueryManager queryManager;

    public MCRSQLIndexer() {
            config = MCRConfiguration.instance();
            SQLQueryTable = config.getString("MCR.QueryTableName", "MCRQuery");
            querytypes = config.getString("MCR.QueryTypes", "document,author");
            LOGGER.info("indexer loaded");
    }
    
    
    /**
     * method loads all searchfield values into database after clearing old values.
     * needs to be done after changes in the fielddefinition 
     */
    public void initialLoad(){
        queryManager = MCRQueryManager.getInstance();
        StringTokenizer tokenizer = new StringTokenizer(querytypes,",");
        createSQLQueryTable();
        while ( tokenizer.hasMoreTokens() )
            queryManager.loadType(tokenizer.nextToken());
    }
    
    
    /**
     * method to update entries of given objectid
     * @param objectid as MCRObjectID
     */
    public void updateObject(MCRObjectID objectid){
        deleteObject(objectid);
        MCRObject obj = null;
        obj.receiveFromDatastore(objectid.getId());
        MCRQueryManager.getInstance().create(obj);
    }
    
    
    /**
     * method to delete all entries of given objectid
     * @param objectid as MCRObjectID
     */
    public void deleteObject(MCRObjectID objectid){
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable)
            .setCondition("MCRID", objectid.getId());
        try {
            c.doUpdate(query.toDeleteStatement());
        }catch(Exception e){
            LOGGER.error(e);
        } finally {
            c.release();
        }
    }
   

    /**
     * internal helper method
     * @param mcrid identifier for object
     * @param values list of values to be indexed vor search. list contains values for each field
     */
    public final void insertInQuery(String mcrid, List values){
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable);
        
        query.setValue(new MCRSQLColumn("MCRID", mcrid, "string"));
        query.setValue(new MCRSQLColumn("MCRTYPE", new MCRObjectID(mcrid).getTypeId(), "string"));
        
        Iterator it = queryManager.getQueryFields().keySet().iterator();
        for(int i=0; i< values.size(); i++){
            String field = (String) it.next();
            String value = (String) values.get(i);
            if(value != ""){
                Element el = (Element) queryManager.getQueryFields().get(field);
                String type = el.getAttributeValue("type");
                if (type.equals("text") || type.equals("name") || type.equals("identifier")){
                    query.setValue(new MCRSQLColumn(field,value.replaceAll("\'","''"),"string"));
                }else if(type.equals("date")){
                    query.setValue( new MCRSQLColumn(field, value.replaceAll("\'","''"),"string"));
                }else if(type.equals("integer")){                 
                    query.setValue( new MCRSQLColumn(field, "" + Integer.parseInt(value.split("[|]")[0]),"integer"));
                }else if(type.equals("decimal")){
                    query.setValue( new MCRSQLColumn(field, "" + value.split("[|]")[0],"decimal"));
                }else if(type.equals("boolean")){
                    query.setValue( new MCRSQLColumn(field, "" + value.split("[|]")[0],"boolean"));
                }
            }
        }
        try {
            c.doUpdate(query.toTypedInsertStatement());
        }catch(Exception e){
            LOGGER.error("e: "+e);
            e.printStackTrace();
        } finally {
            c.release();
        }
    }
    
    /**
     * This method creates the table named SQLQueryTable.
     */
    private void createSQLQueryTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        try {
            MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable);
            query.addColumn("MCRID VARCHAR(64) NOT NULL");
            query.addColumn("MCRTYPE VARCHAR(64) NOT NULL");
            Iterator it = MCRQueryManager.getInstance().getQueryFields().keySet().iterator();
            
            while (it.hasNext()){
                
                query.addColumn(addcolumn((Element) MCRQueryManager.getInstance().getQueryFields().get((String) it.next())));
            }
            
            if (MCRSQLConnection.doesTableExist(SQLQueryTable)) {
                System.out.println("table exists -> will be dropped");
                dropSQLQueryTable();
            }
            c.doUpdate(query.toCreateTableStatement());  
            c.doUpdate(new MCRSQLStatement(SQLQueryTable)
                    .addColumn("MCRID")
                    .toIndexStatement());
        }catch(Exception e){
            LOGGER.error("Fehler", e);
        } finally {
            c.release();
        }
    }
    
    /**
     * internal helper method
     * @param el jdom-element with definition of searchfield
     * @return formated sql-string for table generation
     */
    private String addcolumn(Element el){
        StringBuffer sbRet = new StringBuffer().append("`" + el.getAttributeValue("name").toUpperCase() + "` ");
        String type = el.getAttributeValue("type").toLowerCase();
        if (type.equals("text") || type.equals("name") || type.equals("identifier")){
            sbRet.append("TEXT");
        }else if(type.equals("date")){
            sbRet.append("DATE");
        }else if(type.equals("time")){
            sbRet.append("TIME");
        }else if(type.equals("timestamp")){
            sbRet.append("TIMESTAMP");   
        }else if(type.equals("integer")){
            sbRet.append("INT");
        }else if(type.equals("decimal")){
            sbRet.append("DOUBLE");
        }else if(type.equals("boolean")){
            sbRet.append("smallint");
        }
        return sbRet.toString();
    }
    
    /**
     * This method drops the table named SQLQueryTable.
     */
    private final void dropSQLQueryTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        try {
            if (MCRSQLConnection.doesTableExist(SQLQueryTable)) {
                c.doUpdate("drop table " + SQLQueryTable);
            }
        } finally {
            c.release();
        }
    }

    /**
     * method for initialisation - in SQL not needed
     *  - DO NOT DELETE -
     */
    public void updateConfiguration() {
        // not needed -do not delete
    }
    
}
