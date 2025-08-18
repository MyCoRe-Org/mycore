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

package org.mycore.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationLoader;

/**
 * JUnit 5 extension for MyCoRe tests.
 * <p>
 * This extension provides a temporary folder for each test and loads the configuration properties from the
 * {@link MCRConfigurationLoader}.
 * </p>
 */
public class MCRTestExtension implements Extension, BeforeEachCallback, AfterEachCallback, BeforeAllCallback,
    AfterAllCallback {

    public static final String CLASS_PROPERTIES_MAP_PROPERTY = "classProperties";
    private static final String PROPERTIES_MAP_PROPERTY = "properties";
    private static final String PROPERTIES_LOADED_PROPERTY = "propertiesLoaded";
    private static final String FIRST_EACH_CALLBACK_PROPERTY = "firstEachCallback";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(MCRTestExtension.class);

    private final Path testFolder;

    private final MCRConfigurationLoader configurationLoader;

    private final Map<String, String> mycoreProperties;

    MCRTestExtension() throws IOException {
        testFolder = createTempDirectory();
        MCRTestExtensionConfigurationHelper.initializeTestEnvironment(testFolder);
        configurationLoader = MCRTestExtensionConfigurationHelper.getConfigurationLoader();
        LOGGER.debug(() -> testFolder);
        mycoreProperties = new HashMap<>(configurationLoader.load());
    }

    /**
     * Prepares property that are defined by the test class.
     * If a extensions wants to add properties to the configuration, it should use the
     * {@link #getClassProperties(ExtensionContext)} method to get the properties map.
     * <p>
     * The properties are finally collected in the {@link #beforeEach(ExtensionContext)} method.
     * Class-level properties a cached between the test methods.
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Map<String, String> configProperties = getConfigProperties(context);
        configProperties.clear(); //clear properties from previous test classes
        configProperties.putAll(mycoreProperties);
        configProperties.putAll(MCRTestExtensionConfigurationHelper.getAnnotatedProperties(context));
        context.getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()))
            .put(FIRST_EACH_CALLBACK_PROPERTY, (Runnable) () -> {
                //collect properties defined by beforeAll of other extensions, see JPATestExtension
                Map<String, String> classProperties = getClassProperties(context);
                LOGGER.debug(() -> "Collect extension properties:\n" + classProperties
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse(""));
                configProperties.putAll(classProperties);
            });
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), mycoreProperties, true);
    }

    private Map<String, String> getConfigProperties(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE)
            .getOrComputeIfAbsent(MCRTestExtension.PROPERTIES_MAP_PROPERTY, k -> {
                LOGGER.debug(() -> context.getElement().get() + " creating new properties map");
                return new HashMap<>();
            }, Map.class);
    }

    @Override
    public void afterAll(ExtensionContext context) throws IOException {
        try {
            MCRTestHelper.deleteRecursively(testFolder);
            LogManager.getLogger().debug(() -> "Deleted test folder: " + testFolder);
            if (!MCRJunit5ExtensionHelper.isNestedTestClass(context)) {
                MCRTestExtensionConfigurationHelper.resetConfiguration(configurationLoader, mycoreProperties);
            }
        } finally {
            context.getRoot().getStore(NAMESPACE).put(PROPERTIES_LOADED_PROPERTY, Boolean.FALSE);
        }
    }

    /**
     * Loads the configuration properties from the {@link MCRConfigurationLoader} and the test class
     * and also applies any properties defined by the test method.
     * <p>
     * The current thread is unlocked for MCRSessionMgr.
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ExtensionContext.Namespace testClassNS = ExtensionContext.Namespace.create(context.getRequiredTestClass());
        if (!Boolean.TRUE
            .equals(context.getParent().get().getStore(testClassNS).get(PROPERTIES_LOADED_PROPERTY, Boolean.class))) {
            LOGGER.debug("First each callback");
            Runnable firstEachCallback =
                context.getStore(testClassNS).get(FIRST_EACH_CALLBACK_PROPERTY, Runnable.class);
            firstEachCallback.run();
            context.getParent().get().getStore(testClassNS).put(PROPERTIES_LOADED_PROPERTY, Boolean.TRUE);
        }
        HashMap<String, String> combinedProperties = new HashMap<>(getConfigProperties(context));
        Map<String, String> annotatedProperties = MCRTestExtensionConfigurationHelper.getAnnotatedProperties(context);
        combinedProperties.putAll(annotatedProperties);
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), combinedProperties, true);
        MCRSessionMgr.unlock();
    }

    /**
     * Closes the current session and locks the current thread for MCRSessionMgr.
     *
     * @see MCRTestExtensionConfigurationHelper#resetConfiguration(MCRConfigurationLoader, Map)
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            MCRSessionMgr.releaseCurrentSession();
            currentSession.close();
            MCRSessionMgr.lock();
        }
        MCRTestExtensionConfigurationHelper.resetConfiguration(configurationLoader, getConfigProperties(context));
    }

    /**
     * Returns a map of properties that could be enhanced by other extensions in
     * {@link BeforeAllCallback#beforeAll(ExtensionContext)}.
     *
     * @param context the current extension context
     * @return the properties map for the current test class
     */
    public static Map<String, String> getClassProperties(ExtensionContext context) {
        //check if context is for a class
        if (context.getTestMethod().isPresent()) {
            throw new IllegalStateException("This method should only be called for class-level extensions.");
        }
        return context.getRoot().getStore(NAMESPACE)
            .getOrComputeIfAbsent(MCRTestExtension.CLASS_PROPERTIES_MAP_PROPERTY, k -> {
                LOGGER.debug("Creating empty extension properties");
                return new HashMap<>();
            }, Map.class);
    }

    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("junit-");
    }

}
