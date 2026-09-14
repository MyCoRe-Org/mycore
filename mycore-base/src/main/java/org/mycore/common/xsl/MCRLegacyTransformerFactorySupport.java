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

package org.mycore.common.xsl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.transform.TransformerFactory;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Compatibility support for class-based transformer factory selection.
 * <p>
 * Resolves legacy classes to a unique configured ID, preferring exact matches over subclasses.
 * Classes without a unique match receive an internal {@code legacy:} ID. Their factories are created
 * lazily, shared within the owning registry and always accessed serially.
 * The class-to-ID registration is independent of registry initialization so legacy properties can be
 * resolved while the registry is being constructed.
 */
final class MCRLegacyTransformerFactorySupport {

    private static final String CLASS_PROPERTY_SUFFIX = ".Class";

    private static final String LEGACY_FACTORY_ID_PREFIX = "legacy:";

    private static final ConcurrentMap<String, Class<? extends TransformerFactory>> LEGACY_FACTORY_CLASSES =
        new ConcurrentHashMap<>();

    private final ConcurrentMap<String, MCRSAXTransformerFactoryManager> factories = new ConcurrentHashMap<>();

    /**
     * Returns the shared legacy factory, or {@code null} if the ID has not been registered for a class.
     */
    MCRSAXTransformerFactoryManager get(String id) {
        Class<? extends TransformerFactory> factoryClass = LEGACY_FACTORY_CLASSES.get(id);
        return factoryClass == null ? null : factories.computeIfAbsent(id, _ -> createLegacyFactory(id, factoryClass));
    }

    static String getFactoryId(Class<? extends TransformerFactory> factoryClass,
        Map<String, MCRSAXTransformerFactoryManager> configuredFactories) {
        Map<String, Class<? extends TransformerFactory>> factoryClasses = new HashMap<>();
        configuredFactories.forEach((id, factory) -> factoryClasses.put(id, factory.getFactoryClass()));
        return resolveFactoryId(factoryClasses, factoryClass);
    }

    /**
     * Resolves an ID directly from properties without initializing the shared registry.
     * This keeps legacy-property migration out of the registry's construction path.
     */
    static String getFactoryId(Class<? extends TransformerFactory> factoryClass) {
        String propertyPrefix = MCRTransformerFactoryRegistry.CONFIGURATION_PREFIX + ".";
        Map<String, Class<? extends TransformerFactory>> factoryClasses = new HashMap<>();
        MCRConfiguration2.getSubpropertiesMap(propertyPrefix).keySet().stream()
            .filter(key -> key.endsWith(CLASS_PROPERTY_SUFFIX))
            .filter(key -> key.indexOf('.') == key.lastIndexOf('.'))
            .map(key -> key.substring(0, key.length() - CLASS_PROPERTY_SUFFIX.length()))
            .forEach(id -> MCRConfiguration2.<TransformerFactory>
                getClass(propertyPrefix + id + CLASS_PROPERTY_SUFFIX)
                .ifPresent(configuredClass -> factoryClasses.put(id, configuredClass)));
        return resolveFactoryId(factoryClasses, factoryClass);
    }

    private static String resolveFactoryId(Map<String, Class<? extends TransformerFactory>> factoryClasses,
        Class<? extends TransformerFactory> factoryClass) {
        List<String> exactMatches = factoryClasses.entrySet().stream()
            .filter(entry -> entry.getValue().equals(factoryClass))
            .map(Map.Entry::getKey)
            .toList();
        if (!exactMatches.isEmpty()) {
            return getConfiguredOrLegacyId(exactMatches, factoryClass);
        }
        List<String> assignableMatches = factoryClasses.entrySet().stream()
            .filter(entry -> factoryClass.isAssignableFrom(entry.getValue()))
            .map(Map.Entry::getKey)
            .toList();
        return getConfiguredOrLegacyId(assignableMatches, factoryClass);
    }

    private static String getConfiguredOrLegacyId(List<String> matches,
        Class<? extends TransformerFactory> factoryClass) {
        if (matches.size() == 1) {
            return matches.getFirst();
        }
        String legacyFactoryId = LEGACY_FACTORY_ID_PREFIX + factoryClass.getName();
        LEGACY_FACTORY_CLASSES.putIfAbsent(legacyFactoryId, factoryClass);
        return legacyFactoryId;
    }

    static boolean isLegacyFactoryId(String factoryId) {
        return factoryId.startsWith(LEGACY_FACTORY_ID_PREFIX);
    }

    private MCRSAXTransformerFactoryManager createLegacyFactory(String id,
        Class<? extends TransformerFactory> factoryClass) {
        TransformerFactory factory = TransformerFactory.newInstance(factoryClass.getName(),
            MCRClassTools.getClassLoader());
        return new MCRSAXTransformerFactoryManager(id, factory, true);
    }

}
