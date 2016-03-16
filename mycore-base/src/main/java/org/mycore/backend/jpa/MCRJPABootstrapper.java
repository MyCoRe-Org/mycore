/**
 * 
 */
package org.mycore.backend.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
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
        initializeJPA();
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
        initializeJPA(null);
    }

    public static void initializeJPA(Map<?, ?> properties) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME,
            properties);
        MCREntityManagerProvider.init(entityManagerFactory);
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
