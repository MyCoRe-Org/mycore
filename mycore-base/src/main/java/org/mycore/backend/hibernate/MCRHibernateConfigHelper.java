/**
 * 
 */
package org.mycore.backend.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.logging.log4j.LogManager;
import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * Helper class to check if EntityManagerFactory is correctly configured.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRHibernateConfigHelper {

    public static void checkEntityManagerFactoryConfiguration(EntityManagerFactory entityManagerFactory) {
        try {
            SessionFactoryImpl sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl.class);
            if (PostgreSQL9Dialect.class
                .isInstance(sessionFactoryImpl.getServiceRegistry().getService(JdbcServices.class).getDialect())) {
                //fix ClassLeftUnique and ClassRightUnique, as PostgreSQL cannot evaluate them on statement level
                modifyConstraints(sessionFactoryImpl);
            }
        } catch (PersistenceException e) {
            LogManager.getLogger()
                .warn("Unsupported EntityManagerFactory found: " + entityManagerFactory.getClass().getName());
        }
    }

    private static void modifyConstraints(SessionFactoryImpl sessionFactoryImpl) {
        ClassMetadata classMetadata = sessionFactoryImpl.getClassMetadata(MCRCategoryImpl.class);
        AbstractEntityPersister aep = (AbstractEntityPersister) classMetadata;
        String qualifiedTableName = aep.getTableName();
        try (Session session = sessionFactoryImpl.openSession()) {
            session.doWork(connection -> {
                String updateStmt = Stream.of("ClassLeftUnique", "ClassRightUnique")
                    .flatMap(idx -> Stream.of("drop constraint if exists " + idx,
                        MessageFormat.format(
                            "add constraint {0} unique ({1}) deferrable initially deferred",
                            idx, getUniqueColumns(MCRCategoryImpl.class, idx))))
                    .collect(Collectors.joining(", ", getAlterTableString(connection) + qualifiedTableName + " ", ""));
                try (Statement stmt = connection.createStatement()) {
                    LogManager.getLogger()
                        .info("Fixing PostgreSQL Schema for " + qualifiedTableName + ":\n" + updateStmt);
                    stmt.execute(updateStmt);
                }
            });
        }
    }

    private static String getAlterTableString(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseMinorVersion() < 2 ? "alter table " : "alter table if exists ";
    }

    private static String getUniqueColumns(Class<?> clazz, String name) {
        return Optional.of(clazz)
            .map(c -> c.getAnnotation(Table.class))
            .map(Table::uniqueConstraints)
            .map(Stream::of)
            .flatMap(s -> s
                .filter(uc -> uc.name().equals(name))
                .findAny()
                .map(UniqueConstraint::columnNames))
            .map(Stream::of)
            .map(s -> s.collect(Collectors.joining(", ")))
            .get();
    }

}
