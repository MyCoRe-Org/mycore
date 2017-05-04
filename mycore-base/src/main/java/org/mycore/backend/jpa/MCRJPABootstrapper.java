/**
 * 
 */
package org.mycore.backend.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.hibernate.MCRHibernateConfigHelper;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * Initializes JPA {@link EntityManagerFactory}
 * @author Thomas Scheffler (yagee)
 */
public class MCRJPABootstrapper implements AutoExecutable {

    public static final String PERSISTENCE_UNIT_NAME = "MyCoRe";

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
        } catch (PersistenceException e) {
            //fix for MCR-1236
            if (MCRConfiguration.instance().getBoolean("MCR.Persistence.Database.Enable", true)) {
                LogManager.getLogger()
                    .error(() -> "Could not initialize JPA. Database access is disabled in this session.", e);
                MCRConfiguration.instance().set("MCR.Persistence.Database.Enable", false);
            }
            MCREntityManagerProvider.init(e);
            return;
        }
        Metamodel metamodel = MCREntityManagerProvider.getEntityManagerFactory().getMetamodel();
        checkHibernateMappingConfig(metamodel);
        LogManager.getLogger().info("Mapping these entities: " + metamodel
            .getEntities()
            .stream()
            .map(EntityType::getJavaType)
            .map(Class::getName)
            .collect(Collectors.toList()));
        MCRShutdownHandler.getInstance().addCloseable(new MCRJPAShutdownProcessor());
    }

    public static void initializeJPA() {
        initializeJPA(null, null);
    }

    public static void initializeJPA(String persistenceUnitName) {
        initializeJPA(persistenceUnitName, null);
    }

    public static void initializeJPA(String persistenceUnitName, Map<?, ?> properties) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(
            Optional.ofNullable(persistenceUnitName).orElse(PERSISTENCE_UNIT_NAME),
            properties);
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
        List<String> unMappedEntities = MCRConfiguration
            .instance()
            .getStrings("MCR.Hibernate.Mappings", Collections.emptyList())
            .stream()
            .filter(cName -> !mappedEntities.contains(cName))
            .collect(Collectors.toList());
        if (!unMappedEntities.isEmpty()) {
            throw new MCRException(
                "JPA Mapping is inclomplete. Could not find a mapping for these classes: " + unMappedEntities);
        }
    }

}
