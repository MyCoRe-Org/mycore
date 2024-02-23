/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.log.MCRTableMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This class is used to provide a custom persistence provider for MyCoRe. It uses Hibernate under the hood.
 * It is used to configure the persistence unit without using a persistence.xml file.
 * Instead, the persistence unit is configured using @{@link org.mycore.common.config.MCRConfiguration2}.
 *
 */
public class MCRPersistenceProvider implements PersistenceProvider {

    public static final String JPA_PERSISTENCE_UNIT_PROPERTY_NAME = "MCR.JPA.PersistenceUnit.";
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, Callable<MCRPersistenceUnitDescriptor>> persistenceUnitInitializer;
    private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();
    private final ProviderUtil providerUtil = new ProviderUtil() {
        public LoadState isLoadedWithoutReference(Object proxy, String property) {
            return PersistenceUtilHelper.isLoadedWithoutReference(proxy, property, MCRPersistenceProvider.this.cache);
        }

        public LoadState isLoadedWithReference(Object proxy, String property) {
            return PersistenceUtilHelper.isLoadedWithReference(proxy, property, MCRPersistenceProvider.this.cache);
        }

        public LoadState isLoaded(Object o) {
            return PersistenceUtilHelper.getLoadState(o);
        }
    };

    public MCRPersistenceProvider() {
        persistenceUnitInitializer = MCRConfiguration2.getInstances(JPA_PERSISTENCE_UNIT_PROPERTY_NAME);
        LOGGER.info("Found {} persistence units [{}]", persistenceUnitInitializer.size(),
            String.join(";", persistenceUnitInitializer.keySet()));
    }

    /**
     * Logs the JPA properties of the given persistence unit descriptor
     * @param puDescriptor the persistence unit descriptor
     */
    private static void logJPAProperties(MCRPersistenceUnitDescriptor puDescriptor) {
        MCRTableMessage<Property> table = new MCRTableMessage<>(new MCRTableMessage.Column<>("Name", Property::name),
            new MCRTableMessage.Column<>("Value", (prop) -> {
                if (prop.name.contains("password") || prop.name.contains("Password")) {
                    return "********";
                } else {
                    String value = prop.value();
                    return value.substring(0, Math.min(value.length(), 120));
                }
            }));

        puDescriptor.getProperties().forEach((key, value) -> table.add(new Property(key.toString(), value.toString())));

        List<String> mappingFileNames = puDescriptor.getMappingFileNames();
        for (int i = 0; i < mappingFileNames.size(); i++) {
            String fileName = mappingFileNames.get(i);
            table.add(new Property("File Mapping " + i, fileName));
        }

        Optional.ofNullable(puDescriptor.getProviderClassName())
            .ifPresent((provider) -> table.add(new Property("ProviderClassName", provider)));

        Optional.ofNullable(puDescriptor.getTransactionType())
            .ifPresent((transactionType) -> table.add(new Property("TransactionType", transactionType.name())));

        Optional.ofNullable(puDescriptor.getValidationMode())
            .ifPresent((validationMode) -> table.add(new Property("ValidationMode", validationMode.name())));

        Optional.ofNullable(puDescriptor.getSharedCacheMode())
            .ifPresent((sharedCacheMode) -> table.add(new Property("SharedCacheMode", sharedCacheMode.name())));

        LOGGER.info(table.logMessage("JPA Properties"));
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        // TODO: this is a workarround for MCRConfiguration2.getInstances returning full property keys
        if (persistenceUnitInitializer
            .containsKey(MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME + emName)) {
            LOGGER.info("Creating EntityManagerFactory for persistence unit {}", emName);
            try {

                MCRPersistenceUnitDescriptor puDescriptor = persistenceUnitInitializer
                    .get(MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME + emName).call();

                EntityManagerFactoryBuilderImpl builder = new EntityManagerFactoryBuilderImpl(puDescriptor, map,
                    MCRClassTools.getClassLoader());

                logJPAProperties(puDescriptor);

                return builder.build();
            } catch (Exception e) {
                throw new MCRException(e);
            }

        }
        return null;
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        return null;
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        // TODO: this is a workarround for MCRConfiguration2.getInstances returning full property keys
        if (persistenceUnitInitializer.containsKey(MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME +
            persistenceUnitName)) {
            try {

                MCRPersistenceUnitDescriptor puDescriptor = persistenceUnitInitializer
                    .get(MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME + persistenceUnitName).call();

                EntityManagerFactoryBuilderImpl builder = new EntityManagerFactoryBuilderImpl(puDescriptor, map,
                    MCRClassTools.getClassLoader());

                builder.generateSchema();
                return true;
            } catch (Exception e) {
                throw new MCRException(e);
            }
        }
        return false;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return providerUtil;
    }

    /**
     * A simple record for a property used in logging
     * @param name the name of the property
     * @param value the value of the property
     */
    record Property(String name, String value) {
    }
}
