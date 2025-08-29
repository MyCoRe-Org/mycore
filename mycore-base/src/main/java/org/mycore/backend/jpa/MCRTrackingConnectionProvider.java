/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import java.io.Serial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.mycore.common.MCRException;

/**
 * A {@link MCRTrackingConnectionProvider} is a {@link HikariCPConnectionProvider} that tracks
 * connections when that tracks connections when they are obtained and closed.
 * <p>
 * To track the connections, it creates a table named <code>connection_log</code>, when it is initialized.
 * When a connection is obtained, some basic information about it is written into the database.
 * When the connection is closed, that information is removed from the database.
 * In a properly working application, the table should remain almost empty.
 * <p>
 * Accumulating information about connections in the database indicates a problem with the application
 * where connections are obtained, but never closed.
 * <p>
 * <strong>Intended for development and debugging purposes only. It is not intended or recommended 
 * to use this connection provider in production for an extended time.</strong>
 */
public class MCRTrackingConnectionProvider extends HikariCPConnectionProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private final ConcurrentMap<Connection, Integer> connections = new ConcurrentHashMap<>();

    private final Object nextFreeIdLock = new Object();

    private boolean initialized;

    private int nextFreeId;

    @Override
    public void configure(Map<String, Object> props) throws HibernateException {

        super.configure(props);

        try (Connection connection = getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try {
                Statement statement = connection.createStatement();
                statement.executeUpdate("DROP TABLE IF EXISTS connection_log");
                statement.executeUpdate("CREATE TABLE connection_log ("
                    + "id INTEGER PRIMARY KEY, "
                    + "created TIMESTAMP NOT NULL, "
                    + "thread VARCHAR(256) NOT NULL, "
                    + "stack TEXT NOT NULL)");
                initialized = true;
            } catch (Exception e) {
                LOGGER.warn("Failed to init connection_log table", e);
            }
            connection.setAutoCommit(autoCommit);
        } catch (Exception e) {
            LOGGER.warn("Failed to obtain connection", e);
        }

    }

    @Override
    public Connection getConnection() throws SQLException {

        Connection connection = super.getConnection();
        int connectionId = getNextConnectionId();

        connections.merge(connection, connectionId, (oldConnectionId, newConnectionId) -> {
            LOGGER.warn("Connection reuse detected: {} is now {}", oldConnectionId, newConnectionId);
            return newConnectionId;
        });

        if (initialized) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try {
                PreparedStatement statement = connection
                    .prepareStatement("INSERT INTO connection_log (id, created, thread, stack) VALUES (?, ?, ?, ?)");
                statement.setInt(1, connectionId);
                statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                statement.setString(3, Thread.currentThread().getName());
                statement.setString(4, new MCRException("connection " + connectionId).getStackTraceAsString());
                statement.executeUpdate();
            } catch (Exception e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Failed to insert connection " + connectionId, e);
                }
            }
            connection.setAutoCommit(autoCommit);
        }

        return connection;

    }

    private int getNextConnectionId() {
        int connectionId;
        synchronized (nextFreeIdLock) {
            connectionId = nextFreeId++;
        }
        return connectionId;
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {

        Integer connectionId = connections.remove(connection);
        if (connectionId == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(new MCRException("Unknown connection returned"));
            }
        } else {

            if (initialized) {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(true);
                try {
                    PreparedStatement statement = connection
                        .prepareStatement("DELETE FROM connection_log WHERE id = ?");
                    statement.setInt(1, connectionId);
                    statement.executeUpdate();
                } catch (Exception e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Failed to remove connection " + connectionId, e);
                    }
                }
                connection.setAutoCommit(autoCommit);
            }

        }

        super.closeConnection(connection);

    }

}
