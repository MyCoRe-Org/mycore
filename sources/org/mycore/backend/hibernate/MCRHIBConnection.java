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

package org.mycore.backend.hibernate;

import java.util.List;

import org.mycore.common.*;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Class for hibernate connection to selected database 
 * 
 * @author  Arne Seifert
 *
 */
public class MCRHIBConnection {

    protected static Configuration cfg;	
    protected static SessionFactory sessions;
    protected static Session session;
    
    protected static MCRHIBConnection singleton;
    
    protected static MCRHIBMapping genTable = new MCRHIBMapping();
    
    private static String url, userID, password;
	private static int maxUsages = Integer.MAX_VALUE;
    
    private List typeList;
    MCRConfiguration config = MCRConfiguration.instance();
    
    static {
        MCRConfiguration config = MCRConfiguration.instance();
        url = config.getString("MCR.persistence_sql_database_url");
        userID = config.getString("MCR.persistence_sql_database_userid", "");
        password = config.getString("MCR.persistence_sql_database_passwd", "");
        
        maxUsages = config.getInt(
                "MCR.persistence_sql_database_connection_max_usages",
                Integer.MAX_VALUE);
    }
   
    public static synchronized MCRHIBConnection instance() throws MCRPersistenceException{
        if (singleton == null){
            singleton = new MCRHIBConnection();
        }
        return singleton;  
    }
    
    /**
     * This method initializes the connection to the database
     * 
     * @throws MCRPersistenceException
     */
    protected MCRHIBConnection() throws MCRPersistenceException {
        try{
            buildConfiguration();
            genTable.generateTables(cfg);
            buildSessionFactory();
            
        }catch (Exception exc) {
            String msg = "Could not connect to database";
            throw new MCRPersistenceException(msg, exc);
        }
    }
    
    /**
     * This method creates the configuration needed by hibernate
     */
    private void buildConfiguration(){
        cfg = new Configuration()

		.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
		.setProperty("hibernate.connection.driver_class","com.mysql.jdbc.Driver")
		.setProperty("hibernate.connection.url", url)
		.setProperty("hibernate.connection.username", userID)
		.setProperty("hibernate.connection.password", password)
		.setProperty("hibernate.connection.pool_size", ""+maxUsages)
		.setProperty("hibernate.show_sql", "false");
    }
    
    /**
     * This method creates the SessionFactory for hiberante
     */
    private static void buildSessionFactory(){
        if (sessions == null){
            sessions = cfg.buildSessionFactory();
        }
    }
    
    /**
     * This method creates the Session on the current SessionFactory
     */
    private static void buildSession(){
        
        session = sessions.openSession();
    }
    
    /**
     * This method returns the current session for queries
     * on the database through hibernate
     * @return Session current session object
     */
    public static Session getSession(){
        return session;
    }
	
}