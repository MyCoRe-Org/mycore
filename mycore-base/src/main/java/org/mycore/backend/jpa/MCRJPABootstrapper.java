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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.hibernate.MCRHibernateConfigHelper;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.servlet.ServletContext;

/**
 * Initializes JPA {@link EntityManagerFactory}
 * @author Thomas Scheffler (yagee)
 */
public class MCRJPABootstrapper implements AutoExecutable {

    @Override
    public String getName() {
        return "JPA Bootstrapper";
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        try {
            initializeJPA();
        } catch (PersistenceException pe) {
            disableDatabaseAccess(pe);
            return;
        } catch (MCRException mcre) {
            if (mcre.getCause() instanceof PersistenceException pe) {
                disableDatabaseAccess(pe);
                return;
            }
            throw mcre;
        }

        Metamodel metamodel = MCREntityManagerProvider.getEntityManagerFactory().getMetamodel();
        checkHibernateMappingConfig(metamodel);
        LogManager.getLogger()
            .info("Mapping these entities: {}", () -> metamodel.getEntities()
                .stream()
                .map(EntityType::getJavaType)
                .map(Class::getName)
                .collect(Collectors.toList()));
        MCRShutdownHandler.getInstance().addCloseable(new MCRJPAShutdownProcessor());
    }

    private static void disableDatabaseAccess(PersistenceException ex) {
        //fix for MCR-1236
        if (MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)) {
            LogManager.getLogger()
                .error(() -> "Could not initialize JPA. Database access is disabled in this session.", ex);
            MCRConfiguration2.set("MCR.Persistence.Database.Enable", String.valueOf(false));
        }
        MCREntityManagerProvider.init(ex);
    }

    public static void initializeJPA() {
        initializeJPA(null, null);
    }

    public static void initializeJPA(String persistenceUnitName) {
        initializeJPA(persistenceUnitName, null);
    }

    public static void initializeJPA(String persistenceUnitName, Map<?, ?> properties) {
        String unitName = Optional.ofNullable(persistenceUnitName)
            .orElseGet(() -> MCRConfiguration2.getStringOrThrow("MCR.JPA.PersistenceUnitName"));

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(unitName, properties);

        checkFactory(entityManagerFactory);
        MCREntityManagerProvider.init(entityManagerFactory);
    }

    private static void checkFactory(EntityManagerFactory entityManagerFactory) {
        MCRHibernateConfigHelper.checkEntityManagerFactoryConfiguration(entityManagerFactory);
    }

    private void checkHibernateMappingConfig(Metamodel metamodel) {
        Set<String> mappedEntities = metamodel
            .getEntities()
            .stream()
            .map(EntityType::getJavaType)
            .map(Class::getName)
            .collect(Collectors.toSet());
        List<String> unMappedEntities = MCRConfiguration2.getString("MCR.Hibernate.Mappings")
            .map(MCRConfiguration2::splitValue)
            .orElseGet(Stream::empty)
            .filter(cName -> !mappedEntities.contains(cName))
            .collect(Collectors.toList());
        if (!unMappedEntities.isEmpty()) {
            LogManager.getLogger()
                .error(() -> "JPA Mapping is incomplete. Could not find a mapping for these classes: "
                    + unMappedEntities);
            LogManager.getLogger()
                .error("Could not initialize JPA. Database access is disabled in this session.");
            MCRConfiguration2.set("MCR.Persistence.Database.Enable", String.valueOf(false));
        }
    }

}
