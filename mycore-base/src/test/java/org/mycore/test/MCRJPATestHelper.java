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

package org.mycore.test;

import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.Table;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

/**
 * Helper class for JPA-related testing in the MyCoRe framework.
 * <p>
 * This utility class provides methods for database interaction during tests,
 * including result set printing, schema handling, and SQL execution helpers.
 * It is designed to work alongside the {@link MCRJPAExtension} for JPA testing.
 */
public class MCRJPATestHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Prints a ResultSet to the specified PrintStream in a tabular format.
     *
     * @param resultSet the ResultSet to print
     * @param out the PrintStream to print to (e.g., System.out)
     * @throws SQLException if a database access error occurs
     */
    public static void printResultSet(ResultSet resultSet, PrintStream out) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columns = metaData.getColumnCount();
        Table t = new Table(columns);
        for (int i = 1; i <= columns; i++) {
            t.addValue(metaData.getColumnName(i));
        }
        while (resultSet.next()) {
            for (int i = 1; i <= columns; i++) {
                String value = resultSet.getString(i);
                t.addValue(value != null ? value : "null");
            }
        }
        t.print(out);
    }

    /**
     * Gets the default schema name from Hibernate properties.
     * <p>
     * The schema name will be quoted if Hibernate's globally_quoted_identifiers is enabled.
     *
     * @return an Optional containing the schema name or empty if none is defined
     */
    public static Optional<String> getDefaultSchema() {
        return Optional.ofNullable(MCREntityManagerProvider
            .getEntityManagerFactory()
            .getProperties()
            .get("hibernate.default_schema"))
            .map(Object::toString)
            .map(schema -> shouldQuoteSchemaIdentifieres() ? '"' + schema + '"' : schema);
    }

    /**
     * Determines whether schema identifiers should be quoted based on Hibernate configuration.
     *
     * @return true if schema identifiers should be quoted, false otherwise
     */
    private static boolean shouldQuoteSchemaIdentifieres() {
        return Optional.ofNullable(MCREntityManagerProvider
            .getEntityManagerFactory()
            .getProperties()
            .get("hibernate.globally_quoted_identifiers"))
            .map(Object::toString)
            .map(Boolean::parseBoolean)
            .orElse(Boolean.FALSE);
    }

    /**
     * Quotes a schema identifier if needed based on JPA configuration.
     *
     * @param identifier the schema identifier to quote, for example a column name
     * @return the quoted schema identifier if needed
     */
    public static String quoteSchemaIdentifierIfNeeded(String identifier) {
        return shouldQuoteSchemaIdentifieres() ? '"' + identifier + '"' : identifier;
    }

    /**
     * Gets the fully qualified table name including schema if available.
     *
     * @param table the base table name
     * @return the fully qualified table name with schema if available
     */
    public static String getTableName(String table) {
        String tableName = shouldQuoteSchemaIdentifieres() ? '"' + table + '"' : table;
        return getDefaultSchema().map(s -> s + ".").orElse("") + tableName;
    }

    /**
     * Prints the contents of a database table to System.out.
     *
     * @param table the name of the table to print
     */
    public static void printTable(String table) {
        queryTable(table, (resultSet) -> {
            try {
                printResultSet(resultSet, System.out);
            } catch (SQLException e) {
                LogManager.getLogger().warn("Error while printing result set for table " + table, e);
            }
        });
    }

    public static void queryTable(String table, Consumer<ResultSet> consumer) {
        executeQuery("SELECT * FROM " + getTableName(table), consumer);
    }

    private static void executeQuery(String query, Consumer<ResultSet> consumer) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                consumer.accept(resultSet);
            } catch (SQLException e) {
                LogManager.getLogger().warn("Error while querying '" + query + "'", e);
            }
        });
    }

    /**
     * Executes a SQL update statement without processing the result.
     *
     * @param sql the SQL update statement to execute
     */
    public static void executeUpdate(String sql, Object... args) {
        executeUpdate(sql, (ignore) -> {
        }, args);
    }

    /**
     * Executes a SQL update statement and processes the update count with the provided consumer.
     *
     * @param sql the SQL update statement to execute
     * @param consumer a consumer function that processes the update count
     */
    public static void executeUpdate(String sql, Consumer<Integer> consumer, Object... args) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    statement.setObject(i + 1, args[i]);
                }
                int updateCount = statement.executeUpdate();
                consumer.accept(updateCount);
            } catch (SQLException e) {
                LogManager.getLogger().warn(() -> "Error while update '" + sql + "'", e);
            }
        });
    }

    /**
     * Begins a new transaction on the current EntityManager.
     */
    public static void beginTransaction() {
        MCREntityManagerProvider.getCurrentEntityManager().getTransaction().begin();
    }

    /**
     * Ends the current transaction on the current EntityManager.
     * <p>
     * If the transaction is marked for rollback, it will be rolled back.
     * If the transaction is active, it will be committed.
     * If the commit fails, the transaction will be rolled back.
     *
     * @throws RollbackException if the transaction commit fails
     */
    public static void endTransaction() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction tx = em.getTransaction();
        if (tx != null && tx.isActive()) {
            if (tx.getRollbackOnly()) {
                tx.rollback();
                LOGGER.debug("Transaction rolled back (was marked for rollback)");
            } else {
                try {
                    tx.commit();
                    LOGGER.debug("Transaction committed successfully");
                } catch (RollbackException e) {
                    LOGGER.error("Transaction commit failed", e);
                    if (tx.isActive()) {
                        try {
                            tx.rollback();
                        } catch (Exception ex) {
                            LOGGER.error("Secondary rollback attempt failed", ex);
                        }
                    }
                    throw e;
                }
            }
        }
    }

    /**
     * Starts a new transaction and clears the current EntityManager cache.
     */
    public static void startNewTransaction() {
        endTransaction();
        beginTransaction();
        // clear from cache
        MCREntityManagerProvider.getCurrentEntityManager().clear();
    }

}
