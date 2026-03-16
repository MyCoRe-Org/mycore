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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mycore.common.config.MCRInstanceConfiguration.Option;
import org.mycore.common.config.MCRInstanceConfiguration.Options;
import org.mycore.common.config.instantiator.MCRInstantiator;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;

import jakarta.inject.Singleton;

/**
 * Creates Objects which are configured with properties.
 *
 * @author Sebastian Hofmann
 */
@SuppressWarnings({ "PMD.MCR.Singleton.ClassModifiers", "PMD.MCR.Singleton.PrivateConstructor",
    "PMD.MCR.Singleton.NonPrivateConstructors", "PMD.MCR.Singleton.MethodModifiers",
    "PMD.MCR.Singleton.MethodReturnType", "PMD.SingletonClassReturningNewInstance",
    "PMD.SingleMethodSingleton" })
public class MCRConfigurableInstanceHelper {

    private static final ConcurrentMap<String, MCRInstanceConfiguration> CONFIG_CACHE = new ConcurrentHashMap<>();

    static {
        MCRConfiguration2.addPropertyChangeEventLister(
            key -> true,
            (name, oldVal, newVal) -> CONFIG_CACHE.clear());
    }

    /**
     * Checks if a class is annotated with {@link Singleton}.
     *
     * @param property the configuration property which contains the class
     * @return true if the class in the property is annotated with {@link Singleton}
     */
    public static boolean isSingleton(String property) {
        return MCRConfiguration2.getString(property).stream()
            .anyMatch(propertyVal -> isSingleton(MCRInstantiatorUtils.getClass(property, propertyVal)));
    }

    /**
     * Checks if a class is annotated with {@link Singleton}.
     *
     * @param targetClass the class
     * @return true if the class in the property is annotated with {@link Singleton}
     */
    public static boolean isSingleton(Class<?> targetClass) {
        return targetClass.getDeclaredAnnotation(Singleton.class) != null;
    }

    /**
     * Shorthand for {@link #getInstance(Class, String, Set)} that uses no options.
     */
    public static <S> Optional<S> getInstance(Class<S> superClass, String name) throws MCRConfigurationException {
        return getInstance(superClass, name, Options.NONE);
    }

    /**
     * Creates a configured instance of a class.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name       the property which contains the class name
     * @param options    the options to be used
     * @return the configured instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <S> Optional<S> getInstance(Class<S> superClass, String name, Set<Option> options)
        throws MCRConfigurationException {
        MCRInstanceConfiguration configuration = CONFIG_CACHE.computeIfAbsent(name, MCRInstanceConfiguration::ofName);
        String className = configuration.className();
        if (isAbsent(className) && !options.contains(Option.IMPLICIT)) {
            return Optional.empty();
        }
        return Optional.of(getInstance(superClass, configuration, options));
    }

    private static boolean isAbsent(String className) {
        return className == null || className.isBlank();
    }

    /**
     * Shorthand for {@link #getInstance(Class, MCRInstanceConfiguration, Set)} that uses no options.
     */
    public static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration)
        throws MCRConfigurationException {
        return getInstance(superClass, configuration, Options.NONE);
    }

    /**
     * Creates a configured instance of a class.
     *
     * @param superClass    the intended super class of the instantiated class
     * @param configuration the configuration to be used
     * @param options       the options to be used
     * @return the configured instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration,
        Set<Option> options) throws MCRConfigurationException {
        return MCRInstantiator.getInstance(superClass, configuration, options);
    }

    public static void clearCache() {
        CONFIG_CACHE.clear();
    }

}
