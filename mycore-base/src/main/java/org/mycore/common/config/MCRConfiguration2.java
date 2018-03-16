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

import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.function.MCRTriConsumer;
import org.mycore.common.inject.MCRInjectorConfig;

import com.google.inject.ConfigurationException;

/**
 * DO NOT USE! Work in progress to discuss future development.
 * 
 * @author Thomas Scheffler (yagee)
 * @see <a href="https://mycore.atlassian.net/browse/MCR-1082">MCR-1082</a>
 */
public class MCRConfiguration2 {

    private static ConcurrentHashMap<UUID, EventListener> LISTENERS = new ConcurrentHashMap<>();
    static Hashtable<SingletonKey, Object> instanceHolder = new Hashtable<>();

    public static Map<String, String> getPropertiesMap() {
        return MCRConfiguration.instance().getPropertiesMap();
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String, or null
     * @throws MCRConfigurationException
     *             if the class can not be loaded or instantiated
     */
    public static <T> Optional<T> getInstanceOf(String name) throws MCRConfigurationException {
        return getString(name).map(MCRConfiguration2::instantiateClass);
    }

    /**
     * Returns a instance of the class specified in the configuration property with the given name. If the class was
     * previously instantiated by this method this instance is returned.
     * 
     * @param name
     *            non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException
     *             if the class can not be loaded or instantiated
     */
    public static <T> Optional<T> getSingleInstanceOf(String name) {
        return getString(name)
            .map(className -> new SingletonKey(name, className))
            .map(key -> (T)instanceHolder.computeIfAbsent(key, k -> getInstanceOf(name).orElse(null)));
    }

    /**
     * Loads a Java Class defined in property <code>name</code>.
     * @param name Name of the property
     * @param <T> Supertype of class defined in <code>name</code>
     * @return Optional of Class asignable to <code>&lt;T&gt;</code>
     * @throws MCRConfigurationException
     *             if the the class can not be loaded or instantiated
     */
    public static <T> Optional<Class<? extends T>> getClass(String name) throws MCRConfigurationException{
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
     *
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
    public static Stream<String> splitValue(String value){
        return MCRConfigurationBase.PROPERTY_SPLITTER.splitAsStream(value)
            .map(String::trim)
            .filter(s -> !s.isEmpty());
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

    public static <T> T instantiateClass(String classname) {
        LogManager.getLogger().debug("Loading Class: {}", classname);

        T o = null;
        Class<? extends T> cl = getClassObject(classname);
        try {
            return MCRInjectorConfig.injector().getInstance(cl);
        } catch (ConfigurationException e) {
            // no default or injectable constructor, check for singleton factory method
            try {
                return (T) Stream.of(cl.getMethods())
                    .filter(m -> m.getReturnType().isAssignableFrom(cl))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains("instance"))
                    .findAny()
                    .orElseThrow(() -> new MCRConfigurationException("Could not instantiate class " + classname, e))
                    .invoke(cl, (Object[]) null);
            } catch (ReflectiveOperationException r) {
                throw new MCRConfigurationException("Could not instantiate class " + classname, r);
            }
        }
    }

    private static <T> Class<? extends T> getClassObject(String classname) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> forName = (Class<? extends T>) Class.forName(classname.trim());
            return forName;
        } catch (ClassNotFoundException ex) {
            throw new MCRConfigurationException("Could not load class.", ex);
        }
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
    static class SingletonKey {
        private String property, className;

        public SingletonKey(String property, String className) {
            super();
            this.property = property;
            this.className = className;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((property == null) ? 0 : property.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SingletonKey other = (SingletonKey) obj;
            if (className == null) {
                if (other.className != null) {
                    return false;
                }
            } else if (!className.equals(other.className)) {
                return false;
            }
            if (property == null) {
                if (other.property != null) {
                    return false;
                }
            } else if (!property.equals(other.property)) {
                return false;
            }
            return true;
        }
    }

}
