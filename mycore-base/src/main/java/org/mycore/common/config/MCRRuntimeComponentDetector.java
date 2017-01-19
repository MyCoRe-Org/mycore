/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 3, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

/**
 * On first access this class detects all components, that is either MyCoRe components or application modules, that are
 * available via the current ClassLoader. Every {@link Manifest} of the jar file requires to have a main attribute "POM"
 * and application modules also need to have a "MCR-Application-Module" main attribute present.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRRuntimeComponentDetector {

    private static Logger LOGGER = LogManager.getLogger(MCRRuntimeComponentDetector.class);

    private static final Name ATT_POM = new Name("POM");

    private static final Name ATT_MCR_APPLICATION_MODULE = new Name("MCR-Application-Module");

    private static final Name ATT_MCR_ARTIFACT_ID = new Name("MCR-Artifact-Id");

    private static SortedSet<MCRComponent> ALL_COMPONENTS = Collections
        .unmodifiableSortedSet(getConfiguredComponents());

    private static SortedSet<MCRComponent> MYCORE_COMPONENTS = Collections.unmodifiableSortedSet(
        ALL_COMPONENTS.stream()
            .filter(MCRComponent::isMyCoReComponent)
            .collect(Collectors.toCollection(TreeSet::new)));

    private static SortedSet<MCRComponent> APP_MODULES = Collections.unmodifiableSortedSet(
        ALL_COMPONENTS.stream()
            .filter(MCRComponent::isAppModule)
            .collect(Collectors.toCollection(TreeSet::new)));

    private static SortedSet<MCRComponent> getConfiguredComponents() {
        try {
            String underTesting = System.getProperty("MCRRuntimeComponentDetector.underTesting");
            Enumeration<URL> resources = MCRRuntimeComponentDetector.class.getClassLoader().getResources(
                "META-INF/MANIFEST.MF");
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

            try (InputStream pi = MCRRuntimeComponentDetector.class.getClassLoader().getResourceAsStream(
                pomPropertiesPath)) {
                if (pi == null) {
                    LOGGER.warn("Manifest entry " + ATT_POM + " set to \"" + pomPropertiesPath
                        + "\", but resource could not be loaded.");
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
                LOGGER.warn("No Attribute \"" + ATT_MCR_ARTIFACT_ID + "\" in Manifest of "
                    + mainAttributes.getValue(ATT_MCR_APPLICATION_MODULE) + ".");
                LOGGER.warn("Change this in the future, pom.properties path definition is deprecated.");
                LOGGER.info("Using artifactId in " + pomPropertiesPath + ".");
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
        return ALL_COMPONENTS;
    }

    /**
     * Returns only mycore components sorted via their natural ordering.
     * 
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getMyCoReComponents() {
        return MYCORE_COMPONENTS;
    }

    /**
     * Returns only application modules sorted via their natural ordering.
     * 
     * @see MCRComponent#compareTo(MCRComponent)
     */
    public static SortedSet<MCRComponent> getApplicationModules() {
        return APP_MODULES;
    }

}
