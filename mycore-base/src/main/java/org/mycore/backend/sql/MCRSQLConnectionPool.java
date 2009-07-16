/*
 * 
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

package org.mycore.backend.sql;

import java.sql.Connection;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * This class implements a pool of database connections to a relational database
 * like DB2, using JDBC. Other classes get a connection from the pool when they
 * need one and release the connection after their work has finished.
 * 
 * @see MCRSQLConnection
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRSQLConnectionPool {
    /** The connection pool singleton */
    protected static MCRSQLConnectionPool singleton;

    /** The internal list of free connections */
    protected Vector<MCRSQLConnection> freeConnections = new Vector<MCRSQLConnection>();

    /** The internal list of connections that are currently in use */
    protected Vector<MCRSQLConnection> usedConnections = new Vector<MCRSQLConnection>();

    /** The maximum number of connections that will be built */
    protected int maxNumConnections;

    /** The maximum number of usages of this connection * */
    static int maxUsages;

    /** The maximum age a connection can be before it is reconnected */
    static long maxAge;
    
    static String url;

    static String userID;

    static String password;

    /** The logger */
    private static Logger LOGGER = Logger.getLogger( MCRSQLConnectionPool.class );

    /**
     * Returns the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if the JDBC driver could not be loaded or initial connections
     *             could not be created
     */
    public static synchronized MCRSQLConnectionPool instance() throws MCRPersistenceException {
        if (singleton == null) {
            singleton = new MCRSQLConnectionPool();
        }

        return singleton;
    }

    /**
     * Builds the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if the JDBC driver could not be loaded
     */
    protected MCRSQLConnectionPool() throws MCRPersistenceException {
        LOGGER.info("Building JDBC connection pool...");

        String driver = null;

        LOGGER.debug("Reading database configuration from hibernate.cfg.xml");
        try {
            Element cfg = MCRURIResolver.instance().resolve("resource:hibernate.cfg.xml");
            List<Element> properties = cfg.getChild("session-factory").getChildren("property");
            for (Element property : properties) {
                String name = property.getAttributeValue("name");
                if ("connection.driver_class".equals(name))
                    driver = property.getTextNormalize();
                else if ("connection.url".equals(name))
                    url = property.getTextNormalize();
                if ("connection.username".equals(name))
                    userID = property.getTextNormalize();
                if ("connection.password".equals(name))
                    password = property.getTextNormalize();
            }
        } catch (Exception ex) {
            // Read deprecated configuration values
            LOGGER.debug("Can't read hibernate.cfg.xml, try to read property values...");
            MCRConfiguration config = MCRConfiguration.instance();
            driver = config.getString("MCR.Persistence.SQL.Driver");
            url = config.getString("MCR.Persistence.SQL.Database.URL");
            userID = config.getString("MCR.Persistence.SQL.Database.Userid", "");
            password = config.getString("MCR.Persistence.SQL.Database.Passwd", "");
        }

        LOGGER.debug("Initalize SQL connections, driver: " + driver);
        LOGGER.debug("Initalize SQL connections, url: " + url);
        LOGGER.debug("Initalize SQL connections, userID: " + userID);

        try {
            Class.forName(driver); // Load the JDBC driver
        } catch (Exception exc) {
            String msg = "Could not load JDBC driver class " + driver;
            throw new MCRPersistenceException(msg, exc);
        }

        // Some properties are NOT deprecated, as long as this class is used
        // anywhere
        MCRConfiguration config = MCRConfiguration.instance();
        maxNumConnections = config.getInt("MCR.Persistence.SQL.Connections.Max", 6);
        int initNumConnections = config.getInt("MCR.Persistence.SQL.Connections.Init", maxNumConnections);
        maxUsages = config.getInt("MCR.Persistence.SQL.Database.Connections.MaxUsages", 1000);
        maxAge = config.getLong("MCR.Persistence.SQL.Database.Connections.MaxAge", 3600 * 1000); // 1
                                                                                                    // hour

        // Build the initial number of JDBC connections
        for (int i = 0; i < initNumConnections; i++) {
            freeConnections.addElement(new MCRSQLConnection());
            LOGGER.info("Initialize a MCRSQLConnection.");
        }
    }

    /**
     * Gets a free connection from the pool. When this connection is not used
     * any more by the invoker, he is responsible for returning it into the pool
     * by invoking the <code>release()</code> method of the connection.
     * 
     * @see MCRSQLConnection#release()
     * 
     * @return a free connection for your personal use
     * @throws MCRPersistenceException
     *             if there was a problem while building the connection
     */
    public synchronized MCRSQLConnection getConnection() throws MCRPersistenceException {
        if (usedConnections.size() == maxNumConnections) {
            LOGGER.debug("waiting for database connection...");

            long maxWaitTime = 20000;
            long startTime = System.currentTimeMillis();
            long timeWaited = 0;

            do {
                try {
                    wait(maxWaitTime);
                } catch (InterruptedException ignored) {
                }
                timeWaited = System.currentTimeMillis() - startTime;
            } while ((usedConnections.size() == maxNumConnections) && (timeWaited <= maxWaitTime));

            LOGGER.debug("waited " + timeWaited + " ms for database connection");
            if (usedConnections.size() == maxNumConnections) {
                String msg = "Waited " + timeWaited + " ms, but no database connection is free for use";
                throw new MCRException(msg);
            }
        }

        MCRSQLConnection connection;

        // Do we have to build a connection or is there already one?
        if (freeConnections.isEmpty())
            connection = new MCRSQLConnection();
        else {
            connection = (MCRSQLConnection) (freeConnections.firstElement());
            freeConnections.removeElement(connection);
        }

        usedConnections.addElement(connection);
        connection.use();
        return connection;
    }

    /**
     * Releases a connection, indicating that it is not used any more and should
     * be returned to to pool of free connections. This method is invoked when
     * you call the method <code>release()</code> of the
     * <code>MCRSQLConnection</code> object.
     * 
     * @see MCRSQLConnection#release()
     * 
     * @param connection
     *            the connection that has been used
     */
    synchronized void releaseConnection(MCRSQLConnection connection) {
        if (connection == null) {
            return;
        }

        if (usedConnections.contains(connection)) {
            usedConnections.removeElement(connection);
        }

        if (!freeConnections.contains(connection)) {
            freeConnections.addElement(connection);
        }

        notifyAll();
    }

    /**
     * Finalizer, closes all connections in this connection pool
     */
    public void finalize() {
        try {
            for (int i = 0; i < usedConnections.size(); i++)
                ((Connection) (usedConnections.elementAt(i))).close();

            for (int i = 0; i < freeConnections.size(); i++)
                ((Connection) (freeConnections.elementAt(i))).close();
        } catch (Exception ignored) {
        }
    }
}
