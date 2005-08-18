package org.mycore.backend.sql;

import org.apache.log4j.Logger;
import org.mycore.backend.query.MCRQuerySearcherInterface;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.backend.sql.MCRSQLRowReader;
import org.mycore.common.MCRConfiguration;

public class MCRSQLSearcher implements MCRQuerySearcherInterface{
    
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRSQLSearcher.class.getName());
    
    private static String SQLQueryTable; 
    private static MCRConfiguration config;
    
    public MCRSQLSearcher(){
        config = MCRConfiguration.instance();
        SQLQueryTable = config.getString("MCR.QueryTableName", "MCRQuery");
    }
    
    public void runQuery(int no){
        String query = "SELECT * FROM " + SQLQueryTable;
        
        if (no == 0){
            query += " WHERE `TITLE` like '%Ein%' AND `AUTHOR` like '%Jens Kupferschmidt%'";
        }else if(no==1){
            query += " WHERE `AUTHOR` like '%Jens Kupferschmidt%'";
        }else if(no==2){
            query += " WHERE `TITLE` like '%Ein%'";
        }else if(no==3){
            query += " WHERE `TITLE` like '%Ein%' AND `AUTHOR` like '%Heiko Helmbrecht%'";
        }
        
        System.out.println(query);

        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        
        try {
            MCRSQLRowReader reader;
            
            reader = c.doQuery(query);
            while (reader.next()){
                LOGGER.info("part: " + reader.getString("MCRID"));
                System.out.println("ID: " + reader.getString("MCRID") + " author: " + reader.getString("AUTHOR"));
            }
            
        }catch(Exception e){
            LOGGER.error(e);
        }
    }

}
