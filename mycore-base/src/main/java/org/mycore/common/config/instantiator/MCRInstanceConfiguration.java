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

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.instantiator.MCRInstanceName.Suffix;

/**
 * Represents an extract of properties (typically {@link MCRConfiguration2#getPropertiesMap()}) used to
 * instantiate an object. Provides methods to extract nested configurations.
 * <p>
 * Generally speaking, a configuration has a {@link MCRInstanceConfiguration#name()} that represents the
 * property key used to convey the {@link MCRInstanceConfiguration#valueClass()} of the class that should
 * be instantiated and {@link MCRInstanceConfiguration#properties()} that are relevant for instantiation
 * (i.e. properties whose key originally started with the configuration name, but with that name removed
 * from the start of such keys; a configuration name of <code>Foo.Bar</code> and a property key of
 * <code>Foo.Bar.Baz</code> results in a property key <code>Baz</code> in the configurations properties).
 * <p>
 * Each configuration (top level or nested) keeps an unmodified  reference to the properties used to create
 * the top level configuration.
 */
public final class MCRInstanceConfiguration<S> {

    public static final Set<Option> NO_OPTIONS = EnumSet.noneOf(Option.class);

    public static final Set<Option> IMPLICIT_OPTION = EnumSet.of(Option.IMPLICIT);

    private static final Logger LOGGER = LogManager.getLogger();

    private final Class<S> superClass;

    private final Class<? extends S> valueClass;

    private final MCRInstanceName name;

    private final Map<String, String> properties;

    private final Map<String, String> fullProperties;

