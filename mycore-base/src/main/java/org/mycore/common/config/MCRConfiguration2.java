/**
 * 
 */
package org.mycore.common.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.function.MCRTriConsumer;

/**
 * DO NOT USE! Work in progress to discuss future development.
 * 
 * @author Thomas Scheffler (yagee)
 * @see <a href="https://mycore.atlassian.net/browse/MCR-1082">MCR-1082</a>
 */
public class MCRConfiguration2 {

    private static ConcurrentHashMap<UUID, EventListener> LISTENERS = new ConcurrentHashMap<>();

    public static Map<String, String> getPropertiesMap() {
        return MCRConfiguration.instance().getPropertiesMap();
    }

    public static Map<String, String> getPropertiesMap(final String startsWith) {
        return MCRConfiguration.instance().getPropertiesMap(startsWith);
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String, or null
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public static <T> Optional<T> getInstanceOf(String name) throws MCRConfigurationException {
        return getString(name).map(MCRConfiguration.instance()::loadClass);
    }

    /**
     * Returns a instance of the class specified in the configuration property with the given name. If the class was
     * previously instantiated by this method this instance is returned.
     * 
     * @param name
     *            non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public static <T> Optional<T> getSingleInstanceOf(String name) {
        return Optional.ofNullable(MCRConfiguration.instance().getSingleInstanceOf(name, null));
    }

    /**
     * Returns the configuration property with the specified name.
     * If the value of the property is empty after trimming the returned Optional is empty.
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as an {@link Optional Optional&lt;String&gt;}
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public static Optional<String> getString(String name) {
        return Optional
            .ofNullable(MCRConfiguration.instance().getString(name, null))
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

    private static MCRConfigurationException createConfigurationException(String propertyName) {
        return new MCRConfigurationException("Property '" + propertyName + "' is not set.");
    }

    /**
     * Returns the configuration property with the specified name as a list of strings. Values should be delimited by
     * ','
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a unmodifiable list of strings.
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public static List<String> getStrings(String name) {
        return getString(name)
            .map(MCRConfiguration.PROPERTY_SPLITTER::splitAsStream)
            .orElseThrow(() -> createConfigurationException(name))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
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
     * @throws MCRConfigurationException
     *             if the property with this name is not set
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
     * @throws MCRConfigurationException
     *             if the property with this name is not set
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
     * @throws MCRConfigurationException
     *             if the property with this name is not set
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
     * @throws MCRConfigurationException
     *             if the property with this name is not set
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
     * @throws MCRConfigurationException
     *             if the property with this name is not set
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
        Optional<String> oldValue = getString(name);
        MCRConfiguration.instance().set(name, value);
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

    private static class EventListener {

        private Predicate<String> keyPredicate;

        private MCRTriConsumer<String, Optional<String>, Optional<String>> listener;

        private UUID uuid;

        public EventListener(Predicate<String> keyPredicate,
            MCRTriConsumer<String, Optional<String>, Optional<String>> listener) {
            this.keyPredicate = keyPredicate;
            this.listener = listener;
            this.uuid = UUID.randomUUID();
        }

    }

}
