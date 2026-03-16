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

package org.mycore.common.config.instantiator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.source.MCRSource;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * Utility class providing functions commonly used by {@link MCRInstantiator} and related classes.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MCRInstantiatorUtils {

    private MCRInstantiatorUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(String property, String className) {
        try {
            return (Class<T>) MCRClassTools.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Missing class (" + className + ") configured in: " + property, e);
        }
    }

    public static String methodNames(List<Method> methods) {
        return methods.stream()
            .map(Method::getName)
            .collect(Collectors.joining(", "));
    }

    public static String targetTypeName(MCRTarget target) {
        return target.type().name().toLowerCase(Locale.ROOT);
    }

    public static String annotationClassNames(List<MCRSource> sources) {
        return sources.stream()
            .map(MCRInstantiatorUtils::annotationClassName)
            .collect(Collectors.joining(", "));
    }

    public static String annotationClassName(MCRSource source) {
        return source.annotationClass().getName();
    }

    public static String property(MCRInstanceConfiguration configuration, String annotationName) {
        if (Objects.equals("", annotationName)) {
            return configuration.name().canonical();
        } else {
            return configuration.name().canonical() + "." + annotationName;
        }
    }

    public static MCRConfigurationException emptyNameException(MCRTarget target) {
        return new MCRConfigurationException("The name for target " + targetTypeName(target) + " '" + target.name()
            + "' in configured class " + target.declaringClass().getName() + " must not be empty");
    }

    public static MCRConfigurationException incompatibilityException(String property, MCRTarget target,
        Class<?> annotationValueClass, Object instance) {
        throw new MCRConfigurationException("Instance of class " + instance.getClass().getName()
            + "', configured in " + property + ", is incompatible with annotated value class "
            + annotationValueClass.getName() + " for target " + targetTypeName(target)
            + " '" + target.name() + "' in configured class " + target.declaringClass().getName());
    }

    public static MCRConfigurationException missingException(String property, MCRTarget target,
        String description) {
        throw new MCRConfigurationException(
            capitalize(description) + ", configured in " + property + " (and its sub-properties)," +
                " for target " + targetTypeName(target) + " '" + target.name() + "' in configured class "
                + target.declaringClass().getName()
                + " is missing");
    }

    public static MCRConfigurationException emptyException(String property, MCRTarget target, String description) {
        throw new MCRConfigurationException(
            capitalize(description) + ", configured in " + property + " (and its sub-properties)," +
                " for target " + targetTypeName(target) + " '" + target.name() + "' in configured class "
                + target.declaringClass().getName()
                + " is empty");
    }

    public static MCRConfigurationException emptyRawException(String property, MCRTarget target,
        String description) {
        throw new MCRConfigurationException(
            capitalize(description) + ", configured in " + property + "," +
                " for target " + targetTypeName(target) + " '" + target.name() + "' in configured class "
                + target.declaringClass().getName()
                + " is empty");
    }

    public static MCRConfigurationException nonIntegerKeyException(String property, MCRTarget target,
        String key, String description, NumberFormatException exception) {
        return new MCRConfigurationException(
            capitalize(description) + ", configured in " + property + " (and its sub-properties)," +
                " for target " + targetTypeName(target) + " '" + target.name() + "' in configured class "
                + target.declaringClass().getName() + " is has element with non-integer key " + key, exception);
    }

    public static String capitalize(String description) {
        return description.substring(0, 1).toUpperCase(Locale.ROOT) + description.substring(1);
    }

    public static MCRConfigurationException inconsistentKeysException(String property, MCRTarget target,
        String key1, String key2, String description) {
        return new MCRConfigurationException(
            capitalize(description) + ", configured in " + property + " (and its sub-properties),"
                + " for target " + targetTypeName(target) + " '" + target.name() + "' in configured class "
                + target.declaringClass().getName() + " has element with inconsistent integer keys "
                + key1 + " and " + key2);
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    public static List<String> orderedKeys(String property, MCRTarget target, Map<String, ?> map,
        String description) {

        SortedMap<Integer, String> keyMap = new TreeMap<>();
        for (String key : map.keySet()) {
            try {
                Integer integerValue = Integer.parseInt(key);
                String alreadyMappedKey = keyMap.put(integerValue, key);
                if (alreadyMappedKey != null && !alreadyMappedKey.equals(key)) {
                    throw inconsistentKeysException(property, target, key, alreadyMappedKey, description);
                }
            } catch (NumberFormatException e) {
                throw nonIntegerKeyException(property, target, key, description, e);
            }
        }

        return new ArrayList<>(keyMap.values());

    }

}
