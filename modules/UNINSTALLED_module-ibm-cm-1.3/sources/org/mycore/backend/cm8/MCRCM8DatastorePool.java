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

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

import com.ibm.mm.sdk.common.DKDatastorePool;
import com.ibm.mm.sdk.common.dkDatastore;
import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * Provides functionality to access dkDatastore instances. Every class acquiring
 * dkDatastores needs to release it after use.
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCM8DatastorePool implements Closeable {
    /** The user ID to be used for connecting to the library server */
    protected String uid;

    /** The password to be used for connecting to the library server */
    protected String password;

    /** The connection pool singleton */
    protected static MCRCM8DatastorePool singleton;

    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(MCRCM8DatastorePool.class);

    private static DKDatastorePool CM_CONNECTION_POOL;

    /**
     * Returns the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if connect to CM8 was not successful
     */
    public static synchronized MCRCM8DatastorePool instance() {
        if (singleton == null) {
            singleton = new MCRCM8DatastorePool();
        }

        return singleton;
    }

    /**
     * Builds the connection pool singleton.
     * 
     * @throws MCRPersistenceException
     *             if connect to CM8 was not successful
     */
    protected MCRCM8DatastorePool() throws MCRPersistenceException {
        final MCRConfiguration config = MCRConfiguration.instance();
        LOGGER.info("Building Content Manager connection pool...");
        final String serverName = config.getString("MCR.persistence_cm8_library_server");
        uid = config.getString("MCR.persistence_cm8_user_id");
        password = config.getString("MCR.persistence_cm8_password");
        final int maxNumConnections = config.getInt("MCR.persistence_cm8_max_connections", 1);

        final int initNumConnections = config.getInt("MCR.persistence_cm8_init_connections", maxNumConnections);

        try {
            LOGGER.info("Initiate CM Connection Pool.");
            CM_CONNECTION_POOL = new DKDatastorePool(DKDatastoreICM.class.getName());
            CM_CONNECTION_POOL.setMaxPoolSize(maxNumConnections);
            CM_CONNECTION_POOL.setMinPoolSize(initNumConnections);
            CM_CONNECTION_POOL.setDatastoreName(serverName);
            final String dbSchema = config.getString("MCR.persistence_cm8_schema", uid);
            CM_CONNECTION_POOL.setConnectString("SCHEMA=" + dbSchema);
        } catch (final Exception e) {
            LOGGER.fatal("Could not initilize connection to IBM CM.", e);
        }
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    /**
     * Gets a free dkDatastore from the pool. When this connection is not used
     * any more by the invoker, he is responsible for returning it into the pool
     * by invoking the <code>releaseDatastore()</code> method.
     * 
     * @return a free dkDatastore to the Content Manager library server
     * @throws MCRPersistenceException
     *             if there was a problem connecting to CM
     */
    public dkDatastore getDatastore() throws MCRPersistenceException {
        try {
            LOGGER.debug("CONFIG: " + CM_CONNECTION_POOL.getConfigurationString());
            LOGGER.debug("CONNECT: " + CM_CONNECTION_POOL.getConnectString());
            return CM_CONNECTION_POOL.getConnection(uid, password);
        } catch (final Exception e) {
            throw new MCRPersistenceException("Error while connecting to IBM CM.", e);
        }
    }

    /**
     * Releases a dkDatastore, indicating that it is not used any more and
     * should be returned to to pool of free connections.
     * 
     * @param datastore
     *            the Content Manager dkDatastore that has been used
     */
    public void releaseDatastore(final dkDatastore datastore) {
        if (datastore == null) {
            return;
        }
        try {
            CM_CONNECTION_POOL.returnConnection(datastore);
        } catch (final Exception e) {
            LOGGER.error("Error while returning dkDatastore to pool.", e);
        }
    }

    public void close() {
        try {
            LOGGER.debug("Closing CM Connection Pool");
            CM_CONNECTION_POOL.clearConnections();
            CM_CONNECTION_POOL.destroy();
        } catch (final Exception e) {
            LOGGER.error("Error while closing CM Connection Pool.", e);
        }
    }

}
