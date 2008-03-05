/*
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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.mycore.common.MCRPersistenceException;

/**
 * Instances of this class represent a database connection to a relational
 * database like DB2. MCRSQLConnection is a wrapper around java.sql.Connection
 * and provides some convenience methods for easier SQL usage in MyCoRe. There
 * are two types of methods in this class:
 * 
 * <ul>
 * <li>Non-static methods that execute SQL statements on a certain instance of
 * MCRSQLConnection. Use these if you want to execute multiple statements within
 * a transaction</li>
 * <li>Static methods that get a free MCRSQLConnection from the pool, execute
 * the SQL statement and immediately return the connection back to the pool. Use
 * these if you want to execute just single SQL statements without the need for
 * transactions.</li>
 * </ul>
 * 
 * @see #doQuery( String)
 * @see java.sql.Connection
 * @see MCRSQLConnectionPool
 * @author Frank L\u00fctzenkirchen
 * @author Johannes Buehler
 */
public class MCRSQLConnection {
    /** The wrapped JDBC connection */
    protected Connection connection;

    protected static Logger LOGGER = Logger.getLogger(MCRSQLConnection.class);

    /** The number of usages of this connection so far * */
    private int numUsages = 0;

    /** The time this connections was last used **/
    private long lastUse;

    /**
     * Creates a new connection. This constructor is used by the connection pool
     * class.
     * 
     * @see MCRSQLConnectionPool#getConnection()
     */
    MCRSQLConnection() throws MCRPersistenceException {
        buildJDBCConnection();
        lastUse = System.currentTimeMillis();
    }

    void use() {
        long age = System.currentTimeMillis() - lastUse;
        if ((numUsages > MCRSQLConnectionPool.maxUsages) || (age > MCRSQLConnectionPool.maxAge)) {
            closeJDBCConnection();
            buildJDBCConnection();
            numUsages = 0;
        }
        numUsages++;
        lastUse = System.currentTimeMillis();
    }

    private void buildJDBCConnection() throws MCRPersistenceException {
        LOGGER.debug("MCRSQLConnection: Building connection to JDBC datastore " + MCRSQLConnectionPool.url);

        try {
            if ((MCRSQLConnectionPool.userID != null) && (MCRSQLConnectionPool.userID.trim().length() > 0)) {
                connection = DriverManager.getConnection(MCRSQLConnectionPool.url, MCRSQLConnectionPool.userID, MCRSQLConnectionPool.password);
            } else {
                connection = DriverManager.getConnection(MCRSQLConnectionPool.url);
            }
        } catch (Exception exc) {
            throw new MCRPersistenceException("Could not build JDBC connection to " + MCRSQLConnectionPool.url, exc);
        }
    }

    /**
     * Releases this connection back to the connection pool, indicating that it
     * is no longer needed by the current task.
     * 
     * @see MCRSQLConnectionPool#releaseConnection( MCRSQLConnection )
     */
    public void release() {
        MCRSQLConnectionPool.instance().releaseConnection(this);
    }

    /**
     * Closes this connection to the underlying JDBC datastore. This is called
     * when the connection pool is finalized.
     * 
     * @see MCRSQLConnectionPool#finalize()
     */
    void close() throws MCRPersistenceException {
        closeJDBCConnection();
    }

    void closeJDBCConnection() throws MCRPersistenceException {
        try {
            LOGGER.debug("MCRSQLConnection: Closing connection to JDBC datastore");
            connection.close();
        } catch (Exception exc) {
            LOGGER.warn("Exception while closing JDBC connection", exc);
        }
    }

    public void finalize() {
        closeJDBCConnection();
    }

    /**
     * Returns the underlying JDBC java.sql.Connection object
     * 
     * @return the underlying JDBC java.sql.Connection object
     */
    public Connection getJDBCConnection() {
        return connection;
    }

    /**
     * Executes an SQL select statement on this connection. The results of the
     * query are returned as MCRSQLRowReader instance.
     * 
     * @param query
     *            the SQL select statement to be executed
     * @return the MCRSQLRowReader that can be used for reading the result rows
     */
    public MCRSQLRowReader doQuery(String query) throws MCRPersistenceException {
        LOGGER.debug(query);

        try {
            ResultSet rs = connection.createStatement().executeQuery(query);

            return new MCRSQLRowReader(rs);
        } catch (Exception ex) {
            throw new MCRPersistenceException("Error while executing SQL select statement: " + query, ex);
        }
    }

