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

package org.mycore.common.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;

import com.google.common.collect.Sets;

/**
 * On first access this class detects all components, that is either MyCoRe components or application modules, that are
 * available via the current ClassLoader. Every {@link Manifest} of the jar file requires to have a main attribute "POM"
 * and application modules also need to have a "MCR-Application-Module" main attribute present.
 *
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
@SuppressWarnings("PMD.MCR.ResourceResolver")
public class MCRRuntimeComponentDetector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Name ATT_POM = new Name("POM");

    private static final Name ATT_MCR_APPLICATION_MODULE = new Name("MCR-Application-Module");

    private static final Name ATT_MCR_ARTIFACT_ID = new Name("MCR-Artifact-Id");

    private static final SortedSet<MCRComponent> ALL_COMPONENTS_LOW_TO_HIGH =
        Collections.unmodifiableSortedSet(getConfiguredComponents());

    private static final SortedSet<MCRComponent> ALL_COMPONENTS_HIGH_TO_LOW = reverseOf(ALL_COMPONENTS_LOW_TO_HIGH);

    private static final SortedSet<MCRComponent> MYCORE_COMPONENTS_LOW_TO_HIGH =
        subsetOf(ALL_COMPONENTS_LOW_TO_HIGH, MCRComponent::isMyCoReComponent);

    private static final SortedSet<MCRComponent> MYCORE_COMPONENTS_HIGH_TO_LOW =
        reverseOf(MYCORE_COMPONENTS_LOW_TO_HIGH);

    private static final SortedSet<MCRComponent> APP_MODULES_LOW_TO_HIGH =
        subsetOf(ALL_COMPONENTS_LOW_TO_HIGH, MCRComponent::isAppModule);

    private static final SortedSet<MCRComponent> APP_MODULES_HIGH_TO_LOW = reverseOf(APP_MODULES_LOW_TO_HIGH);

    private static <T extends Comparable<T>> SortedSet<T> reverseOf(SortedSet<T> set) {
        return Collections.unmodifiableSortedSet(
            set.stream().collect(Collectors.toCollection(() -> new TreeSet<>(Collections.reverseOrder()))));
    }

    private static <T> SortedSet<T> subsetOf(SortedSet<T> set, Predicate<T> filter) {
        return Collections.unmodifiableSortedSet(
            set.stream().filter(filter).collect(Collectors.toCollection(TreeSet::new)));
    }

    private static SortedSet<MCRComponent> getConfiguredComponents() {
        try {
            String underTesting = System.getProperty("MCRRuntimeComponentDetector.underTesting");
            Enumeration<URL> resources = MCRClassTools.getClassLoader().getResources("META-INF/MANIFEST.MF");
            if (!resources.hasMoreElements() && underTesting == null) {
                LOGGER.warn("Did not find any Manifests.");
                return Collections.emptySortedSet();
            }
            SortedSet<MCRComponent> components = Sets.newTreeSet();
            while (resources.hasMoreElements()) {
                URL manifestURL = resources.nextElement();
                try (InputStream manifestStream = manifestURL.openStream()) {
                    Manifest manifest = new Manifest(manifestStream);
                    MCRComponent component = buildComponent(manifest, manifestURL);

                    if (component != null) {
                        components.add(component);
                    }
                }
            }
            if (underTesting != null) {
                //support JUnit-Tests
                MCRComponent component = new MCRComponent(underTesting, new Manifest());
                components.add(component);
            }
            return components;
        } catch (IOException e) {
            LOGGER.warn("Error while detecting MyCoRe components", e);
            return Sets.newTreeSet();
        }
    }

    private static MCRComponent buildComponent(Manifest manifest, URL manifestURL) throws IOException {
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes mainAttributes = manifest.getMainAttributes();
        String artifactId = mainAttributes.getValue(ATT_MCR_ARTIFACT_ID);
        String pomPropertiesPath = mainAttributes.getValue(ATT_POM);
        boolean usePomProperties = false;

        if (artifactId == null) {
            if (!mainAttributes.containsKey(ATT_POM)) {
                return null;
            }

            if (pomPropertiesPath == null) {
                return null;
            }

            try (InputStream pi = MCRClassTools.getClassLoader().getResourceAsStream(
                pomPropertiesPath)) {
                if (pi == null) {
                    LOGGER.warn("Manifest entry {} set to \"{}\", but resource could not be loaded.", ATT_POM,
                        pomPropertiesPath);
                    return null;
                }
                Properties pomProperties = new Properties();
                pomProperties.load(pi);
                artifactId = (String) pomProperties.get("artifactId");
                usePomProperties = true;
            }
        }

        if (artifactId != null && artifactId.startsWith("mycore-")
            || mainAttributes.containsKey(ATT_MCR_APPLICATION_MODULE)) {
            if (usePomProperties) {
                LOGGER.warn("No Attribute \"{}\" in Manifest of {}.", () -> ATT_MCR_ARTIFACT_ID,
                    () -> mainAttributes.getValue(ATT_MCR_APPLICATION_MODULE));
                LOGGER.warn("Change this in the future, pom.properties path definition is deprecated.");
                LOGGER.info("Using artifactId in {}.", pomPropertiesPath);
            }

            return new MCRComponent(artifactId, manifest, extractJarFile(manifestURL));
        }
        return null;
    }

    private static File extractJarFile(URL manifestURL) {
        try {
            if (manifestURL.toExternalForm().startsWith("jar:")) {
                return new File(new URI(manifestURL.getPath().replaceAll("!.*$", "")));
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Couldn't extract jar file path from MANIFEST.MF url.", e);
        }

        return null;
    }

    /**
     * Returns all components sorted via their natural ordering.
     *
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getAllComponents() {
        return ALL_COMPONENTS_LOW_TO_HIGH;
    }

    /**
     * Returns all components sorted via the given order
     *
     * @param order The order, defaults to {@link ComponentOrder#LOWEST_PRIORITY_FIRST}
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getAllComponents(ComponentOrder order) {
        return order == ComponentOrder.LOWEST_PRIORITY_FIRST ? ALL_COMPONENTS_LOW_TO_HIGH
            : ALL_COMPONENTS_HIGH_TO_LOW;
    }

    /**
     * Returns only mycore components sorted via their natural ordering.
     *
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getMyCoReComponents() {
        return MYCORE_COMPONENTS_LOW_TO_HIGH;
    }

    /**
     * Returns only mycore components sorted via the given order.
     *
     * @param order The order, defaults to {@link ComponentOrder#LOWEST_PRIORITY_FIRST}
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getMyCoReComponents(ComponentOrder order) {
        return order == ComponentOrder.LOWEST_PRIORITY_FIRST ? MYCORE_COMPONENTS_LOW_TO_HIGH
            : MYCORE_COMPONENTS_HIGH_TO_LOW;
    }

    /**
     * Returns only application modules sorted via their natural ordering.
     *
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getApplicationModules() {
        return APP_MODULES_LOW_TO_HIGH;
    }

    /**
     * Returns only application modules sorted via the given order.
     *
     * @param order The order, defaults to {@link ComponentOrder#LOWEST_PRIORITY_FIRST}
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getApplicationModules(ComponentOrder order) {
        return order == ComponentOrder.LOWEST_PRIORITY_FIRST ? APP_MODULES_LOW_TO_HIGH
            : APP_MODULES_HIGH_TO_LOW;
    }

    public enum ComponentOrder {

        LOWEST_PRIORITY_FIRST,

        HIGHEST_PRIORITY_FIRST;

    }

}
