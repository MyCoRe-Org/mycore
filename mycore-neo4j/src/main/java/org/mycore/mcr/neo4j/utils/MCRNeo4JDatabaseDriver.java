/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.mcr.neo4j.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

/**
 * The MCRNeo4JDatabaseDriver class is a Java driver implementation for connecting to a Neo4j database. It provides
 * methods to establish a connection, test connection settings, and execute queries.
 *
 * @author Andreas Kluge
 */
public class MCRNeo4JDatabaseDriver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static MCRNeo4JDatabaseDriver instance = null;

    private final String url;

    private String user;

    private String password;

    private Driver driver;

    /**
    * Constructs an MCRNeo4JDatabaseDriver object with connection settings loaded from a configuration file.
    */
    private MCRNeo4JDatabaseDriver() {
        this.url = MCRConfiguration2.getString(MCRNeo4JConstants.NEO4J_CONFIG_PREFIX + "ServerURL").orElse("");
        this.user = MCRConfiguration2.getString(MCRNeo4JConstants.NEO4J_CONFIG_PREFIX + "user").orElse("");
        this.password = MCRConfiguration2.getString(MCRNeo4JConstants.NEO4J_CONFIG_PREFIX + "password").orElse("");
    }

    /**
    * Returns an instance of MCRNeo4JDatabaseDriver. If no instance exists, a new one is created.
    *
    * @return the MCRNeo4JDatabaseDriver instance
    */
    public static MCRNeo4JDatabaseDriver getInstance() {
        if (instance == null) {
            instance = new MCRNeo4JDatabaseDriver();
        }

        return instance;
    }

    /**
    * Tests the connection settings by checking if the URL, username, and password are set, if the driver is
    * initialized and submits a test query.
    *
    * @return true if the connection settings are valid, false otherwise
    */
    public boolean testConnectionSettings() {
        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            if (url.isEmpty()) {
                LOGGER.info("No database URL");
            }
            if (user.isEmpty()) {
                LOGGER.info("No user");
            }
            if (password.isEmpty()) {
                LOGGER.info("No password");
            }
            return false;
        }
        if (driver == null) {
            LOGGER.info("driver is null");
            return false;
        }

        try {
            driver.verifyConnectivity();
        } catch (Exception e) {
            LOGGER.info("Verification failed");
            return false;
        }

        try (Session session = driver.session()) {
            String queryResult = session.executeWrite(tx -> {
                Query query = new Query("RETURN '1'");
                Result result = tx.run(query);
                return result.single().get(0).asString();
            });
            LOGGER.info("Test query result is: {}", queryResult);
            return queryResult.equals("1");
        } catch (Exception e) {
            LOGGER.info("Exception: {}", e.getMessage());
        }
        return false;
    }

    /**
    * Returns the driver used for the database connection. If the driver is not initialized, a connection is created.
    *
    * @return the Neo4j driver
    */
    public Driver getDriver() {
        if (this.driver == null) {
            createConnection();
        }
        return driver;
    }

    /**
    * Creates a connection to the Neo4j database using the stored connection settings.
    */
    private void createConnection() {
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
    }

    /**
    * Creates a connection to the Neo4j database using the specified URL, username, and password.
    *
    * @param url      the URL of the Neo4j database
    * @param user     the username for the database connection
    * @param password the password for the database connection
    */
    public void createConnection(String url, String user, String password) {
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
    }
}
