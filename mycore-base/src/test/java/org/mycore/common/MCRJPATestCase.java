package org.mycore.common;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.hibernate.HibernateException;
import org.junit.After;
import org.junit.Before;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRJPABootstrapper;

public class MCRJPATestCase extends MCRTestCase {

    protected EntityManager entityManager;

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
            Persistence.generateSchema(MCRJPABootstrapper.PERSISTENCE_UNIT_NAME, schemaProperties);
            LogManager.getLogger().debug(() -> "invoked '" + action + "' sql script:\n" + output.toString());
        }
    }

    @Before()
    @Override
    public void setUp() throws Exception {
        // Configure logging etc.
        super.setUp();
        Logger.getLogger(MCRHibTestCase.class).debug("Setup JPA");
        MCRJPABootstrapper.initializeJPA();
        exportSchema();
        try {
            LogManager.getLogger().debug("Prepare hibernate test", new RuntimeException());
            entityManager = MCREntityManagerProvider.getCurrentEntityManager();
            beginTransaction();
            entityManager.clear();
        } catch (RuntimeException e) {
            LogManager.getLogger().error("Error while setting up JPA JUnit test.", e);
            throw e;
        }
    }

    public void exportSchema() throws IOException {
        exportSchema("create");
    }

    public void dropSchema() throws IOException {
        exportSchema("drop");
    }

    public MCRJPATestCase() {
        super();
    }

    @After
    public void tearDown() throws Exception {
        endTransaction();
        entityManager.close();
        dropSchema();
        super.tearDown();
        entityManager = null;
    }

    protected void beginTransaction() {
        entityManager.getTransaction().begin();
    }

    protected void endTransaction() throws HibernateException {
        EntityTransaction tx = entityManager.getTransaction();
        if (tx != null && tx.isActive()) {
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

    protected void startNewTransaction() {
        endTransaction();
        beginTransaction();
        // clear from cache
        entityManager.clear();
    }

}
