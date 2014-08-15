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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * On first access this class detects all components,
 * that is either MyCoRe components or application modules, that are available via the current ClassLoader.
 * 
 * Every {@link Manifest} of the jar file requires to have a main attribute "POM" and application modules
 * also need to have a "MCR-Application-Module" main attribute present.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRRuntimeComponentDetector {


    private static Logger LOGGER = Logger.getLogger(MCRRuntimeComponentDetector.class);

    private static final Name ATT_POM = new Name("POM");

    private static final Name ATT_MCR_APPLICATION_MODULE = new Name("MCR-Application-Module");
    
    private static final Name ATT_MCR_ARTIFACT_ID = new Name("MCR-Artifact-Id");

    private static SortedSet<MCRComponent> ALL_COMPONENTS = Collections
        .unmodifiableSortedSet(getConfiguredComponents());

    private static SortedSet<MCRComponent> MYCORE_COMPONENTS = Collections.unmodifiableSortedSet(Sets.filter(
        ALL_COMPONENTS, new Predicate<MCRComponent>() {
            @Override
            public boolean apply(MCRComponent input) {
                return input.isMyCoReComponent();
            }
        }));

    private static SortedSet<MCRComponent> APP_MODULES = Collections.unmodifiableSortedSet(Sets.filter(ALL_COMPONENTS,
        new Predicate<MCRComponent>() {
            @Override
            public boolean apply(MCRComponent input) {
                return input.isAppModule();
            }
        }));

    private static SortedSet<MCRComponent> getConfiguredComponents() {
        try {
            Enumeration<URL> resources = MCRRuntimeComponentDetector.class.getClassLoader().getResources(
                "META-INF/MANIFEST.MF");
            if (!resources.hasMoreElements()) {
                LOGGER.warn("Did not find any Manifests.");
                //TODO: JDK 8 will include Collections.emtpySortedSet()
                return Sets.newTreeSet();
            }
            SortedSet<MCRComponent> components = Sets.newTreeSet();
            while (resources.hasMoreElements()) {
                URL manifestURL = resources.nextElement();
                try (InputStream manifestStream = manifestURL.openStream()) {
                    Manifest manifest = new Manifest(manifestStream);
                    MCRComponent component = buildComponent(manifest);
                    
                    if (component != null) {
                        components.add(component);
                    }
                }
            }
            return components;
        } catch (IOException e) {
            LOGGER.warn("Error while detecting MyCoRe components", e);
            return Sets.newTreeSet();
        }
    }

    private static MCRComponent buildComponent(Manifest manifest) throws IOException {
        Attributes mainAttributes = manifest.getMainAttributes();
        String artifactId = mainAttributes.getValue(ATT_MCR_ARTIFACT_ID);
        
        if(artifactId == null){
            LOGGER.warn("Could not found " + ATT_MCR_ARTIFACT_ID + " in Manifest, add this for future compability.");
            LOGGER.warn("Try reading from pom.properties instead.");
            
            if (!mainAttributes.containsKey(ATT_POM)) {
                return null;
            }
            
            
            String pomPropertiesPath = (String) mainAttributes.get(ATT_POM);
            if (pomPropertiesPath == null) {
                return null;
            }
            
            try (InputStream pi = MCRRuntimeComponentDetector.class.getClassLoader().getResourceAsStream(pomPropertiesPath)) {
                if (pi == null) {
                    LOGGER.warn("Manifest entry " + ATT_POM + " set to \"" + pomPropertiesPath
                            + "\", but resource could not be loaded.");
                    return null;
                }
                Properties pomProperties = new Properties();
                pomProperties.load(pi);
                artifactId = (String) pomProperties.get("artifactId");
            }
        }
        
        if (artifactId != null && artifactId.startsWith("mycore-")
                || mainAttributes.containsKey(ATT_MCR_APPLICATION_MODULE)) {
            return new MCRComponent(artifactId, manifest);
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
