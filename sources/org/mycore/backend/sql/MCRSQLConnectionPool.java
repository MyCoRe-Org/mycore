/*
 * $RCSfile$
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;

/**
 * This class implements a pool of database connections to a relational database
 * like DB2, using JDBC. Other classes get a connection from the pool when they
 * need one and release the connection after their work has finished.
 * 
 * @see MCRSQLConnection
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRSQLConnectionPool {
    /** The connection pool singleton */
    protected static MCRSQLConnectionPool singleton;

    /** The internal list of free connections */
    protected Vector freeConnections = new Vector();

    /** The internal list of connections that are currently in use */
    protected Vector usedConnections = new Vector();

    /** The maximum number of connections that will be built */
    protected int maxNumConnections;

    /** A SQL select statement configured to test the connection works */
    protected String testStatement;

    /** The logger */
    private static Logger logger = Logger.getLogger("org.mycore.backend.sql");

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
        MCRConfiguration config = MCRConfiguration.instance();

        logger.info("Building JDBC connection pool...");
        maxNumConnections = config.getInt("MCR.persistence_sql_max_connections", 1);

        int initNumConnections = config.getInt("MCR.persistence_sql_init_connections", maxNumConnections);
        String driver = config.getString("MCR.persistence_sql_driver");
        logger.debug("MCR.persistence_sql_driver: " + driver);

        testStatement = config.getString("MCR.persistence_sql_connection_test_query", null);

        try {
            Class.forName(driver);
        } // Load the JDBC driver
        catch (Exception exc) {
            String msg = "Could not load JDBC driver class " + driver;
            throw new MCRPersistenceException(msg, exc);
        }

        // Build the initial number of JDBC connections
        for (int i = 0; i < initNumConnections; i++) {
            freeConnections.addElement(new MCRSQLConnection());
            logger.info("Initialize a MCRSQLConnection.");
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
        logger.debug("getConnection(), currently " + usedConnections.size() + " used, " + freeConnections.size() + " free");
        // Wait for a free connection

        int waitCount = 0, maxWaitCount = 20;
        while ((usedConnections.size() == maxNumConnections) && (waitCount < maxWaitCount))
            waitCount++;
        logger.debug("All connections in use, waiting for a free connection, try # " + waitCount);
        try {
            wait(1000);
        } catch (InterruptedException ignored) {
        }
        if (waitCount == maxWaitCount) {
            String msg = "Waited a long time, but no database connection is free for use";
            throw new MCRException(msg);
        }

        MCRSQLConnection connection;

        // Do we have to build a connection or is there already one?
        if (freeConnections.isEmpty())
            connection = new MCRSQLConnection();
        else {
            connection = (MCRSQLConnection) (freeConnections.firstElement());
            freeConnections.removeElement(connection);

            // Ensure connection still works
            if (testStatement != null) {
                try {
                    Statement st = connection.getJDBCConnection().createStatement();
                    ResultSet rs = st.executeQuery(testStatement);
                    rs.close();
                    st.close();
                } catch (SQLException ex) {
                    logger.debug("Error while checking existing connection:" + ex.getMessage(), ex);
                    logger.debug("Connection may be closed, trying to create a new one.");
                    try {
                        connection.close();
                    } catch (Exception ignored) {
                    }
                    connection = new MCRSQLConnection();
                }
            }
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

        logger.debug("releaseConnection(), currently " + usedConnections.size() + " used, " + freeConnections.size() + " free");
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

    /**
     * The method return the logger for org.mycore.backend.cm8 .
     * 
     * @return the logger.
     */
    static final Logger getLogger() {
        return logger;
    }
}