    /**
     * Executes an SQL update statement on this connection.
     * 
     * @param statement
     *            the SQL create, insert or delete statement to be executed
     */
    public void doUpdate(String statement) throws MCRPersistenceException {
        LOGGER.debug(statement);

        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(statement);
            stmt.close();
        } catch (SQLException ex) {
            LOGGER.debug("MCRSQLConnection doUpdate: " + statement);
            LOGGER.debug(ex.getMessage());
            throw new MCRPersistenceException("Error while executing SQL update statement: " + statement, ex);
        }
    }

    /**
     * Executes an SQL select statement on this connection, where the expected
     * result is just a single value of a row.
     * 
     * @param query
     *            the SQL select statement to be executed
     * @return the value of the first column of the first result row as a String
     */
    public String getSingleValue(String query) throws MCRPersistenceException {
        MCRSQLRowReader r = doQuery(query);
        String value = r.next() ? r.getString(1) : null;
        r.close();

        return value;
    }

    /**
     * Executes an SQL "SELECT COUNT(*) FROM" statement on this connection,
     * returning the number of rows that match the condition.
     * 
     * @param condition
     *            the SQL select statement to be executed, beginning at the SQL
     *            "FROM" keyword
     * @return the number of matching rows, or 0 if no rows match
     */
    public int countRows(String condition) throws MCRPersistenceException {
        String query = "SELECT count(*) FROM " + condition;
        String count = getSingleValue(query);

        return ((count == null) ? 0 : Integer.parseInt(count));
    }

    /**
     * Checks if there are any matching rows for a given SQL condition by
     * executing an SQL select statement on this connection.
     * 
     * @param condition
     *            the condition of an SQL select statement to be executed,
     *            beginning at the SQL "FROM" keyword
     * @return true, if there are any rows matching this condition
     */
    public boolean exists(String condition) throws MCRPersistenceException {
        return (countRows(condition) > 0);
    }

    /**
     * Executes an SQL update statement, using any currently free connection
     * from the pool.
     * 
     * @param statement
     *            the SQL create, insert or delete statement to be executed
     */
    public static void justDoUpdate(String statement) throws MCRPersistenceException {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(statement);
        } finally {
            c.release();
        }
    }

    /**
     * Executes an SQL select statement where the expected result is just a
     * single value of a row, using any currently free connection from the pool.
     * 
     * @param query
     *            the SQL select statement to be executed
     * @return the value of the first column of the first result row as a String
     */
    public static String justGetSingleValue(String query) throws MCRPersistenceException {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            return c.getSingleValue(query);
        } finally {
            c.release();
        }
    }

    /**
     * Executes an SQL "SELECT COUNT(*) FROM" statement, returning the number of
     * rows that match the condition, using any currently free connection from
     * the pool.
     * 
     * @param condition
     *            the SQL select statement to be executed, beginning at the SQL
     *            "FROM" keyword
     * @return the number of matching rows, or 0 if no rows match
     */
    public static int justCountRows(String condition) throws MCRPersistenceException {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            return c.countRows(condition);
        } finally {
            c.release();
        }
    }

    /**
     * Checks if there are any matching rows for a given SQL condition by
     * executing an SQL select statement, using any currently free connection
     * from the pool.
     * 
     * @param condition
     *            the condition of an SQL select statement to be executed,
     *            beginning at the SQL "FROM" keyword
     * @return true, if there are any rows matching this condition
     */
    public static boolean justCheckExists(String condition) throws MCRPersistenceException {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            return c.exists(condition);
        } finally {
            c.release();
        }
    }

    /**
     * Checks existence of table
     * 
     * @param tablename
     * @throws MCRPersistenceException
     *             if the JDBC driver could not be loaded or initial connections
     *             could not be created or can not get a connection
     * @return true or false
     */
    public static boolean doesTableExist(String tablename) throws MCRPersistenceException {
        boolean ret = false;
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            String[] tableTypes = { "TABLE" };
            DatabaseMetaData dbmd = c.getJDBCConnection().getMetaData();
            ResultSet resultSet = dbmd.getTables(null, null, tablename, tableTypes);
            int recordCount = 0;

            while (resultSet.next()) {
                ++recordCount;
            }

            if (recordCount != 0) {
                ret = true;
            }

            resultSet.close();
        } catch (Exception exc) {
        } finally {
            c.release();
        }

        return ret;
    }
}
