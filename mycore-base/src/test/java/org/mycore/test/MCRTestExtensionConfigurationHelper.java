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
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.xsl.MCRParameterCollector;

public class MCRTestExtensionConfigurationHelper {

    public static final String MCR_HOME_PROPERTY = "MCR.Home";
    public static final String MCR_APP_NAME_PROPERTY = "MCR.AppName";

    static void initializeTestEnvironment(Path junitFolder) throws IOException {
        if (junitFolder == null) {
            throw new IllegalArgumentException("junitFolder must not be null");
        }
        if (System.getProperties().getProperty(MCR_HOME_PROPERTY) == null) {
            Path baseDir = junitFolder.resolve("mcrhome");
            Files.createDirectories(baseDir);
            System.out.println("Setting MCR.Home=" + baseDir.toAbsolutePath());
            System.getProperties().setProperty(MCR_HOME_PROPERTY, baseDir.toAbsolutePath().toString());
        }
        if (System.getProperties().getProperty(MCR_APP_NAME_PROPERTY) == null) {
            String currentComponentName = detectCurrentComponentName();
            System.out.println("Setting MCR.AppName=" + currentComponentName);
            System.getProperties().setProperty(MCR_APP_NAME_PROPERTY, detectCurrentComponentName());
        }
        Path mcrHome = getMCRHomeDirPath();
        Path configDir = mcrHome.resolve(System.getProperty(MCR_APP_NAME_PROPERTY));
        System.out.println("Creating config directory: " + configDir);
        Files.createDirectories(configDir);
    }

    /**
     * Clears the cache and reloads the configuration from the loader.
     *
     * @param configurationLoader The loader to use for loading configuration
     * @param baseProperties class-level configuration properties
     */
    static void resetConfiguration(MCRConfigurationLoader configurationLoader, Map<String, String> baseProperties) {
        MCRParameterCollector.clearCache();
        LogManager.getLogger().info("Reloading configuration");
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), baseProperties, true);
    }

    private static Path getMCRHomeDirPath() {
        Path mcrHome = null;
        try {
            URI homeUri = new URI(System.getProperty(MCR_HOME_PROPERTY));
            if (homeUri.isAbsolute()) {
                mcrHome = Paths.get(homeUri);
            }
        } catch (URISyntaxException e) {
            //fallback to default later
        }
        if (mcrHome == null) {
            mcrHome = Paths.get(System.getProperty(MCR_HOME_PROPERTY));
        }
        return mcrHome;
    }

    static Map<String, String> getAnnotatedProperties(ExtensionContext extensionContext) {
        return findTestConfiguration(extensionContext).stream()
            .flatMap(conf -> Stream.of(conf.properties()))
            .collect(Collectors.toMap(
                MCRTestProperty::key,
                p -> getAnnotatedValue(p, extensionContext),
                (sub, sup) -> sub));
    }

    private static String getAnnotatedValue(MCRTestProperty property, ExtensionContext context) {

        boolean empty = property.empty();

        String stringValue = property.string();
        boolean customString = !Objects.equals(stringValue, "");

        Class<?> classNameOfValue = property.classNameOf();
        boolean customClassNameOf = classNameOfValue != Void.class;

        if ((empty && (customString || customClassNameOf)) || (customString && customClassNameOf)) {
            throw new MCRException("@" + MCRTestProperty.class.getSimpleName()
                + " of " + context.getElement().map(Object::toString).orElse("unknown")
                + " can either be empty or have either a string- or a classNameOf-value,"
                + " got empty=" + empty + ",  stringValue=" + stringValue
                + ", classNameOf=" + classNameOfValue);
        }

        if (empty) {
            return "";
        } else if (customString) {
            return stringValue;
        } else if (customClassNameOf) {
            return classNameOfValue.getName();
        }

        throw new MCRException("@" + MCRTestProperty.class.getSimpleName()
            + " of " + context.getElement().map(Object::toString).orElse("unknown")
            + " must either be empty or have either a string- or a classNameOf-value");

    }

    private static List<MCRTestConfiguration> findTestConfiguration(ExtensionContext context) {
        return context.getElement().map(element -> switch (element) {
            case Method method -> findMethodTestConfiguration(method);
            case Class<?> testClass -> findClassHierarchyTestConfigurations(testClass);
            default -> List.<MCRTestConfiguration>of();
        }).orElseGet(List::of);
    }

    private static List<MCRTestConfiguration> findMethodTestConfiguration(Method method) {
        return Optional.ofNullable(method.getAnnotation(MCRTestConfiguration.class))
            .map(List::of)
            .orElseGet(List::of);
    }

    // Traverse the class hierarchy to find @MyAnnotation on the class or any of its superclasses.
    private static List<MCRTestConfiguration> findClassHierarchyTestConfigurations(Class<?> clazz) {
        List<MCRTestConfiguration> annotations = new ArrayList<>();
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            MCRTestConfiguration annotation = current.getAnnotation(MCRTestConfiguration.class);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    static MCRConfigurationLoader getConfigurationLoader() {
        System.setProperty("MCRRuntimeComponentDetector.underTesting", detectCurrentComponentName());
        String mcrComp = MCRRuntimeComponentDetector.getMyCoReComponents().stream().map(MCRComponent::toString).collect(
            Collectors.joining(", "));
        String appMod = MCRRuntimeComponentDetector.getApplicationModules()
            .stream()
            .map(MCRComponent::toString)
            .collect(Collectors.joining(", "));
        System.out.printf("MyCoRe components detected: %s\nApplications modules detected: %s\n",
            mcrComp.isEmpty() ? "'none'" : mcrComp, appMod.isEmpty() ? "'none'" : appMod);
        return MCRConfigurationLoaderFactory.getConfigurationLoader();
    }

    static String detectCurrentComponentName() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir).getFileName().toString();
    }

    public static Path getBaseDir() {
        String baseDir = System.getProperties().getProperty(MCRTestExtensionConfigurationHelper.MCR_HOME_PROPERTY);
        if (baseDir == null || baseDir.isEmpty()) {
            throw new IllegalStateException(MCRTestExtensionConfigurationHelper.MCR_HOME_PROPERTY + " must be set");
        }
        return Paths.get(baseDir);
    }

}
