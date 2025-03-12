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

import static org.mycore.test.MCRJPATestHelper.beginTransaction;
import static org.mycore.test.MCRJPATestHelper.endTransaction;
import static org.mycore.test.MCRJPATestHelper.getDefaultSchema;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.backend.hibernate.MCRHibernateConfigHelper;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRJPABootstrapper;
import org.mycore.backend.jpa.MCRPersistenceProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

/**
 * JUnit 5 extension for JPA testing in the MyCoRe framework.
 * <p>
 * This extension handles the setup and teardown of JPA entities and database transactions
 * for testing. It creates an in-memory H2 database, sets up the schema, manages transactions,
 * and cleans up after tests.
 * <p>
 * Use this extension with {@code @ExtendWith(MCRJPAExtension.class)} on your test class.
 */
public class MCRJPAExtension
    implements Extension, BeforeEachCallback, AfterEachCallback, AfterAllCallback, BeforeAllCallback {
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MCRJPAExtension.class);
    private static final String SCHEMA_INITIALIZED = "schemaInitialized";
    private static final Logger LOGGER = LogManager.getLogger();
    //name of current component or application module under test
    private String componentName;
    private EntityManager entityManager;

    public MCRJPAExtension() {
        this.componentName = MCRTestExtensionConfigurationHelper.detectCurrentComponentName();
    }

    /**
     * Initializes JPA configuration before all tests in the class are executed.
     * Sets up H2 in-memory database properties for the current component.
     *
     * @param context the extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        addJPAProperties(MCRTestExtension.getClassProperties(context));
    }

    private void addJPAProperties(Map<String, String> classProperties) {
        String emPropertyPrefix = MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME + componentName;
        classProperties.put(emPropertyPrefix + ".Class", "org.mycore.backend.jpa.MCRPersistenceUnitDescriptor");
        classProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.url", "jdbc:h2:mem:" +
            componentName);
        classProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.driver", "org.h2.Driver");
        classProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.user", "postgres");
        classProperties.put(emPropertyPrefix + ".Properties.jakarta.persistence.jdbc.password", "junit");
        classProperties.put(emPropertyPrefix + ".Properties.hibernate.default_schema", "junit");
        classProperties.put(
            emPropertyPrefix + ".Properties.hibernate.globally_quoted_identifiers_skip_column_definitions", "true");
        classProperties.put(emPropertyPrefix + ".Properties.hibernate.globally_quoted_identifiers", "true");
    }

    /**
     * Cleans up after all tests in the class have been executed.
     * Drops the schema unless this is a nested test class.
     *
     * @param context the extension context
     * @throws IOException if cleanup fails
     */
    @Override
    public void afterAll(ExtensionContext context) throws IOException {
        if (!MCRJunit5ExtensionHelper.isNestedTestClass(context)) {
            dropSchema();
            context.getRoot().getStore(NAMESPACE).put(SCHEMA_INITIALIZED, Boolean.FALSE);
        }
    }

    /**
     * Cleans up after each test method.
     * Ends the transaction, truncates all tables, and closes the EntityManager.
     *
     * @param context the extension context
     */
    @Override
    public void afterEach(ExtensionContext context) {
        try {
            endTransaction();
        } finally {
            if (entityManager != null) {
                truncateAllTables();
                entityManager.close();
            }
        }
    }

    /**
     * Prepares the test environment before each test method.
     * Initializes JPA if needed and begins a new transaction.
     *
     * @param context the extension context
     * @throws IOException if preparation fails
     */
    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        if (!context.getStore(NAMESPACE).getOrDefault(SCHEMA_INITIALIZED, Boolean.class, Boolean.FALSE)
            || MCREntityManagerProvider.getEntityManagerFactory() == null) {
            LogManager.getLogger().info("Setup JPA");
            MCRJPABootstrapper.initializeJPA(componentName);
            exportSchema();
            MCRHibernateConfigHelper
                .checkEntityManagerFactoryConfiguration(MCREntityManagerProvider.getEntityManagerFactory());
            context.getRoot().getStore(NAMESPACE).put(SCHEMA_INITIALIZED, Boolean.TRUE);
        }

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

    protected Optional<EntityManager> getEntityManager() {
        return Optional.ofNullable(entityManager);
    }

    /**
     * Truncates all tables in the current schema to clean the database state.
     * Temporarily disables referential integrity to allow truncating tables with foreign keys.
     */
    protected void truncateAllTables() {
        LOGGER.debug("Truncate all tables");
        entityManager.unwrap(Session.class).getSessionFactory().getSchemaManager().truncateMappedObjects();
    }

    /**
     * Executes schema generation with the specified action.
     *
     * @param action the schema generation action ("create" or "drop")
     * @throws IOException if schema generation fails
     */
    private void generateSchema(String action) throws IOException {
        Map<String, Object> schemaProperties = new HashMap<>();
        schemaProperties.put("jakarta.persistence.schema-generation.database.action", action);
        try (StringWriter output = new StringWriter()) {
            if (LogManager.getLogger().isDebugEnabled()) {
                schemaProperties.put("jakarta.persistence.schema-generation.scripts.action", action);
                schemaProperties.put("jakarta.persistence.schema-generation.scripts." + action + "-target", output);
            }
            Persistence.generateSchema(componentName, schemaProperties);
            LogManager.getLogger().debug(() -> "invoked '" + action + "' sql script:\n" + output);
        }
    }

    /**
     * Creates the database schema for testing.
     *
     * @throws IOException if schema creation fails
     */
    public void exportSchema() throws IOException {
        executeSQLSchemaOperation(schema -> "create schema " + schema);
        generateSchema("create");
    }

    /**
     * Executes a schema operation like creating or dropping a schema.
     *
     * @param schemaFunction a function that takes a schema name and returns an SQL statement
     */
    private void executeSQLSchemaOperation(Function<String, String> schemaFunction) {
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

    /**
     * Drops the database schema.
     *
     * @throws IOException if schema dropping fails
     */
    public void dropSchema() throws IOException {
        generateSchema("drop");
        executeSQLSchemaOperation(schema -> "drop schema " + schema);
    }

}
