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

package org.mycore.backend.cm8;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;

/**
 * This class implements a pool of database connections to IBM Content Manager
 * 8.1 Library Server. Other classes get a connection from the pool when they
 * need one and release the connection after their work has finished.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
class MCRCM8ConnectionPool {
    /** The internal list of free connections */
    protected Vector freeConnections = new Vector();

    /** The internal list of connections that are currently in use */
    protected Vector usedConnections = new Vector();

    /** The maximum number of connections that will be built */
    protected int maxNumConnections;

    /** The symbolic name of the CM 8.1 library server */
    protected String serverName;

    /** The user ID to be used for connecting to the library server */
    protected String uid;

    /** The password to be used for connecting to the library server */
    protected String password;

    /** The connection pool singleton */
    protected static MCRCM8ConnectionPool singleton;

    /** The logger */
    private static Logger logger = Logger.getLogger("org.mycore.backend.cm8");

    /**
     * Returns the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if connect to CM8 was not successful
     */
    public static synchronized MCRCM8ConnectionPool instance() {
        if (singleton == null) {
            singleton = new MCRCM8ConnectionPool();
        }

        return singleton;
    }

    /**
     * Builds the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if connect to CM8 was not successful
     */
    protected MCRCM8ConnectionPool() throws MCRPersistenceException {
        MCRConfiguration config = MCRConfiguration.instance();
        logger.info("Building Content Manager connection pool...");
        serverName = config.getString("MCR.persistence_cm8_library_server");
        uid = config.getString("MCR.persistence_cm8_user_id");
        password = config.getString("MCR.persistence_cm8_password");
        maxNumConnections = config.getInt("MCR.persistence_cm8_max_connections", 1);

        int initNumConnections = config.getInt("MCR.persistence_cm8_init_connections", maxNumConnections);

        // Build the initial number of CM8 connections
        for (int i = 0; i < initNumConnections; i++)
            freeConnections.addElement(buildConnection());

        logger.info("");
    }

    /**
     * Creates a DKDatastoreICM connection to the Content Manager library
     * server.
     * 
     * @throws MCRPersistenceException
     *             if connect to Content Manager fails
     */
    protected DKDatastoreICM buildConnection() {
        logger.info("Building one connection to Content Manager 8 ...");

        try {
            DKDatastoreICM connection = new DKDatastoreICM();
            logger.debug("Server=" + serverName + "  User=" + uid + "  PW=" + password);
            connection.connect(serverName, uid, password, "");

            return connection;
        } catch (Exception ex) {
            String msg = "Could not connect to Content Manager 8 library server";
            throw new MCRPersistenceException(msg, ex);
        }
    }

    /**
     * Gets a free connection from the pool. When this connection is not used
     * any more by the invoker, he is responsible for returning it into the pool
     * by invoking the <code>releaseConnection()</code> method.
     * 
     * @return a free connection to the Content Manager library server datastore
     * @throws MCRPersistenceException
     *             if there was a problem connecting to CM
     */
    public synchronized DKDatastoreICM getConnection() throws MCRPersistenceException {
        // Wait for a free connection
        while (usedConnections.size() == maxNumConnections)

            try {
                wait();
            } catch (InterruptedException ignored) {
            }

        DKDatastoreICM connection;

        // Do we have to build a connection or is there already one?
        if (freeConnections.isEmpty()) {
            connection = buildConnection();
        } else {
            connection = (DKDatastoreICM) (freeConnections.firstElement());
            freeConnections.removeElement(connection);
        }

        usedConnections.addElement(connection);

        return connection;
    }

    /**
     * Releases a connection, indicating that it is not used any more and should
     * be returned to to pool of free connections.
     * 
     * @param connection
     *            the Content Manager connection that has been used
     */
    public synchronized void releaseConnection(DKDatastoreICM connection) {
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
                ((DKDatastoreICM) (usedConnections.elementAt(i))).disconnect();

            for (int i = 0; i < freeConnections.size(); i++)
                ((DKDatastoreICM) (freeConnections.elementAt(i))).disconnect();
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
