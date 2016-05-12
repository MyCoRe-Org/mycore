/**
 * 
 */
package org.mycore.backend.hibernate;

import java.text.MessageFormat;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.logging.log4j.LogManager;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL92Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.mycore.backend.hibernate.dialects.MCRPostgreSQL92Dialect;
import org.mycore.backend.hibernate.dialects.MCRPostgreSQL94Dialect;
import org.mycore.backend.hibernate.dialects.MCRPostgreSQL9Dialect;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Helper class to check if EntityManagerFactory is correctly configured.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRHibernateConfigHelper {

    public static void checkEntityManagerFactoryConfiguration(EntityManagerFactory entityManagerFactory) {
        try {
            SessionFactoryImpl sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl.class);
            Dialect dialect = sessionFactoryImpl.getDialect();
            checkPostgreSQL94Dialect(dialect);
            checkPostgreSQL92Dialect(dialect);
            checkPostgreSQL9Dialect(dialect);
        } catch (PersistenceException e) {
            LogManager.getLogger()
                .warn("Unsupported EntityManagerFactory found: " + entityManagerFactory.getClass().getName());
        }
    }

    private static void checkPostgreSQL9Dialect(Dialect dialect) {
        checkDialect(dialect, PostgreSQL9Dialect.class, MCRPostgreSQL9Dialect.class);
    }

    private static void checkPostgreSQL92Dialect(Dialect dialect) {
        checkDialect(dialect, PostgreSQL92Dialect.class, MCRPostgreSQL92Dialect.class);
    }

    private static void checkPostgreSQL94Dialect(Dialect dialect) {
        checkDialect(dialect, PostgreSQL94Dialect.class, MCRPostgreSQL94Dialect.class);
    }

    private static void checkDialect(Dialect dialect, Class<? extends Dialect> forbidden,
        Class<? extends Dialect> replacement) {
        if (dialect.getClass().getName().equals(forbidden.getName())) {
            throw new MCRConfigurationException(MessageFormat.format("Hibernate dialect is unsupported: {0}. Please set ''hibernate.dialect'' to ''{1}'' in your persistence.xml and read more on http://mycore.de/documentation/production/postgres.html",
                dialect.getClass().getName(), replacement.getName()));
        }
    }

}
