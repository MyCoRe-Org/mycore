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

import org.mycore.common.*;
import org.mycore.backend.hibernate.tables.MCRID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

/**
 * Class for hibernate connection to selected database
 *
 * @author  Arne Seifert
 *
 */
public class MCRHIBConnection {

    protected static Configuration cfg;
    protected static SessionFactory sessions;

    protected static MCRHIBConnection singleton;

    protected static MCRHIBMapping genTable = new MCRHIBMapping();

    private static String url, userID, password, driver;
    private static int maxUsages = Integer.MAX_VALUE;

    MCRConfiguration config = MCRConfiguration.instance();

    static {
        MCRConfiguration config = MCRConfiguration.instance();
        url = config.getString("MCR.persistence_sql_database_url");
        userID = config.getString("MCR.persistence_sql_database_userid", "");
        password = config.getString("MCR.persistence_sql_database_passwd", "");
        driver = config.getString("MCR.persistence_sql_driver", "");

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
            String fn = System.getProperty("MCR.configuration.file", "mycore.properties");
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

        String dialect;
	if(url.toLowerCase().indexOf("mysql") >= 0)
            dialect = "org.hibernate.dialect.MySQLDialect";
        else if(url.toLowerCase().indexOf("db2") >= 0)
            dialect = "org.hibernate.dialect.DB2Dialect";
        else if(url.toLowerCase().indexOf("hyper") >= 0 || url.toLowerCase().indexOf("hsql") >=0)
            dialect = "org.hibernate.dialect.HSQLDialect";
        else if(url.toLowerCase().indexOf("oracle") >= 0)
            dialect = "org.hibernate.dialect.OracleDialect"; // Oracle9Dialect
        else if(url.toLowerCase().indexOf("post") >= 0)
            dialect = "org.hibernate.dialect.PostgreSQLDialect ";
        else
            throw new MCRException("Couldn't determine database type from connection string: \""+url+"\"");

        cfg = new Configuration()
		.setProperty("hibernate.dialect", dialect)
		.setProperty("hibernate.connection.driver_class", driver)
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
     * This method returns the current session for queries
     * on the database through hibernate
     * @return Session current session object
     */
    public Session getSession(){
        return sessions.openSession();
    }

    public Configuration getConfiguration(){
        return cfg;
    }

    public long getID() {
	synchronized(singleton) {
	    Session session = getSession();
	    Transaction tx = session.beginTransaction();

	    /* generate new id */
	    MCRID id = new MCRID();
	    session.save(id);

	    /* free up previous ids in the ID table */
	    List result = session.createCriteria(MCRID.class).list();
	    int t;
	    for(t=0;t<result.size();t++) {
		MCRID lid = (MCRID)result.get(t);
		if(lid.getId() > id.getId()) { //sanity check
		    throw new IllegalStateException("bad \"native\" database ID generator");
		} else if(lid.getId() < id.getId())
		    session.delete(lid);
	    }

	    tx.commit();
	    session.close();
	    return id.getId();
	}
    }
}
