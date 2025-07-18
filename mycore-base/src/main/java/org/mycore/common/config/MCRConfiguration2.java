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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRClassTools;
import org.mycore.common.function.MCRTriConsumer;

/**
 * Provides methods to manage and read all configuration properties from the MyCoRe configuration files.
 * The Properties used by this class are used from {@link MCRConfigurationBase}.
 * <h2>NOTE</h2>
 * <p><strong>All {@link Optional} values returned by this class are {@link Optional#empty() empty} if the property
 * is not set OR the trimmed value {@link String#isEmpty() is empty}. If you want to distinguish between
 * empty properties and unset properties use {@link MCRConfigurationBase#getString(String)} instead.</strong>
 * </p>
 * <p>
 * Using this class is very easy, here is an example:
 * </p>
 * {@snippet :
 * // Get a configuration property as a String:
 * String sValue = MCRConfiguration2.getString("MCR.String.Value").orElse(defaultValue);
 *
 * // Get a configuration property as a List of String (values are seperated by ","):
 * List<String> lValue = MCRConfiguration2.getString("MCR.StringList.Value").stream()
 *     .flatMap(MCRConfiguration2::splitValue)
 *     .collect(Collectors.toList());
 *
 * // Get a configuration property as a long array (values are seperated by ","):
 * long[] la = MCRConfiguration2.getString("MCR.LongList.Value").stream()
 *     .flatMap(MCRConfiguration2::splitValue)
 *     .mapToLong(Long::parseLong)
 *     .toArray();
 *
 * // Get a configuration property as an int, use 500 as default if not set:
 * int max = MCRConfiguration2.getInt("MCR.Cache.Size").orElse(500);
 * }
 *
 * There are some helper methods to help you with converting values
 * <ul>
 *     <li>{@link #getOrThrow(String, Function)}</li>
 *     <li>{@link #splitValue(String)}</li>
 *     <li>{@link #instantiateClass(Class, String)}</li>
 * </ul>
 *
 * As you see, the class provides methods to get configuration properties as different data types and allows you to
 * specify defaults. All MyCoRe configuration properties should start with "<CODE>MCR.</CODE>"
 * <p>
 * Using the <CODE>set</CODE> methods allows client code to set new configuration properties or
 * overwrite existing ones with new values.
 *
 * @author Thomas Scheffler (yagee)
 * @since 2018.05
 */
// because of intrusive test case org.mycore.common.config.MCRConfigurationTest.testSingletonMapGet
@SuppressWarnings("PMD.MutableStaticState") 
public class MCRConfiguration2 {

    private static final Map<UUID, EventListener> LISTENERS = new ConcurrentHashMap<>();

    static Map<SingletonKey, Object> instanceHolder = new MCRConcurrentHashMap<>();

    public static Map<String, String> getPropertiesMap() {
        return Collections.unmodifiableMap(MCRConfigurationBase.getResolvedProperties().getAsMap());
    }

    /**
     * Returns a sub map of properties where key is transformed.
     *
     * <ol>
     *     <li>if property starts with <code>propertyPrefix</code>, the property is in the result map</li>
     *     <li>the key of the target map is the name of the property without <code>propertPrefix</code></li>
     * </ol>
     * Example for <code>propertyPrefix="MCR.Foo."</code>:
     * {@snippet lang=Properties :
     *     MCR.Foo.Bar=Baz
     *     MCR.Foo.Hello=World
     *     MCR.Other.Prop=Value
     * }
     * will result in
     * {@snippet lang=Properties :
     *     Bar=Baz
     *     Hello=World
     * }
     * @param propertyPrefix prefix of the property name
     * @return a map of the properties as stated above
     */
    public static Map<String, String> getSubPropertiesMap(String propertyPrefix) {
        return MCRConfigurationBase.getResolvedProperties()
            .getAsMap()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(propertyPrefix))
            .collect(Collectors.toMap(e -> e.getKey().substring(propertyPrefix.length()), Map.Entry::getValue));
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name the non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException if the class can not be loaded or instantiated
     */
    public static <S> Optional<S> getInstanceOf(Class<S> superClass, String name) throws MCRConfigurationException {
        return getInstanceOf(superClass, name, MCRConfigurableInstanceHelper.NO_OPTIONS);
    }

