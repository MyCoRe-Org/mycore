/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an extract of properties (typically {@link MCRConfiguration2#getPropertiesMap()}) used to
 * instantiate an object. Provides methods to extract nested configurations.
 * <p>
 * Generally speaking, a configuration has a {@link MCRInstanceConfiguration#name()} that represents the
 * property key used to convey the {@link MCRInstanceConfiguration#className()} of the class that should
 * be instantiated and {@link MCRInstanceConfiguration#properties()} that are relevant for instantiation
 * (i.e. properties whose key originally started with the configuration name, but with that name removed
 * from the start of such keys; a configuration name of <code>Foo.Bar</code> and a property key of
 * <code>Foo.Bar.Baz</code> results in a property key <code>Baz</code> in the configurations properties).
 * <p>
 * Each configuration (top level or nested) keeps an unmodified  reference to the properties used to create
 * the top level configuration.
 */
public class MCRInstanceConfiguration {

    private final MCRInstanceName name;

    private final String className;

    private final Map<String, String> properties;

    private final Map<String, String> fullProperties;

    private MCRInstanceConfiguration(MCRInstanceName name, String className, Map<String, String> properties,
        Map<String, String> fullProperties) {
        this.name = name;
        this.className = className;
        this.properties = properties;
        this.fullProperties = fullProperties;
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(MCRInstanceName, Map)} that creates the name with
     * {@link MCRInstanceName#of(String)} and uses {@link MCRConfiguration2#getPropertiesMap()} as the properties.
     *
     * @param name the name
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofName(String name) {
        return ofName(MCRInstanceName.of(name), MCRConfiguration2.getPropertiesMap());
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(MCRInstanceName, Map)} that creates the name with
     * {@link MCRInstanceName#of(String)}.
     *
     * @param name the name
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofName(String name, Map<String, String> properties) {
        return ofName(MCRInstanceName.of(name), properties);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofName(MCRInstanceName, Map)} that uses
     * {@link MCRConfiguration2#getPropertiesMap()} as the properties.
     *
     * @param name the name
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofName(MCRInstanceName name) {
        return ofName(name, MCRConfiguration2.getPropertiesMap());
    }

    /**
     * Creates a new configuration based on the given properties.
     * <p>
     * Example: Given an {@link MCRInstanceName} <code>Some.Instance.Name</code> and properties
     * <ul>
     *     <li><code>Some.Instance.Name=some.instance.ClassName</code></li>
     *     <li><code>Some.Instance.Name.Key1=Value1</code></li>
     *     <li><code>Some.Instance.Name.Key2=Value1</code></li>
     * </ul>
     * this will return an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name</code>,
     * the {@link MCRInstanceConfiguration#className()} <code>some.instance.ClassName</code>,
     * {@link MCRInstanceConfiguration#properties()}
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value2</code></li>
     * </ul>
     * and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the given properties.
     * <p>
     * Alternatively, the {@link MCRInstanceName} <code>Some.Instance.Name.Class</code> or
     * <code>Some.Instance.Name.class</code> could have been used to convey the class name, in which case
     * the keys <code>Class</code>, <code>class</code> and the empty key, if present,
     * are not added to the {@link MCRInstanceConfiguration#properties()}.
     * <p>
     * Example: If <code>Some.Instance.Name.Class=some.instance.ClassName</code> would have been used
     * to convey the class name, properties <code>Some.Instance.Name.class=Foo</code> and
     * <code>Some.Instance.Name=Bar</code> would be ignored. The resulting
     * {@link MCRInstanceConfiguration#properties()} would not contains entries with keys <code>class</code>
     * or the empty key, respectively.
     *
     * @param name the name
     * @param properties the properties
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofName(MCRInstanceName name, Map<String, String> properties) {
        String className = properties.get(name.actual());
        Map<String, String> reducedProperties = reduceProperties(name, name.canonical(), properties);
        return new MCRInstanceConfiguration(name, className, reducedProperties, properties);
    }

    private static Map<String, String> reduceProperties(MCRInstanceName name, String prefix,
        Map<String, String> properties) {
        Map<String, String> reducedProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(prefix)) {
                if (key.length() != prefix.length() && key.charAt(prefix.length()) == '.') {
                    String reducedKey = key.substring(prefix.length() + 1);
                    if (!name.ignoredKeys().contains(reducedKey)) {
                        reducedProperties.put(reducedKey, value);
                    }
                }
            }
        }
        return reducedProperties;
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofClass(String, Map)} that uses the given classes name
     * and empty properties.
     *
     * @param instanceClass the class
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofClass(Class<?> instanceClass) {
        return ofClass(instanceClass.getName(), Collections.emptyMap());
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofClass(String, Map)} that uses the given classes name.
     *
     * @param instanceClass the class
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofClass(Class<?> instanceClass, Map<String, String> properties) {
        return ofClass(instanceClass.getName(), properties);
    }

    /**
     * Shorthand for {@link MCRInstanceConfiguration#ofClass(String, Map)} that uses empty properties.
     *
     * @param className the class name
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofClass(String className) {
        return ofClass(className, Collections.emptyMap());
    }

    /**
     * Creates a new configuration for a given class name and the given properties.
     * <p>
     * Example: Given a class name <code>some.instance.ClassName</code> and properties
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value1</code></li>
     * </ul>
     * this will return an {@link MCRInstanceConfiguration}
     * representing an empty {@link MCRInstanceConfiguration#name()},
     * the {@link MCRInstanceConfiguration#className()} <code>some.instance.ClassName</code>,
     * {@link MCRInstanceConfiguration#properties()}
     * <ul>
     *     <li><code>Key1=Value1</code></li>
     *     <li><code>Key2=Value2</code></li>
     * </ul>
     * and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the given properties.
     * <p>
     *
     * @param className the class name
     * @param properties the properties
     * @return the configuration
     */
    public static MCRInstanceConfiguration ofClass(String className, Map<String, String> properties) {
        MCRInstanceName name = MCRInstanceName.of(MCRInstanceName.Suffix.UPPER_CASE.representation().orElse(""));
        name.ignoredKeys().forEach(properties::remove);
        return new MCRInstanceConfiguration(name, className, properties, properties);
    }

    public MCRInstanceName name() {
        return name;
    }

    public String className() {
        return className;
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
     * {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassName</code>
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
    public MCRInstanceConfiguration nestedConfiguration(String prefix) {
        MCRInstanceName nestedName = name.subName(prefix);
        String className = properties.get(nestedName.suffix().appendTo(prefix));
        Map<String, String> reducedProperties = reduceProperties(nestedName, prefix, properties);
        return new MCRInstanceConfiguration(nestedName, className, reducedProperties, fullProperties);
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
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassNameA</code>
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
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassNameB</code>
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
    public Map<String, MCRInstanceConfiguration> nestedConfigurationMap() {
        Map<String, MCRInstanceConfiguration> nestedConfigurationMap = new HashMap<>();
        for (Map.Entry<String, String> entry : properties().entrySet()) {
            String key = entry.getKey();
            int index = key.indexOf('.');
            String nestedConfigurationKey = -1 == index ? key : key.substring(0, index);
            if (!nestedConfigurationMap.containsKey(nestedConfigurationKey)) {
                String nestedConfigurationSuffix = nestedConfigurationKey;
                nestedConfigurationMap.put(nestedConfigurationKey, nestedConfiguration(nestedConfigurationSuffix));
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
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassNameA</code>
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
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassNameB</code>
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
     * @param commonPrefix the common prefix
     * @return the nested configuration map
     */
    public Map<String, MCRInstanceConfiguration> nestedConfigurationMap(String commonPrefix) {
        String commonSuffixWithDelimiter = commonPrefix + ".";
        Map<String, MCRInstanceConfiguration> nestedConfigurationMap = new HashMap<>();
        for (Map.Entry<String, String> entry : properties().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(commonSuffixWithDelimiter)) {
                String remainingKey = key.substring(commonSuffixWithDelimiter.length());
                int index = remainingKey.indexOf('.');
                String nestedConfigurationKey = -1 == index ? remainingKey : remainingKey.substring(0, index);
                if (!nestedConfigurationMap.containsKey(nestedConfigurationKey)) {
                    String nestedConfigurationSuffix = commonSuffixWithDelimiter + nestedConfigurationKey;
                    nestedConfigurationMap.put(nestedConfigurationKey, nestedConfiguration(nestedConfigurationSuffix));
                }
            }
        }
        return nestedConfigurationMap;
    }

    /**
     * Returns a {@link List} of configurations for nested instances, ordered by the first name segment
     * (which all must be integer values).
     * <p>
     * Example: Given an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceName} <code>Some.Instance.Name</code>, properties
     * <ul>
     *     <li><code>1=some.nested.ClassName1</code></li>
     *     <li><code>1.Key1=ValueA1</code></li>
     *     <li><code>1.Key2=ValueA2</code></li>
     *     <li><code>3=some.nested.ClassName3</code></li>
     *     <li><code>3.Key1=ValueB1</code></li>
     *     <li><code>3.Key2=ValueB2</code></li>
     * </ul>
     * this will return a list containing
     * <ol>
     *     <li>
     *         at index <code>0</code> an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.1</code>,
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassName1</code> and
     *         {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueA1</code></li>
     *            <li><code>Key2=ValueA2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     *     <li>
     *         at index <code>1</code> an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.3</code>,
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassName3</code> and
     *         {@link MCRInstanceConfiguration#properties()}
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
     * in the top level configuration, properties <code>Foo.1.Class</code>/<code>Foo.3.Class</code> would be used to
     * convey the class name for the nested configurations and properties
     * <code>Foo.1.class</code>/<code>Foo.3.class</code> and <code>Foo.1</code>/<code>Foo.3</code> would be ignored.
     * The resulting {@link MCRInstanceConfiguration#properties()} would not contain entries with keys
     * <code>class</code> or the empty key, respectively.
     *
     * @return the nested configuration list
     */
    public List<MCRInstanceConfiguration> nestedConfigurationList() {
        return mapToList(nestedConfigurationMap());
    }

    /**
     * Returns a {@link List} of configurations for nested instances with a common prefix, ordered by the
     * name segment following that common prefix (which all must be integer values).
     * <p>
     * Example: Given an {@link MCRInstanceConfiguration}
     * representing the {@link MCRInstanceName} <code>Some.Instance.Name</code>, properties
     * <ul>
     *     <li><code>Foo.1=some.nested.ClassName1</code></li>
     *     <li><code>Foo.1.Key1=ValueA1</code></li>
     *     <li><code>Foo.1.Key2=ValueA2</code></li>
     *     <li><code>Foo.3=some.nested.ClassName3</code></li>
     *     <li><code>Foo.3.Key1=ValueB1</code></li>
     *     <li><code>Foo.3.Key2=ValueB2</code></li>
     *     <li><code>Bar=UnrelatedValue</code></li>
     * </ul>
     * and a <em>commonPrefix</em> of <code>Foo</code>, this will return a list containing
     * <ol>
     *     <li>
     *         at index <code>0</code> an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.1</code>,
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassName1</code> and
     *         {@link MCRInstanceConfiguration#properties()}
     *        <ul>
     *            <li><code>Key1=ValueA1</code></li>
     *            <li><code>Key2=ValueA2</code></li>
     *        </ul>
     *        and {@link MCRInstanceConfiguration#fullProperties()} that are equal to the the full properties of this
     *        configuration (i.e. the full properties used to create the top level configuration).
     *     </li>
     *     <li>
     *         at index <code>1</code> an {@link MCRInstanceConfiguration}
     *         representing the {@link MCRInstanceConfiguration#name()} <code>Some.Instance.Name.Foo.3</code>,
     *         {@link MCRInstanceConfiguration#className()} <code>some.nested.ClassName3</code> and
     *         {@link MCRInstanceConfiguration#properties()}
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
     * in the top level configuration, properties <code>Foo.1.Class</code>/<code>Foo.3.Class</code> would be used to
     * convey the class name for the nested configurations and properties
     * <code>Foo.1.class</code>/<code>Foo.3.class</code> and <code>Foo.1</code>/<code>Foo.3</code> would be ignored.
     * The resulting {@link MCRInstanceConfiguration#properties()} would not contain entries with keys
     * <code>class</code> or the empty key, respectively.
     *
     * @param commonPrefix the common prefix
     * @return the nested configuration list
     */
    public List<MCRInstanceConfiguration> nestedConfigurationList(String commonPrefix) {
        return mapToList(nestedConfigurationMap(commonPrefix));
    }

    private List<MCRInstanceConfiguration> mapToList(Map<String, MCRInstanceConfiguration> map) {
        return map
            .entrySet()
            .stream()
            .map(entry -> Map.entry(Integer.parseInt(entry.getKey()), entry.getValue()))
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MCRInstanceConfiguration {" +
            "name=" + name + ", " +
            "className=" + className + ", " +
            "properties=" + properties + "}";
    }

}
