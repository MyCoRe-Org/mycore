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

package org.mycore.common;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.mycore.backend.hibernate.MCRHibernateConfigHelper;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRJPABootstrapper;
import org.mycore.backend.jpa.MCRPersistenceProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;

@Deprecated(forRemoval = true)
public class MCRJPATestCase extends MCRTestCase {

    private EntityManager entityManager;

    protected Optional<EntityManager> getEntityManager() {
        return Optional.ofNullable(entityManager);
    }

    protected static void printResultSet(ResultSet resultSet, PrintStream out) throws SQLException {
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

    private static void exportSchema(String action) throws IOException {
        Map<String, Object> schemaProperties = new HashMap<>();
        schemaProperties.put("jakarta.persistence.schema-generation.database.action", action);
        try (StringWriter output = new StringWriter()) {
            if (LogManager.getLogger().isDebugEnabled()) {
                schemaProperties.put("jakarta.persistence.schema-generation.scripts.action", action);
                schemaProperties.put("jakarta.persistence.schema-generation.scripts." + action + "-target", output);
            }
            Persistence.generateSchema(getCurrentComponentName(), schemaProperties);
            LogManager.getLogger().debug(() -> "invoked '" + action + "' sql script:\n" + output);
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        String emPropertyPrefix = MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME + getCurrentComponentName();

        testProperties.put(emPropertyPrefix + ".Class", "org.mycore.backend.jpa.MCRPersistenceUnitDescriptor");

        testProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.url", "jdbc:h2:mem:" +
            getCurrentComponentName());

        testProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.driver", "org.h2.Driver");

        testProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.user", "postgres");

        testProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.password", "junit");

        testProperties.put(emPropertyPrefix + ".Properties.hibernate.default_schema", "junit");

        testProperties.put(
            emPropertyPrefix + ".Properties.hibernate.globally_quoted_identifiers_skip_column_definitions", "true");

        testProperties.put(emPropertyPrefix + ".Properties.hibernate.globally_quoted_identifiers", "true");

        return testProperties;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // Configure logging etc.
        super.setUp();
        LogManager.getLogger().debug("Setup JPA");
        MCRJPABootstrapper.initializeJPA(getCurrentComponentName());
        exportSchema();
        MCRHibernateConfigHelper
            .checkEntityManagerFactoryConfiguration(MCREntityManagerProvider.getEntityManagerFactory());
        try {
            LogManager.getLogger().debug("Prepare hibernate test", new RuntimeException());
            entityManager = MCREntityManagerProvider.getCurrentEntityManager();
            beginTransaction();
            entityManager.clear();
        } catch (RuntimeException e) {
            LogManager.getLogger().error("Error while setting up JPA JUnit test.", e);
            entityManager = null;
            throw e;
        }
    }

    public void exportSchema() throws IOException {
        doSchemaOperation(schema -> "create schema " + schema);
        exportSchema("create");
    }

    private void doSchemaOperation(Function<String, String> schemaFunction) {
        EntityManager currentEntityManager = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction transaction = currentEntityManager.getTransaction();
        try {
            transaction.begin();
            getDefaultSchema().ifPresent(
                schemaFunction
                    .andThen(currentEntityManager::createNativeQuery)
                    .andThen(Query::executeUpdate)::apply);
        } finally {
            if (transaction.isActive()) {
                if (transaction.getRollbackOnly()) {
                    transaction.rollback();
                } else {
                    transaction.commit();
                }
            }
        }
    }

    protected static Optional<String> getDefaultSchema() {
        return Optional.ofNullable(MCREntityManagerProvider
            .getEntityManagerFactory()
            .getProperties()
            .get("hibernate.default_schema"))
            .map(Object::toString)
            .map(schema -> quoteSchema() ? '"' + schema + '"' : schema);
    }

    protected static boolean quoteSchema() {
        return Optional.ofNullable(MCREntityManagerProvider
            .getEntityManagerFactory()
            .getProperties()
            .get("hibernate.globally_quoted_identifiers"))
            .map(Object::toString)
            .map(Boolean::parseBoolean)
            .orElse(Boolean.FALSE);
    }

    public void dropSchema() throws IOException {
        exportSchema("drop");
        doSchemaOperation(schema -> "drop schema " + schema);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            endTransaction();
        } finally {
            if (entityManager != null) {
                entityManager.close();
                dropSchema();
            }
            super.tearDown();
            entityManager = null;
        }
    }

    protected void beginTransaction() {
        getEntityManager().ifPresent(em -> em.getTransaction().begin());
    }

    protected void endTransaction() {
        getEntityManager().ifPresent(em -> {
            EntityTransaction tx = em.getTransaction();
            if (tx != null && tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    try {
                        tx.commit();
                    } catch (RollbackException e) {
                        if (tx.isActive()) {
                            tx.rollback();
                        }
                        throw e;
                    }
                }
            }
        });
    }

    protected void startNewTransaction() {
        endTransaction();
        beginTransaction();
        // clear from cache
        getEntityManager().ifPresent(EntityManager::clear);
    }

    protected static String getTableName(String table) {
        return getDefaultSchema().map(s -> s + ".").orElse("") + table;
    }

    protected static void printTable(String table) {
        queryTable(table, (resultSet) -> {
            try {
                printResultSet(resultSet, System.out);
            } catch (SQLException e) {
                LogManager.getLogger().warn("Error while printing result set for table " + table, e);
            }
        });
    }

    protected static void queryTable(String table, Consumer<ResultSet> consumer) {
        executeQuery("SELECT * FROM " + getTableName(table), consumer);
    }

    protected static void executeQuery(String query, Consumer<ResultSet> consumer) {
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

    protected static void executeUpdate(String sql) {
        executeUpdate(sql, (ignore) -> {
        });
    }

    protected static void executeUpdate(String sql, Consumer<Integer> consumer) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                int updateCount = statement.executeUpdate(sql);
                consumer.accept(updateCount);
            } catch (SQLException e) {
                LogManager.getLogger().warn("Error while update '" + sql + "'", e);
            }
        });
    }
}