    private static <S> Optional<S> getInstanceOf(Class<S> superClass, String name,
        Set<MCRConfigurableInstanceHelper.Option> options) {
        if (MCRConfigurableInstanceHelper.isSingleton(superClass)) {
            return getSingleInstanceOf(superClass, name, options);
        } else {
            return MCRConfigurableInstanceHelper.getInstance(superClass, name, options);
        }
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name the non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException if the class can not be loaded or instantiated
     * or the configuration property is not set
     */
    public static <S> S getInstanceOfOrThrow(Class<S> superClass, String name) throws MCRConfigurationException {
        return getInstanceOf(superClass, name, MCRConfigurableInstanceHelper.ADD_IMPLICIT_CLASS_PROPERTIES)
            .orElseThrow(() -> createConfigurationException(name));
    }

    /**
     * Returns an instance of the class specified in the configuration property with the given name. If the class was
     * previously instantiated by this method, that instance is returned.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException if the class can not be loaded or instantiated
     */
    public static <S> Optional<S> getSingleInstanceOf(Class<S> superClass, String name) {
        return getSingleInstanceOf(superClass, name, MCRConfigurableInstanceHelper.NO_OPTIONS);
    }

    private static <S> Optional<S> getSingleInstanceOf(Class<S> superClass, String name,
        Set<MCRConfigurableInstanceHelper.Option> options) {
        return getString(name)
            .map(className -> new ConfigSingletonKey(name, className))
            .map(key -> (S) instanceHolder.computeIfAbsent(key,
                k -> MCRConfigurableInstanceHelper.getInstance(superClass, name, options).orElse(null)));
    }

    /**
     * Returns an instance of the class specified in the configuration property with the given name. If the class was
     * previously instantiated by this method, that instance is returned.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException if the class can not be loaded or instantiated
     * or the configuration property is not set
     */
    public static <S> S getSingleInstanceOfOrThrow(Class<S> superClass, String name) {
        return getSingleInstanceOf(superClass, name, MCRConfigurableInstanceHelper.ADD_IMPLICIT_CLASS_PROPERTIES)
            .orElseThrow(() -> createConfigurationException(name));
    }

    /**
     * Loads a Java Class defined in property <code>name</code>.
     * @param name Name of the property
     * @param <T> Supertype of class defined in <code>name</code>
     * @return Optional of Class asignable to <code>&lt;T&gt;</code>
     * @throws MCRConfigurationException
     *             if the the class can not be loaded or instantiated
     */
    public static <T> Optional<Class<? extends T>> getClass(String name) throws MCRConfigurationException {
        return getString(name).map(MCRConfiguration2::<T>getClassObject);
    }

    /**
     * Returns the configuration property with the specified name.
     * If the value of the property is empty after trimming the returned Optional is empty.
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as an {@link Optional Optional&lt;String&gt;}
     */
    public static Optional<String> getString(String name) {
        return MCRConfigurationBase.getString(name)
            .map(String::trim)
            .filter(s -> !s.isEmpty());
    }

    /**
     * Returns the configuration property with the specified name as String.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @throws MCRConfigurationException
     *             if property is not set
     */
    public static String getStringOrThrow(String name) {
        return getString(name).orElseThrow(() -> createConfigurationException(name));
    }

    /**
     * Returns the configuration property with the specified name.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param mapper
     *            maps the String value to the return value
     * @throws MCRConfigurationException
     *             if property is not set
     */
    public static <T> T getOrThrow(String name, Function<String, ? extends T> mapper) {
        return getString(name).map(mapper).orElseThrow(() -> createConfigurationException(name));
    }

    public static MCRConfigurationException createConfigurationException(String propertyName) {
        return new MCRConfigurationException("Configuration property " + propertyName + " is not set.");
    }

    /**
     * Splits a String value in a Stream of trimmed non-empty Strings.
     * <p>
     * This method can be used to split a property value delimited by ',' into values.
     *
     * <p>
     *     Example:
     * </p>
     * <p>
     * <code>
     *     MCRConfiguration2.getOrThrow("MCR.ListProp", MCRConfiguration2::splitValue)<br>
     *         .map(Integer::parseInt)<br>
     *         .collect(Collectors.toList())<br>
     * </code>
     * </p>
     * @param value a property value
     * @return a Stream of trimmed, non-empty Strings
     */
    public static Stream<String> splitValue(String value) {
        return MCRConfigurationBase.PROPERTY_SPLITTER.splitAsStream(value)
            .map(String::trim)
            .filter(s -> !s.isEmpty());
    }

    /**
     * @return a list of properties which represent a configurable class
     */
    public static Stream<String> getInstantiatablePropertyKeys(String prefix) {
        return getSubPropertiesMap(prefix).entrySet()
            .stream()
            .filter(es -> {
                boolean result;
                String s = es.getKey();
                if (!s.contains(".")) {
                    result = true;
                } else {
                    result = (s.endsWith(".class") || s.endsWith(".Class")) &&
                        !s.substring(0, s.length() - ".class".length()).contains(".");
                }
                return result;
            })
            .filter(es -> es.getValue() != null)
            .filter(es -> !es.getValue().isBlank())
            .map(Map.Entry::getKey)
            .map(prefix::concat);
    }

    /**
     * Gets a list of properties which represent a configurable class and turns them in to a map.
     * @return a map where the key is a String describing the configurable instance value
     */
    public static <S> Map<String, Callable<S>> getInstances(Class<S> superClass, String prefix) {
        return getInstantiatablePropertyKeys(prefix)
            .collect(Collectors.toMap(
                k -> MCRInstanceName.of(k).canonical().substring(prefix.length()),
                v -> () -> getInstanceOf(superClass, v).orElse(null)));
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as an <CODE>int</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is not an <CODE>int</CODE> value
     */
    public static Optional<Integer> getInt(String name) throws NumberFormatException {
        return getString(name).map(Integer::parseInt);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>long</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>long</CODE> value
     */
    public static Optional<Long> getLong(String name) throws NumberFormatException {
        return getString(name).map(Long::parseLong);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>float</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>float</CODE> value
     */
    public static Optional<Float> getFloat(String name) throws NumberFormatException {
        return getString(name).map(Float::parseFloat);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * double</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>double
     *         </CODE> value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>double</CODE> value
     */
    public static Optional<Double> getDouble(String name) throws NumberFormatException {
        return getString(name).map(Double::parseDouble);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return <CODE>true</CODE>, if and only if the specified property has the value <CODE>true</CODE>
     */
    public static Optional<Boolean> getBoolean(String name) {
        return getString(name).map(Boolean::parseBoolean);
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * String</CODE> value. If the parameter <CODE>value</CODE> is <CODE>
     * null</CODE>, the property will be deleted.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property, possibly <CODE>
     *            null</CODE>
     */
    public static void set(final String name, String value) {
        Optional<String> oldValue = MCRConfigurationBase.getStringUnchecked(name);
        MCRConfigurationBase.set(name, value);
        LISTENERS
            .values()
            .stream()
            .filter(el -> el.keyPredicate.test(name))
            .forEach(el -> el.listener.accept(name, oldValue, Optional.ofNullable(value)));
    }

    public static void set(String name, Supplier<String> value) {
        set(name, value.get());
    }

    public static <T> void set(String name, T value, Function<T, String> mapper) {
        set(name, mapper.apply(value));
    }

    /**
     * Adds a listener that is called after a new value is set.
     *
     * @param keyPredicate
     *            a filter upon the property name that if matches executes the listener
     * @param listener
     *            a {@link MCRTriConsumer} with property name as first argument and than old and new value as Optional.
     * @return a UUID to {@link #removePropertyChangeEventListener(UUID) remove the listener} later
     */
    public static UUID addPropertyChangeEventLister(Predicate<String> keyPredicate,
        MCRTriConsumer<String, Optional<String>, Optional<String>> listener) {
        EventListener eventListener = new EventListener(keyPredicate, listener);
        LISTENERS.put(eventListener.uuid, eventListener);
        return eventListener.uuid;
    }

    public static boolean removePropertyChangeEventListener(UUID uuid) {
        return LISTENERS.remove(uuid) != null;
    }

    public static <S> S instantiateClass(Class<S> superClass, String className) {
        return MCRConfigurableInstanceHelper.getInstance(superClass, MCRInstanceConfiguration.ofClass(className));
    }

    public static <S> Stream<S> instantiateClasses(Class<S> superClass, String propertyName) {
        return getString(propertyName)
            .map(MCRConfiguration2::splitValue)
            .orElseGet(Stream::empty)
            .map(className -> instantiateClass(superClass, className));
    }

    private static <T> Class<? extends T> getClassObject(String classname) {
        try {
            return MCRClassTools.forName(classname.trim());
        } catch (ClassNotFoundException ex) {
            throw new MCRConfigurationException("Could not load class.", ex);
        }
    }

    private static class EventListener {

        private Predicate<String> keyPredicate;

        private MCRTriConsumer<String, Optional<String>, Optional<String>> listener;

        private UUID uuid;

        EventListener(Predicate<String> keyPredicate,
            MCRTriConsumer<String, Optional<String>, Optional<String>> listener) {
            this.keyPredicate = keyPredicate;
            this.listener = listener;
            this.uuid = UUID.randomUUID();
        }

    }

    interface SingletonKey {
        String property();

        String classname();
    }

    record ConfigSingletonKey(String property, String classname) implements SingletonKey {

    }

}
