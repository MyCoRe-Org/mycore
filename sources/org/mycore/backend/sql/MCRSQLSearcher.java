package org.mycore.backend.sql;

import org.apache.log4j.Logger;
import org.mycore.backend.query.MCRQuerySearcherInterface;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.backend.sql.MCRSQLRowReader;
import org.mycore.common.MCRConfiguration;

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
