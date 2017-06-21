package org.mycore.common;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.RollbackException;

import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.mycore.backend.hibernate.MCRHibernateConfigHelper;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRJPABootstrapper;

public class MCRJPATestCase extends MCRTestCase {

    protected Optional<EntityManager> entityManager;

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
        schemaProperties.put("javax.persistence.schema-generation.database.action", action);
        try (StringWriter output = new StringWriter()) {
            if (LogManager.getLogger().isDebugEnabled()) {
                schemaProperties.put("javax.persistence.schema-generation.scripts.action", action);
                schemaProperties.put("javax.persistence.schema-generation.scripts." + action + "-target", output);
            }
            Persistence.generateSchema(getCurrentComponentName(), schemaProperties);
            LogManager.getLogger().debug(() -> "invoked '" + action + "' sql script:\n" + output.toString());
        }
    }

    @Before()
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
            entityManager = Optional.of(MCREntityManagerProvider.getCurrentEntityManager());
            beginTransaction();
            entityManager.get().clear();
        } catch (RuntimeException e) {
            LogManager.getLogger().error("Error while setting up JPA JUnit test.", e);
            entityManager = Optional.empty();
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

    protected Optional<String> getDefaultSchema() {
        return Optional.ofNullable(MCREntityManagerProvider
            .getEntityManagerFactory()
            .getProperties()
            .get("hibernate.default_schema"))
            .map(Object::toString);
    }

    public void dropSchema() throws IOException {
        exportSchema("drop");
        doSchemaOperation(schema -> "drop schema " + schema);
    }

    public MCRJPATestCase() {
        super();
    }

    @After
    public void tearDown() throws Exception {
        try {
            endTransaction();
        } finally {
            if (entityManager.isPresent()) {
                entityManager.get().close();
                dropSchema();
            }
            super.tearDown();
            entityManager = null;
        }
    }

    protected void beginTransaction() {
        entityManager.ifPresent(em -> em.getTransaction().begin());
    }

    protected void endTransaction() {
        entityManager.ifPresent(em -> {
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
        entityManager.ifPresent(EntityManager::clear);
    }

}
