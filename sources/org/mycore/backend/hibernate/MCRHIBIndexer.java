package org.mycore.backend.hibernate;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.jdom.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.MCRHIBMapping;
import org.mycore.backend.hibernate.MCRTableGenerator;
import org.mycore.backend.query.MCRQueryIndexerInterface;
import org.mycore.backend.query.MCRQueryManager;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRHIBIndexer implements MCRQueryIndexerInterface {

    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRHIBIndexer.class.getName());
    
    protected static MCRHIBMapping genTable = new MCRHIBMapping();
    protected static MCRHIBConnection hibconnection = MCRHIBConnection.instance();
    
    private static String SQLQueryTable = "";
    private static String querytypes = "";
    private static MCRConfiguration config;
    private static boolean updated = false;


    protected MCRQueryManager queryManager;
    
    public MCRHIBIndexer() {
        config = MCRConfiguration.instance();
        SQLQueryTable = config.getString("MCR.QueryTableName", "MCRQuery");
        querytypes = config.getString("MCR.QueryTypes", "document,derivate,author");
        LOGGER.info("indexer loaded");
    }
    
    /**
     * method loads all searchfield values into database after clearing old values.
     * needs to be done after changes in the fielddefinition 
     */
    public void initialLoad() {
        queryManager = MCRQueryManager.getInstance();       
        StringTokenizer tokenizer = new StringTokenizer(querytypes,",");
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            session.createQuery("delete from MCRQuery").executeUpdate(); 
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            LOGGER.error(e);
        }finally{
            session.close();
        }
        
        while ( tokenizer.hasMoreTokens() )
            queryManager.loadType(tokenizer.nextToken());
    }

    
    /**
     * method updates all search values for a single object
     * @param objectid identifier for single object
     */
    public void updateObject(MCRObjectID objectid) {
        try{
            MCRQueryManager.getInstance().insertObject(objectid);
        }catch(Exception e){
            LOGGER.error(e);
        }

    }

    /**
     * method deletes all values for given objectid
     * @param objectid identifier for single object
     */
    public void deleteObject(MCRObjectID objectid) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            MCRHIBQuery query = new MCRHIBQuery();
            query.setValue("setMcrid", objectid.getId());
            session.delete(query);
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            LOGGER.error(e);
        }finally{
            session.close();
        }
        
    }
    
    /**
     * internal helper method
     * @param mcrid identifier for object
     * @param values list of values to be indexed vor search. list contains values for each field
     */
    public final void insertInQuery(String mcrid, List values){
        
        MCRHIBQuery query = new MCRHIBQuery();

        query.setValue("setmcrid", mcrid);
        Iterator it = queryManager.getQueryFields().keySet().iterator();
        for(int i=0; i< values.size(); i++){
            Element el = queryManager.getField((String) it.next());
            try{
                if(values.get(i) != null){
                   query.setValue("set" + el.getAttributeValue("name"), (String) values.get(i) );                    
                }
            }catch(Exception e){
                LOGGER.error(e);
            }
        }
        
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try {
            session.saveOrUpdate( query.getQueryObject());
            tx.commit();   
        }catch(Exception e){
            LOGGER.error(e);
            tx.rollback();
        } finally {
            session.close();
        }

    }
    
    /**
     * method updates the hibernate configuration and introduces the new table for searching
     */
    public void updateConfiguration() {
        try {
            if(! updated){
                // update schema -> first time create table
                Configuration cfg = hibconnection.getConfiguration();
                MCRTableGenerator map = new MCRTableGenerator(SQLQueryTable, "org.mycore.backend.query.MCRQuery", "", 1);
                
                Iterator it = MCRQueryManager.getInstance().getQueryFields().keySet().iterator();
                map.addIDColumn("mcrid", "MCRID", new StringType(), 64, "assigned", false);
                while (it.hasNext()){
                    Element el = (Element) MCRQueryManager.getInstance().getQueryFields().get((String) it.next());              
                    map.addColumn(el.getAttributeValue("name"),
                            el.getAttributeValue("name"),
                            getHibType(el.getAttributeValue("type")),
                            2147483647, false, false, false);
                }
                
                cfg.addXML(map.getTableXML());
                cfg.createMappings();
                hibconnection.buildSessionFactory(cfg);
                updated = true;
                
                new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);
            }
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    /**
     * internal helper mehtod: translates fieldtypes into hibernate types
     * @param type typename as string
     * @return hibernate type
     */
    private static org.hibernate.type.Type getHibType(String type){
        
        if(type.equals("integer")){
            return new IntegerType();
        }else if(type.equals("date")){
            return new DateType();
        }else if(type.equals("time")){
            return new TimeType();
        }else if(type.equals("timestamp")){
            return new TimestampType();
        }else if(type.equals("decimal")){
            return new DoubleType();
        }else if(type.equals("boolean")){
            return new BooleanType();
        }else{
            return new StringType();
        }

    }

}
