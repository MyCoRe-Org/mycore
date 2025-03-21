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

package org.mycore.backend.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Helper class to check if EntityManagerFactory is correctly configured.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRHibernateConfigHelper {

    public static void checkEntityManagerFactoryConfiguration(EntityManagerFactory entityManagerFactory) {
        try {
            SessionFactoryImpl sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl.class);
            if (PostgreSQLDialect.class
                .isInstance(sessionFactoryImpl.getServiceRegistry().getService(JdbcServices.class).getDialect())) {
                //fix ClassLeftUnique and ClassRightUnique, as PostgreSQL cannot evaluate them on statement level
                modifyConstraints(sessionFactoryImpl);
            }
        } catch (PersistenceException e) {
            LogManager.getLogger()
                .warn("Unsupported EntityManagerFactory found: {}", () -> entityManagerFactory.getClass().getName());
        }
    }

    private static void modifyConstraints(SessionFactoryImpl sessionFactoryImpl) {
        MappingMetamodel mappingMetamodel = sessionFactoryImpl.getMappingMetamodel();
        EntityPersister entityPersister = mappingMetamodel.findEntityDescriptor(MCRCategoryImpl.class);
        String qualifiedTableName = ((SingleTableEntityPersister) entityPersister).getTableName();
        try (Session session = sessionFactoryImpl.openSession()) {
            session.doWork(connection -> {
                String updateStmt = Stream.of("ClassLeftUnique", "ClassRightUnique")
                    .flatMap(idx -> Stream.of("drop constraint if exists " + idx,
                        String.format(Locale.ROOT, "add constraint %s unique (%s) deferrable initially deferred", idx,
                            getUniqueColumns(MCRCategoryImpl.class, idx))))
                    .collect(Collectors.joining(", ", getAlterTableString(connection) + qualifiedTableName + " ", ""));
                try (Statement stmt = connection.createStatement()) {
                    LogManager.getLogger().info("Fixing PostgreSQL Schema for {}:\n{}", qualifiedTableName, updateStmt);
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