    private MCRInstanceConfiguration(Class<S> superClass, Class<? extends S> valueClass, MCRInstanceName name,
        Map<String, String> properties, Map<String, String> fullProperties) {
        this.superClass = superClass;
        this.valueClass = valueClass;
        this.name = name;
        this.properties = properties;
        this.fullProperties = fullProperties;
        if (name.suffix() != Suffix.UPPER_CASE) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Using instance configuration for actual instance name {}, " +
                    "which doesn't have suffix '.Class'. Support for such names is deprecated" +
                    "and support will be removed in a future release of MyCoRe", name::actual);
            }
        }
    }

    public boolean instantiatable() {
        return valueClass != null;
    }

    public S instantiate() {
        return MCRInstantiator.instantiate(this);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofClass(Class, Class, String, Suffix, Map, Map)} that
     * creates uses {@link MCRClassTools#forName(String)} to resolve the value class.
     */
    public static <S> MCRInstanceConfiguration<S> ofClassName(Class<S> superClass, String className,
        String prefix, Suffix suffix, Map<String, String> properties, Map<String, String> fullProperties) {
        try {
            Class<? extends S> valueClass = MCRClassTools.forName(className);
            return ofClass(superClass, valueClass, prefix, suffix, properties, fullProperties);
        } catch (ClassNotFoundException e) {
            throw new MCRException("Failed to load class " + className, e);

        }
    }

    /**
     * Creates a new configuration for the given super class and value class based on the given properties.
     * <p>
     * Example: Given value class <code>Some.Instance.Name</code>, prefix <code>Some.Instance.Name</code>,
     * suffix {@link Suffix#NONE} and properties
     * <ul>
     *     <li><code>Some.Instance.Name.Key1=Value1</code></li>
     *     <li><code>Some.Instance.Name.Key2=Value1</code></li>
     * </ul>
     * this will return an {@link MCRInstanceConfiguration} representing the
     * {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name</code>,
     * the {@link MCRInstanceConfiguration#valueClass()} <code>some.instance.ClassName</code>,
     * {@link MCRInstanceConfiguration#properties()}
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value2</code></li>
     * </ul>
     * and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the given properties.
     * <p>
     * Alternatively, the {@link Suffix#LOWER_CASE} or {@link Suffix#NONE} could be used, in which case
     * the keys <code>Class</code>, <code>class</code> and the empty key, if present,
     * are not added to the {@link MCRInstanceConfiguration#properties()}.
     * <p>
     * Example: If prefix <code>Some.Instance.Name</code> and suffix {@link Suffix#UPPER_CASE}
     * would have been used, properties <code>Some.Instance.Name.class=Foo</code> and
     * <code>Some.Instance.Name=Bar</code> would be ignored. The resulting
     * {@link MCRInstanceConfiguration#properties()} would not contains entries with keys <code>class</code>
     * or the empty key, respectively.
     */
    public static <S> MCRInstanceConfiguration<S> ofClass(Class<S> superClass, Class<? extends S> valueClass,
        String prefix, Suffix suffix, Map<String, String> properties, Map<String, String> fullProperties) {
        MCRInstanceName name = MCRInstanceName.of(suffix.appendTo(prefix));
        name.ignoredKeys().forEach(properties::remove);
        return new MCRInstanceConfiguration<>(superClass, valueClass, name, properties, fullProperties);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(Class, MCRInstanceName, Map, Set)} that
     * creates the name with {@link MCRInstanceName#of(String)} and
     * uses {@link MCRConfiguration2#getPropertiesMap()} as the properties and
     * uses {@link MCRInstanceConfiguration#NO_OPTIONS} as the options.
     */
    public static <T> MCRInstanceConfiguration<T> ofName(Class<T> superClass, String name) {
        return ofName(superClass, MCRInstanceName.of(name), MCRConfiguration2.getPropertiesMap(), NO_OPTIONS);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(Class, MCRInstanceName, Map, Set)} that
     * creates the name with {@link MCRInstanceName#of(String)} and
     * uses {@link MCRInstanceConfiguration#NO_OPTIONS} as the options.
     */
    public static <T> MCRInstanceConfiguration<T> ofName(Class<T> superClass, String name,
        Map<String, String> properties) {
        return ofName(superClass, MCRInstanceName.of(name), properties, NO_OPTIONS);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(Class, MCRInstanceName, Map, Set)} that
     * creates the name with {@link MCRInstanceName#of(String)} and
     * uses {@link MCRConfiguration2#getPropertiesMap()} as the properties.
     */
    public static <T> MCRInstanceConfiguration<T> ofName(Class<T> superClass, String name,
        Set<Option> options) {
        return ofName(superClass, MCRInstanceName.of(name), MCRConfiguration2.getPropertiesMap(), options);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(Class, MCRInstanceName, Map, Set)} that
     * creates the name with {@link MCRInstanceName#of(String)} and.
     */
    public static <T> MCRInstanceConfiguration<T> ofName(Class<T> superClass, String name,
        Map<String, String> properties, Set<Option> options) {
        return ofName(superClass, MCRInstanceName.of(name), properties, options);
    }

    /**
     * Creates a new configuration for the given super class and instance name based on the given properties.
     * <p>
     * Example: Given an {@link MCRInstanceName} <code>Some.Instance.Name</code> and properties
     * <ul>
     *     <li><code>Some.Instance.Name=some.instance.ClassName</code></li>
     *     <li><code>Some.Instance.Name.Key1=Value1</code></li>
     *     <li><code>Some.Instance.Name.Key2=Value1</code></li>
     * </ul>
     * this will return an {@link MCRInstanceConfiguration} representing the
     * {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name</code>,
     * the {@link MCRInstanceConfiguration#valueClass()} <code>some.instance.ClassName</code>,
     * {@link MCRInstanceConfiguration#properties()}
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value2</code></li>
     * </ul>
     * and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the given properties.
     * <p>
     * Alternatively, the {@link MCRInstanceName} <code>Some.Instance.Name.Class</code> or
     * <code>Some.Instance.Name.class</code> could be used to convey the class name, in which case
     * the keys <code>Class</code>, <code>class</code> and the empty key, if present,
     * are not added to the {@link MCRInstanceConfiguration#properties()}.
     * <p>
     * Example: If <code>Some.Instance.Name.Class=some.instance.ClassName</code> would have been used
     * to convey the class name, properties <code>Some.Instance.Name.class=Foo</code> and
     * <code>Some.Instance.Name=Bar</code> would be ignored. The resulting
     * {@link MCRInstanceConfiguration#properties()} would not contains entries with keys <code>class</code>
     * or the empty key, respectively.
     */
    public static <T> MCRInstanceConfiguration<T> ofName(Class<T> superClass, MCRInstanceName name,
        Map<String, String> properties, Set<Option> options) {
        Class<? extends T> valueClass = getValueClass(superClass, name, name.actual(), properties, options);
        Map<String, String> reducedProperties = reduceProperties(name, name.canonical(), properties);
        return new MCRInstanceConfiguration<>(superClass, valueClass, name, reducedProperties, properties);
    }

    private static <S> Class<? extends S> getValueClass(Class<S> superClass, MCRInstanceName name,
        String classProperty, Map<String, String> properties, Set<Option> options) {
        String className = properties.get(classProperty);

        if (className != null) {
            if (className.isBlank()) {
                return null;
            }
            try {
                return MCRClassTools.forName(className);
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException("Missing class (" + className + ")" +
                    " configured in: " + name.actual(), e);
            }
        }

        if (options.contains(Option.IMPLICIT) && Modifier.isFinal(superClass.getModifiers())) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[IMPLICIT] Assuming implicit property {}={}", name.actual(), superClass.getName());
            }
            return superClass;
        }
        return null;

    }

    private static Map<String, String> reduceProperties(MCRInstanceName name, String prefix,
        Map<String, String> properties) {

        final String prefixWithDelimiter = prefix + '.';
        final int prefixWithDelimiterLength = prefixWithDelimiter.length();
        final List<String> ignoredKeys = name.ignoredKeys();

        Map<String, String> reducedProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefixWithDelimiter)) {
                continue;
            }
            String reducedKey = key.substring(prefixWithDelimiterLength);
            reducedProperties.put(reducedKey, entry.getValue());
        }
        String directProperty = properties.get(prefix);
        if (directProperty != null) {
            reducedProperties.put("", directProperty);
        }
        name.ignoredKeys().forEach(reducedProperties::remove);
        return reducedProperties;
    }

    public Class<S> superClass() {
        return superClass;
    }

    public Class<? extends S> valueClass() {
        return valueClass;
    }

    public MCRInstanceName name() {
        return name;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public Map<String, String> fullProperties() {
        return fullProperties;
    }

    /**
     * Returns the configuration for a nested instance.
     * <p>
     * Example: Given an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceName} <code>Some.Instance.Name</code>, properties
     * <ul>
     *     <li><code>Foo=some.nested.ClassName</code></li>
     *     <li><code>Foo.Key1=Value1</code></li>
     *     <li><code>Foo.Key2=Value2</code></li>
     *     <li><code>Bar=UnrelatedValue</code></li>
     * </ul>
     * and a <em>prefix</em> of <code>Foo</code>, this will return a an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo</code>,
     * {@link MCRInstanceConfiguration#valueClass()} <code>some.nested.ClassName</code>
     * and properties
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value2</code></li>
     * </ul>
     * <p>
     * If an {@link MCRInstanceName} with suffix <code>.Class</code> or <code>.class</code> would have been used in
     * the top level {@link MCRInstanceConfiguration}, the same suffix is used for nested configurations.
     * <p>
     * Example: If a property with suffix <code>.Class</code> would have been used to convey the class name in the
     * top level configuration, property <code>Foo.Class</code> would be used to convey the class name for the
     * nested configuration and properties <code>Foo.class</code> and <code>Foo</code> would be ignored.
     * The resulting {@link MCRInstanceConfiguration#properties()} would not contain entries with keys
     * <code>class</code> or the empty key, respectively.
     *
     * @param prefix the prefix
     * @return the nested configuration
     */
    public <N> MCRInstanceConfiguration<N> nested(Class<N> superClass, String prefix) {
        MCRInstanceName nestedName = name.subName(prefix);
        String classProperty = nestedName.suffix().appendTo(prefix);
        Class<? extends N> valueClass =
            getValueClass(superClass, nestedName, classProperty, properties, IMPLICIT_OPTION);
        Map<String, String> reducedProperties = reduceProperties(nestedName, prefix, properties);
        return new MCRInstanceConfiguration<>(superClass, valueClass, nestedName, reducedProperties, fullProperties);
    }

    /**
     * Returns a {@link Map} of configurations for nested instances, mapped by the first name segment.
     * <p>
     * Example: Given an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceName} <code>Some.Instance.Name</code>, properties
     * <ul>
     *     <li><code>A=come.nested.ClassNameA</code></li>
     *     <li><code>A.Key1=ValueA1</code></li>
     *     <li><code>A.Key2=ValueA2</code></li>
     *     <li><code>B=some.nested.ClassNameB</code></li>
     *     <li><code>B.Key1=ValueB1</code></li>
     *     <li><code>B.Key2=ValueB2</code></li>
     * </ul>
     * this will return a map containing
     * <ol>
     *     <li>
     *         an entry with key <code>A</code> mapping to an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.A</code>,
     *         {@link MCRInstanceConfiguration#valueClass()} <code>some.nested.ClassNameA</code>
     *         and {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueA1</code></li>
     *            <li><code>Key2=ValueA2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     *     <li>
     *         an entry with key <code>B</code> mapping to an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.B</code>,
     *         {@link MCRInstanceConfiguration#valueClass()} <code>some.nested.ClassNameB</code>
     *         and {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueB1</code></li>
     *            <li><code>Key2=ValueB2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     * </ol>
     * <p>
     * If an {@link MCRInstanceName} with suffix <code>.Class</code> or <code>.class</code> would have been used in
     * the top level {@link MCRInstanceConfiguration}, the same suffix is used for nested configurations.
     * <p>
     * Example: If a property with suffix <code>.Class</code> would have been used to convey the original class name
     * in the top level configuration, properties <code>Foo.A.Class</code>/<code>Foo.B.Class</code> would be used to
     * convey the class name for the nested configurations and properties
     * <code>Foo.A.class</code>/<code>Foo.B.class</code> and <code>Foo.A</code>/<code>Foo.B</code> would be ignored.
     * The resulting {@link MCRInstanceConfiguration#properties()} would not contain entries with keys
     * <code>class</code> or the empty key, respectively.
     *
     * @return the nested configuration map
     */
    public <N> Map<String, MCRInstanceConfiguration<N>> nestedMap(Class<N> superClass) {
        Map<String, MCRInstanceConfiguration<N>> nestedConfigurationMap = new HashMap<>();
        for (Map.Entry<String, String> entry : properties().entrySet()) {
            String key = entry.getKey();
            int index = key.indexOf('.');
            String nestedConfigurationKey = -1 == index ? key : key.substring(0, index);
            if (!nestedConfigurationMap.containsKey(nestedConfigurationKey)) {
                String nestedConfigurationSuffix = nestedConfigurationKey;
                nestedConfigurationMap.put(nestedConfigurationKey,
                    nested(superClass, nestedConfigurationSuffix));
            }
        }
        return nestedConfigurationMap;
    }

    /**
     * Returns a {@link Map} of configurations for nested instances with a common prefix, mapped by the
     * name segment following that common prefix.
     * <p>
     * Example: Given an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceName} <code>Some.Instance.Name</code>, properties
     * <ul>
     *     <li><code>Foo.A=come.nested.ClassNameA</code></li>
     *     <li><code>Foo.A.Key1=ValueA1</code></li>
     *     <li><code>Foo.A.Key2=ValueA2</code></li>
     *     <li><code>Foo.B=some.nested.ClassNameB</code></li>
     *     <li><code>Foo.B.Key1=ValueB1</code></li>
     *     <li><code>Foo.B.Key2=ValueB2</code></li>
     *     <li><code>Bar=UnrelatedValue</code></li>
     * </ul>
     * and a <em>commonPrefix</em> of <code>Foo</code>, this will return a map containing
     * <ol>
     *     <li>
     *         an entry with key <code>A</code> mapping to an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.A</code>,
     *         {@link MCRInstanceConfiguration#valueClass()} <code>some.nested.ClassNameA</code>
     *         and {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueA1</code></li>
     *            <li><code>Key2=ValueA2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     *     <li>
     *         an entry with key <code>B</code> mapping to an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.B</code>,
     *         {@link MCRInstanceConfiguration#valueClass()} <code>some.nested.ClassNameB</code>
     *         and {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueB1</code></li>
     *            <li><code>Key2=ValueB2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     * </ol>
     * <p>
     * If an {@link MCRInstanceName} with suffix <code>.Class</code> or <code>.class</code> would have been used in
     * the top level {@link MCRInstanceConfiguration}, the same suffix is used for nested configurations.
     * <p>
     * Example: If a property with suffix <code>.Class</code> would have been used to convey the original class name
     * in the top level configuration, properties <code>Foo.A.Class</code>/<code>Foo.B.Class</code> would be used to
     * convey the class name for the nested configurations and properties
     * <code>Foo.A.class</code>/<code>Foo.B.class</code> and <code>Foo.A</code>/<code>Foo.B</code> would be ignored.
     * The resulting {@link MCRInstanceConfiguration#properties()} would not contain entries with keys
     * <code>class</code> or the empty key, respectively.
     *
     * @param prefix the common prefix
     * @return the nested configuration map
     */
    public <N> Map<String, MCRInstanceConfiguration<N>> nestedMap(Class<N> superClass, String prefix) {
        if (prefix.isEmpty()) {
            return nestedMap(superClass);
        }
        String suffixWithDelimiter = prefix + ".";
        Map<String, MCRInstanceConfiguration<N>> nestedConfigurationMap = new HashMap<>();
        for (Map.Entry<String, String> entry : properties().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(suffixWithDelimiter)) {
                String remainingKey = key.substring(suffixWithDelimiter.length());
                int index = remainingKey.indexOf('.');
                String nestedConfigurationKey = -1 == index ? remainingKey : remainingKey.substring(0, index);
                if (!nestedConfigurationMap.containsKey(nestedConfigurationKey)) {
                    String nestedConfigurationSuffix = suffixWithDelimiter + nestedConfigurationKey;
                    nestedConfigurationMap.put(nestedConfigurationKey,
                        nested(superClass, nestedConfigurationSuffix));
                }
            }
        }
        return nestedConfigurationMap;
    }

    @Override
    public String toString() {
        return "MCRInstanceConfiguration {" +
            "superClass=" + superClass.getName() + ", " +
            "valueClass=" + valueClass.getName() + ", " +
            "name=" + name + ", " +
            "fullProperties=" + properties + ", " +
            "#fullProperties=" + fullProperties.size() + "}";
    }

    public enum Option {

        /**
         * If a class name is required to be in the configuration properties (for example, because of usage of
         * {@link MCRConfiguration2#getInstanceOfOrThrow(Class, String)} or because of an annotation with
         * <code>required=true</code>) and the expected super class is a final class (meaning, if the property
         * containing the class name is required to exist and can only have the class name of that final class),
         * assume that that property exists (if it doesn't).
         */
        IMPLICIT

    }

}
