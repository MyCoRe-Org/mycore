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
 * <p>
 * Configuration properties are applied in this order, each overriding the previous one:
 * </p>
 * <ol>
 * <li>mycore.properties</li>
 * <li>properties contributed by other extensions through {@link #getClassProperties(ExtensionContext)}</li>
 * <li>{@link org.mycore.common.MCRTestProperty} of the test class and its enclosing and super classes</li>
 * <li>{@link org.mycore.common.MCRTestProperty} of the test method</li>
 * </ol>
 * <p>
 * What a test declares therefore always wins over what an extension contributed, so that a test can opt out of
 * any part of the setup an extension has chosen for it.
 * </p>
 * <p>
 * Subsystems that a test can only use safely when a dedicated extension has set them up are configured to fail
 * instead of silently working on shared state. Currently this is the metadata store, which defaults to
 * {@link MCRAlwaysFailingXMLMetadataManager} until {@link MCRMetadataExtension} replaces it. A test that needs
 * such a subsystem declares its extension; one that does not gets a clear error rather than side effects on
 * unrelated test classes.
 * </p>
 */
public class MCRTestExtension implements Extension, BeforeEachCallback, AfterEachCallback, BeforeAllCallback,
    AfterAllCallback {

    public static final String CLASS_PROPERTIES_MAP_PROPERTY = "classProperties";

    /**
     * The property naming the {@link org.mycore.datamodel.common.MCRXMLMetadataManager} implementation. Tests get
     * {@link MCRAlwaysFailingXMLMetadataManager} unless an extension or the test itself configures something else.
     */
    public static final String METADATA_MANAGER_CLASS_PROPERTY = "MCR.Metadata.Manager.Class";

    private static final String INITIALIZED_PROPERTY = "initialized";
    private static final String PROPERTIES_MAP_PROPERTY = "properties";
    private static final String PROPERTIES_LOADED_PROPERTY = "propertiesLoaded";
    private static final String FIRST_EACH_CALLBACK_PROPERTY = "firstEachCallback";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(MCRTestExtension.class);

    private final Path testFolder;

    private final MCRConfigurationLoader configurationLoader;

    private final Map<String, String> mycoreProperties;

    /**
     * The metadata manager of mycore.properties, which {@link #disableMetadataStoreByDefault()} replaces.
     * <p>
     * Static, so that {@link MCRMetadataExtension} can read it without depending on which instance of this
     * extension belongs to its test class. The value is derived from the configuration of the module under test
     * and is therefore the same for every test class of a JVM fork.
     */
    private static volatile String configuredMetadataManager;

    MCRTestExtension() throws IOException {
        testFolder = createTempDirectory();
        MCRTestExtensionConfigurationHelper.initializeTestEnvironment(testFolder);
        configurationLoader = MCRTestExtensionConfigurationHelper.getConfigurationLoader();
        LOGGER.debug(() -> testFolder);
        mycoreProperties = new HashMap<>(configurationLoader.load());
        configuredMetadataManager = disableMetadataStoreByDefault();
    }

    /**
     * Points {@link #METADATA_MANAGER_CLASS_PROPERTY} at {@link MCRAlwaysFailingXMLMetadataManager}, so that a test
     * that uses the metadata store without an extension that sets one up fails instead of working on state shared
     * with every other test of the same fork.
     * <p>
     * Whichever manager mycore.properties names is replaced, so this does not have to know which one MyCoRe
     * configures by default, nor which one a module configures for its own tests. An extension that sets up a
     * metadata store puts it back with {@link #enableConfiguredMetadataManager(ExtensionContext)}.
     *
     * @return the metadata manager configured in mycore.properties, or null if none is configured
     */
    private String disableMetadataStoreByDefault() {
        String configuredManager = mycoreProperties.get(METADATA_MANAGER_CLASS_PROPERTY);
        if (configuredManager == null) {
            return null;
        }
        mycoreProperties.put(METADATA_MANAGER_CLASS_PROPERTY, MCRAlwaysFailingXMLMetadataManager.class.getName());
        return configuredManager.trim();
    }

    /**
     * Puts the metadata manager of mycore.properties back in place of the
     * {@link MCRAlwaysFailingXMLMetadataManager} that tests get by default. An extension that sets up a metadata
     * store calls this, so that the test it serves can use one.
     * <p>
     * A manager that the test names in an {@link org.mycore.common.MCRTestProperty} still wins, because the
     * annotated properties are applied after those contributed by an extension.
     *
     * @param context the current extension context
     */
    public static void enableConfiguredMetadataManager(ExtensionContext context) {
        getClassProperties(context).put(METADATA_MANAGER_CLASS_PROPERTY, getConfiguredMetadataManager());
    }

    /**
     * Returns the {@link org.mycore.datamodel.common.MCRXMLMetadataManager} that mycore.properties configures, as
     * opposed to the {@link MCRAlwaysFailingXMLMetadataManager} that tests get by default.
     *
     * @return the fully qualified class name of the configured metadata manager
     */
    public static String getConfiguredMetadataManager() {
        String configuredManager = configuredMetadataManager;
        if (configuredManager == null) {
            throw new IllegalStateException("The configured metadata manager is unknown, because no "
                + MCRTestExtension.class.getSimpleName() + " has been instantiated. Annotate the test with @"
                + MyCoReTest.class.getSimpleName() + ".");
        }
        return configuredManager;
    }

    /**
     * Prepares property that are defined by the test class.
     * If a extensions wants to add properties to the configuration, it should use the
     * {@link #getClassProperties(ExtensionContext)} method to get the properties map.
     * <p>
     * The properties are finally collected in the {@link #beforeEach(ExtensionContext)} method.
     * Class-level properties a cached between the test methods.
     */
    /**
     * Fails unless this extension has already set up the configuration for the current test class.
     * <p>
     * Every MyCoRe extension builds on that configuration, in its {@code beforeAll} as well as in its
     * {@code beforeEach}. Since JUnit calls the extensions in declaration order, {@link MyCoReTest} has to come
     * first. An extension that would otherwise fail somewhere deep inside MyCoRe calls this to say so plainly.
     *
     * @param context the current extension context
     * @param extension the extension that requires the configuration, for the error message
     */
    public static void requireInitialized(ExtensionContext context, Class<? extends Extension> extension) {
        if (!Boolean.TRUE.equals(context.getStore(NAMESPACE).get(INITIALIZED_PROPERTY, Boolean.class))) {
            throw new IllegalStateException(context.getRequiredTestClass().getName() + " uses @ExtendWith("
                + extension.getSimpleName() + ".class), which builds on the configuration that "
                + MCRTestExtension.class.getSimpleName() + " sets up. Annotate the test class with @"
                + MyCoReTest.class.getSimpleName() + " and declare it before @ExtendWith("
                + extension.getSimpleName() + ".class), because JUnit calls the extensions in declaration order.");
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        context.getStore(NAMESPACE).put(INITIALIZED_PROPERTY, Boolean.TRUE);
        Map<String, String> configProperties = getConfigProperties(context);
        Map<String, String> annotatedProperties = MCRTestExtensionConfigurationHelper.getAnnotatedProperties(context);
        configProperties.clear(); //clear properties from previous test classes
        configProperties.putAll(mycoreProperties);
        configProperties.putAll(annotatedProperties);
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
                //what the test class itself declares wins over what an extension contributed
                configProperties.putAll(annotatedProperties);
            });
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), mycoreProperties, true);
    }

    private ExtensionContext getClassContext(ExtensionContext context) {
        ExtensionContext currentCtx = context;
        //navigate up to the class context, which can be several levels up for @ParameterizedTest
        while (currentCtx.getTestMethod().isPresent() && currentCtx.getParent().isPresent()) {
            currentCtx = currentCtx.getParent().get();
        }
        return currentCtx;
    }

    private Map<String, String> getConfigProperties(ExtensionContext context) {
        ExtensionContext classContext = getClassContext(context);
        return classContext.getStore(NAMESPACE)
            .computeIfAbsent(MCRTestExtension.PROPERTIES_MAP_PROPERTY, k -> {
                LOGGER.debug(() -> classContext.getRequiredTestClass() + " creating new properties map");
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
        // Keyed by test class: JUnit resolves a store key through the ancestor contexts, so a shared key would
        // hand a @Nested class the properties that an extension contributed for one of its siblings.
        String key = CLASS_PROPERTIES_MAP_PROPERTY + '/' + context.getRequiredTestClass().getName();
        return context.getStore(NAMESPACE)
            .computeIfAbsent(key, k -> {
                LOGGER.debug("Creating empty extension properties");
                return new HashMap<>();
            }, Map.class);
    }

    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("junit-");
    }

}
