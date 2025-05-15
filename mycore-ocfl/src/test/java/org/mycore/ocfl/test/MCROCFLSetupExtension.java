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

package org.mycore.ocfl.test;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.ocfl.MCROCFLException;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.niofs.MCROCFLFileSystemProvider;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.niofs.storage.MCROCFLLocalFileStorage;
import org.mycore.ocfl.niofs.storage.MCROCFLNeverEvictStrategy;
import org.mycore.ocfl.niofs.storage.MCROCFLRemoteFileStorage;
import org.mycore.ocfl.repository.MCROCFLHashRepositoryProvider;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryBuilder;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.util.MCROCFLDeleteUtils;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

/**
 * JUnit 5 extension that sets up and tears down an {@code MCROCFLRepository} before and after each test.
 * <p>
 * It detects {@code remote} and {@code purge} fields on the test class (optionally annotated with
 * {@link PermutedParam}), configures a {@link RepositoryProviderMock}, and injects the repository into a field named
 * {@code repository}.
 * <p>
 * Optionally loads a test derivate if the test class or method is annotated with {@link LoadDefaultDerivate}.
 * <p>
 * After the test, the repository is purged, transactions are rolled back, and OCFL caches are cleared.
 * <p>
 * Expected test class fields:
 * <ul>
 *   <li>{@code boolean remote}</li>
 *   <li>{@code boolean purge}</li>
 *   <li>{@code MCROCFLRepository repository}</li>
 * </ul>
 */
public class MCROCFLSetupExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();

        // Find and inject common fields if necessary (e.g., remote and purge)
        Boolean remote = getBooleanFieldValue(clazz, testInstance, "remote");
        Boolean purge = getBooleanFieldValue(clazz, testInstance, "purge");

        // Set up the repository
        MCROCFLRepository repository = createRepository(remote != null ? remote : false);
        setField(clazz, testInstance, "repository", repository);

        // Configure purge property
        String purgePropertyName =
            MCROCFLDeleteUtils.PROPERTY_PREFIX +
                MCROCFLObjectIDPrefixHelper.MCRDERIVATE.replace(":", "");
        MCRConfiguration2.set(purgePropertyName, Boolean.toString(purge != null ? purge : false));

        // Configure Local Storage
        if (remote != null && remote) {
            MCRConfiguration2.set("MCR.Content.TempStorage", MCROCFLRemoteFileStorage.class.getName());
            MCRConfiguration2.set("MCR.Content.TempStorage.EvictionStrategy",
                MCROCFLNeverEvictStrategy.class.getName());
            MCRConfiguration2.set("MCR.Content.TempStorage.Path", "%MCR.datadir%/ocfl-temp-storage");
        } else {
            MCRConfiguration2.set("MCR.Content.TempStorage", MCROCFLLocalFileStorage.class.getName());
        }

        // Execute common initialization
        MCROCFLFileSystemProvider.get().init();

        // load derivate
        if (isLoadDerivate(context)) {
            MCRTransactionManager.beginTransactions(MCROCFLFileSystemTransaction.class);
            MCROCFLTestCaseHelper.loadDerivate(MCROCFLTestCaseHelper.DERIVATE_1);
            MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
        }
    }

    private MCROCFLRepository createRepository(boolean remote) throws IOException {
        String repositoryId = MCRConfiguration2.getString("MCR.Content.Manager.Repository").orElseThrow();
        MCROCFLRepositoryProvider repositoryProvider = MCROCFLRepositoryProvider.getProvider(repositoryId);
        if (!(repositoryProvider instanceof RepositoryProviderMock repositoryProviderMock)) {
            throw new MCROCFLException("Invalid provider. Should be RepositoryProviderMock, but is "
                + repositoryProvider.getClass().getSimpleName());
        }
        repositoryProviderMock.setRemote(remote);
        repositoryProviderMock.init(MCROCFLRepositoryProvider.REPOSITORY_PROPERTY_PREFIX + repositoryId);

        LOGGER.info("OCFL repository root: {}", repositoryProviderMock.getRepositoryRoot());
        LOGGER.info("OCFL repository working directory: {}", repositoryProviderMock.getWorkDir());

        return repositoryProvider.getRepository();
    }

    private void tearDownRepository(MCROCFLRepository repository) {
        for (String objectId : repository.listObjectIds().toList()) {
            repository.purgeObject(objectId);
        }
    }

    private boolean isLoadDerivate(ExtensionContext context) {
        // First, check if the test method has the annotation.
        Optional<AnnotatedElement> element = context.getElement();
        if (element.isPresent() && element.get().isAnnotationPresent(LoadDefaultDerivate.class)) {
            return element.get().getAnnotation(LoadDefaultDerivate.class).value();
        }
        // Otherwise, check at the class level.
        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isPresent() && testClass.get().isAnnotationPresent(LoadDefaultDerivate.class)) {
            return testClass.get().getAnnotation(LoadDefaultDerivate.class).value();
        }
        // by default, we load the derivate
        return true;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();

        // Clean up repository
        MCROCFLRepository repository = (MCROCFLRepository) getField(clazz, testInstance, "repository");
        tearDownRepository(repository);
        MCROCFLFileSystemProvider.get().clearCache();
        MCRTransactionManager.rollbackTransactions();
        MCROCFLFileSystemTransaction.resetTransactionCounter();
    }

    private Boolean getBooleanFieldValue(Class<?> clazz, Object instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Boolean) field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private Object getField(Class<?> clazz, Object instance, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    private void setField(Class<?> clazz, Object instance, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * Marks a test or test class to explicitly control whether the default test derivate should be loaded.
     * <p>
     * If omitted, derivate loading defaults to {@code true}.
     * <p>
     * Can be used at method or class level.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface LoadDefaultDerivate {
        boolean value() default true;
    }

    public static class RepositoryProviderMock extends MCROCFLHashRepositoryProvider {

        private boolean remote;

        public void setRemote(boolean remote) {
            this.remote = remote;
        }

        @Override
        protected MCROCFLRepository buildRepository(String id) {
            return new MCROCFLRepositoryBuilder()
                .id(id)
                .remote(this.remote)
                .defaultLayoutConfig(getExtensionConfig())
                .storage(this::configureStorage)
                .workDir(workDir)
                .buildMCR();
        }

    }

}
