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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

public class MCRPersistenceUnitDescriptor implements PersistenceUnitDescriptor {

    public static final String MCR_PERSISTENCE_PROPERTIES_PREFIX = "Properties.";
    private static final Logger LOGGER = LogManager.getLogger();
    private String persistenceProviderClassName;
    private boolean useQuotedIdentifiers;
    private boolean excludeUnlistedClasses;
    private PersistenceUnitTransactionType transactionType;
    private ValidationMode validationMode;
    private SharedCacheMode sharedCacheMode;
    private List<String> managedClassNames;

    private List<String> mappingFileNames;

    private Map<String, String> properties;

    private String name;

    @MCRPostConstruction
    public void initializeName(String property) {
       this.name = property.substring(MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME.length())
               .split("\\.")[0];
       LOGGER.info("Initialized persistence unit {}", this.name);
    }

    @MCRProperty(name = "PersistenceProviderClassName", required = false,
        defaultName = "MCR.JPA.ProviderClassName")
    public void setPersistenceProviderClassName(String persistenceProviderClassName) {
        this.persistenceProviderClassName = persistenceProviderClassName;
    }

    @MCRProperty(name = "UseQuotedIdentifiers", required = false, defaultName = "MCR.JPA.UseQuotedIdentifiers")
    public void setUseQuotedIdentifiersString(String useQuotedIdentifiersString) {
        setUseQuotedIdentifiers(Boolean.parseBoolean(useQuotedIdentifiersString));
    }

    @MCRProperty(name = "ExcludeUnlistedClasses", required = false,
        defaultName = "MCR.JPA.ExcludeUnlistedClasses")
    public void setExcludeUnlistedClassesString(String excludeUnlistedClassesString) {
        setExcludeUnlistedClasses(Boolean.parseBoolean(excludeUnlistedClassesString));
    }

    @MCRProperty(name = "TransactionType", required = false, defaultName = "MCR.JPA.TransactionType")
    public void setTransactionTypeString(String transactionTypeString) {
        setTransactionType(PersistenceUnitTransactionType.valueOf(transactionTypeString));
    }

    @MCRProperty(name = "ValidationMode", required = false, defaultName = "MCR.JPA.ValidationMode")
    public void setValidationModeString(String validationModeString) {
        setValidationMode(ValidationMode.valueOf(validationModeString));
    }

    @MCRProperty(name = "SharedCacheMode", required = false, defaultName = "MCR.JPA.SharedCacheMode")
    public void setSharedCacheModeString(String sharedCacheModeString) {
        setSharedCacheMode(SharedCacheMode.valueOf(sharedCacheModeString));
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
       return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProviderClassName() {
        return persistenceProviderClassName;
    }

    @Override
    public boolean isUseQuotedIdentifiers() {
        return useQuotedIdentifiers;
    }

    public void setUseQuotedIdentifiers(boolean useQuotedIdentifiers) {
        this.useQuotedIdentifiers = useQuotedIdentifiers;
    }

    @Override
    public boolean isExcludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(PersistenceUnitTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    @MCRProperty(name = "ManagedClassNames", required = false, defaultName = "MCR.JPA.ManagedClassNames")
    public void setManagedClassNames(String managedClassNames) {
        Stream<String> classNames = MCRConfiguration2.splitValue(managedClassNames);
        setManagedClassNames(classNames.filter(className -> !className.isBlank()).toList());
    }

    public void setManagedClassNames(List<String> managedClassNames) {
        this.managedClassNames = managedClassNames;
    }

    @Override
    public List<String> getMappingFileNames() {
        return mappingFileNames;
    }

    @MCRProperty(name = "MappingFileNames", required = false, defaultName = "MCR.JPA.MappingFileNames")
    public void setMappingFileNames(String mappingFileNames) {
        Stream<String> fileNames = MCRConfiguration2.splitValue(mappingFileNames);
        setMappingFileNames(fileNames.filter(fileName -> !fileName.isBlank()).toList());
    }

    public void setMappingFileNames(List<String> mappingFileNames) {
        this.mappingFileNames = mappingFileNames;
    }

    @Override
    public List<URL> getJarFileUrls() {
        // LOGGER.warn("getJarFileUrls not implemented");
        return Collections.emptyList();
    }

    @Override
    public Object getNonJtaDataSource() {
        // LOGGER.warn("getNonJtaDataSource not implemented");
        return null;
    }

    @Override
    public Object getJtaDataSource() {
        // LOGGER.warn("getJtaDataSource not implemented");
        return null;
    }

    @Override
    public Properties getProperties() {
        Properties createdProperties = new Properties();
        this.properties.keySet()
            .stream()
            .filter(key -> key.startsWith(MCR_PERSISTENCE_PROPERTIES_PREFIX))
            .forEach(key -> {
                String propertyName = key.substring(MCR_PERSISTENCE_PROPERTIES_PREFIX.length());
                String propertyKey = this.properties.get(key);

                createdProperties.put(propertyName, propertyKey);
            });
        return createdProperties;
    }

    @MCRProperty(name = "*")
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public ClassLoader getClassLoader() {
        return MCRClassTools.getClassLoader();
    }

    @Override
    public ClassLoader getTempClassLoader() {
        return getClassLoader();
    }

    @Override
    public void pushClassTransformer(EnhancementContext enhancementContext) {
        // LOGGER.warn("pushClassTransformer not implemented");
    }

    @Override
    public ClassTransformer getClassTransformer() {
        // LOGGER.warn("getClassTransformer not implemented");
        return null;
    }
}
