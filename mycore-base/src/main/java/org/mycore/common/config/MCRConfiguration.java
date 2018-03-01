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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Provides methods to manage and read all configuration properties from the MyCoRe configuration files.
 * The Properties used by this class are initialized via {@link MCRConfigurationLoaderFactory}.
 * The class is implemented using the singleton pattern. Using this class is very easy, here is an example:
 *
 * <PRE>
 * // Get a configuration property as a String:
 * String driver = MCRConfiguration.instance().getString("MCR.JDBC.Driver");
 *
 * // Get a configuration property as an int, use 500 as default if not set:
 * int max = MCRConfiguration.instance().getInt("MCR.Cache.Size", 500);
 * </PRE>
 *
 * As you see, the class provides methods to get configuration properties as different data types and allows you to
 * specify defaults. All MyCoRe configuration properties should start with "<CODE>MCR.</CODE>"
 *
 * The class also provides methods for <B>listing and saving</B> all properties to an <CODE>OutputStream</CODE>.
 * Using the <CODE>set</CODE> methods allows
 * client code to set new configuration properties or overwrite existing ones with new values.
 * @see #list(PrintStream)
 * @see #store
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRConfiguration {

    /**
     * The single instance of this class that will be used at runtime
     */
    private static MCRConfiguration singleton;

    static {
        createSingleton();
    }

    /**
     * Returns the single instance of this class that can be used to read and manage the configuration properties.
     *
     * @return the single instance of <CODE>MCRConfiguration</CODE> to be used
     */
    public static MCRConfiguration instance() {
        return singleton;
    }

    /**
     * Use this method as a default value for {@link #getStrings(String, List)}.
     *
     * @return an empty list of Strings
     * @see Collections#emptyList()
     */
    public static List<String> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Instantiates the singleton by calling the protected constructor.
     */
    protected static void createSingleton() {
        try {
            singleton = new MCRConfiguration();
        } catch (IOException e) {
            throw new MCRConfigurationException("Could not instantiate MCRConfiguration.", e);
        }
        singleton.systemModified();
    }

    /**
     * return the given properties sorted by keys
     * @param props - properties to be sorted
     *                if props is null - an empty properties object that supports sorting by key will be created
     * @return a new properties object sorted by keys
     */
    public static Properties sortProperties(Properties props) {
        Properties sortedProps = new Properties() {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(super.keySet()));
            }
        };
        if (props != null) {
            sortedProps.putAll(props);
        }
        return sortedProps;
    }

    /**
     * returns the last point in time when the MyCoRe system was last modified. This method can help you to validate
     * caches not under your controll, e.g. client caches.
     *
     * @see System#currentTimeMillis()
     */
    public final long getSystemLastModified() {
        return MCRConfigurationBase.getSystemLastModified();
    }

    /**
     * signalize that the system state has changed. Call this method when ever you changed the persistency layer.
     */
    public final void systemModified() {
        MCRConfigurationBase.systemModified();
    }

    /**
     * Protected constructor to create the singleton instance
     */
    protected MCRConfiguration() throws IOException {
    }

    /**
     * Substitute all %properties%.
     */
    protected synchronized void resolveProperties() {
        MCRConfigurationBase.resolveProperties();
    }

    private MCRProperties getResolvedProperties() {
        return MCRConfigurationBase.getResolvedProperties();
    }

    private MCRProperties getBaseProperties() {
        return MCRConfigurationBase.getBaseProperties();
    }

    public MCRProperties getDeprecatedProperties() {
        return MCRConfigurationBase.getDeprecatedProperties();
    }

    public Map<String, String> getPropertiesMap() {
        return Collections.unmodifiableMap(getResolvedProperties().getAsMap());
    }

    /**
     * Returns all the properties beginning with the specified string
     *
     * @param startsWith
     *            the string all the returned properties start with
     * @return the list of properties
     */
    public Map<String, String> getPropertiesMap(final String startsWith) {
        return getPropertiesMap().entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(startsWith))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     *
     * @param name
     *            the non-null and non-empty qualified name of the configuration property
     * @param defaultname
     *            the qualified class name
     * @return Instance of the value of the configuration property
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public <T> T getInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        return defaultname == null ? MCRConfiguration2.getOrThrow(name, MCRConfiguration2::instantiateClass)
            : MCRConfiguration2.<T> getInstanceOf(name)
                .orElseGet(() -> MCRConfiguration2.<T> instantiateClass(defaultname));
    }

    /**
     * Returns a new instance of the class specified in the configuration property with the given name.
     *
     * @param name
     *            the non-null and non-empty qualified name of the configuration property
     * @param defaultObj
     *            the default object;
     * @return Instance of the value of the configuration property
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public <T> T getInstanceOf(String name, T defaultObj) {
        return MCRConfiguration2.<T> getInstanceOf(name).orElse(defaultObj);
    }

    /**
     * Loads a Java Class defined in property <code>name</code>.
     * @param name Name of the property
     * @param <T> Supertype of class defined in <code>name</code>
     * @return non null Class asignable to <code>&lt;T&gt;</code>
     * @throws MCRConfigurationException if property is not defined or class could not be loaded
     */
    public <T> Class<? extends T> getClass(String name) throws MCRConfigurationException{
        return MCRConfiguration2.<T> getClass(name)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(name));
    }

    /**
     * Loads a Java Class defined in property <code>name</code>.
     * @param name Name of the property
     * @param defaultClass default value if property is not defined
     * @param <T> Supertype of class defined in <code>name</code>
     * @return non null Class asignable to <code>&lt;T&gt;</code>
     */
    public <T> Class<? extends T> getClass(String name, Class<? extends T> defaultClass){
        return MCRConfiguration2.<T> getClass(name).orElse(defaultClass);
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
    public <T> T getInstanceOf(String name) throws MCRConfigurationException {
        return getInstanceOf(name, null);
    }

    /**
     * Returns a instance of the class specified in the configuration property with the given name. If the class was
     * previously instantiated by this method this instance is returned.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public <T> T getSingleInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        return MCRConfiguration2.<T> getSingleInstanceOf(name).map(Optional::of)
            .orElseGet(() -> Optional.ofNullable(defaultname)
                .map(className -> new MCRConfiguration2.SingletonKey(name, className))
                .map(key -> (T) MCRConfiguration2.instanceHolder.computeIfAbsent(key,
                    k -> MCRConfiguration2.instantiateClass(defaultname))))
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(name));
    }

    /**
     * Returns a instance of the class specified in the configuration property with the given name. If the class was
     * prevously instantiated by this method this instance is returned.
     *
     * @param name
     *            non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration property
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or instantiated
     */
    public <T> T getSingleInstanceOf(String name) {
        return getSingleInstanceOf(name, null);
    }

    /**
     * Returns the configuration property with the specified name as a String.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public String getString(String name) {
        return MCRConfigurationBase.getString(name).map(String::trim)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(name));
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
    public List<String> getStrings(String name) {
        return MCRConfigurationBase.getString(name)
            .map(MCRConfiguration2::splitValue)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(name))
            .collect(Collectors.toList());
    }

    /**
     * Returns the configuration property with the specified name as a list of strings. Values should be delimited by
     * ','
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the configuration property as a unmodifiable list of strings or <code>defaultValue</code>.
     */
    public List<String> getStrings(String name, List<String> defaultValue) {
        return MCRConfigurationBase.getString(name)
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElse(defaultValue);
    }

    /**
     * Returns the configuration property with the specified name as a String, or returns a given default value if the
     * property is not set.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the configuration property as a String
     */
    public String getString(String name, String defaultValue) {
        return MCRConfiguration2.getString(name).orElse(defaultValue);
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
    public int getInt(String name) throws NumberFormatException {
        return MCRConfiguration2.getOrThrow(name, Integer::parseInt);
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value, or returns a given default value if the property is not set.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property /** Returns the configuration property
     *            with the specified name as an <CODE>int</CODE> value, or returns a given default value if the property
     *            is not set.
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as an <CODE>int</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is set but is not an <CODE>int
     *             </CODE> value
     */
    public int getInt(String name, int defaultValue) throws NumberFormatException {
        return MCRConfiguration2.getInt(name).orElse(defaultValue);
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
    public long getLong(String name) throws NumberFormatException {
        return MCRConfiguration2.getOrThrow(name, Long::parseLong);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value, or returns a given default value if the property is not set.
     *
     * @return the value of the specified property as a <CODE>long</CODE> value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>long
     *             </CODE> value
     */
    public long getLong(String name, long defaultValue) throws NumberFormatException {
        return MCRConfiguration2.getLong(name).orElse(defaultValue);
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
    public float getFloat(String name) throws NumberFormatException {
        return MCRConfiguration2.getOrThrow(name, Float::parseFloat);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value, or returns a given default value if the property is not set.
     *
     * @return the value of the specified property as a <CODE>float</CODE> value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>
     *             float</CODE> value
     */
    public float getFloat(String name, float defaultValue) throws NumberFormatException {
        return MCRConfiguration2.getFloat(name).orElse(defaultValue);
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
    public double getDouble(String name) throws NumberFormatException {
        return MCRConfiguration2.getOrThrow(name, Double::parseDouble);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * double</CODE> value, or returns a given default value if the property is not set.
     *
     * @return the value of the specified property as a <CODE>double</CODE> value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>
     *             double</CODE> value
     */
    public double getDouble(String name, double defaultValue) throws NumberFormatException {
        return MCRConfiguration2.getDouble(name).orElse(defaultValue);
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
    public boolean getBoolean(String name) {
        return MCRConfiguration2.getOrThrow(name, Boolean::parseBoolean);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value, or returns a given default value if the property is not set. If the property is set and its
     * value is not <CODE>true
     * </CODE>, then <code>false</code> is returned.
     *
     * @return the value of the specified property as a <CODE>boolean</CODE> value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        return MCRConfiguration2.getBoolean(name).orElse(defaultValue);
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
    public void set(String name, String value) {
        MCRConfiguration2.set(name, value);
    }

    /**
     *  use {@link MCRConfigurationBase#initialize(Map, boolean)}
     */
    public synchronized void initialize(Map<String, String> props, boolean clear) {
        MCRConfigurationBase.initialize(props, clear);
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * int</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, int value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * long</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, long value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * float</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, float value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * double</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, double value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * boolean</CODE> value.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, boolean value) {
        set(name, String.valueOf(value));
    }

    /**
     * Lists all configuration properties currently set to a PrintStream. Useful for debugging, e. g. by calling
     * <P>
     * <CODE>MCRConfiguration.instance().list( System.out );</CODE>
     * </P>
     *
     * @see java.util.Properties#list( PrintStream )
     * @param out
     *            the PrintStream to list the configuration properties on
     */
    public void list(PrintStream out) {
        getResolvedProperties().list(out);
    }

    /**
     * Lists all configuration properties currently set to a PrintWriter. Useful for debugging.
     *
     * @see java.util.Properties#list( PrintWriter )
     * @param out
     *            the PrintWriter to list the configuration properties on
     */
    public void list(PrintWriter out) {
        getResolvedProperties().list(out);
    }

    /**
     * Stores all configuration properties currently set to an OutputStream.
     *
     * @see java.util.Properties#store
     * @param out
     *            the OutputStream to write the configuration properties to
     * @param header
     *            the header to prepend before writing the list of properties
     * @throws IOException
     *             if writing to the OutputStream throws an <CODE>IOException
     *             </CODE>
     */
    public void store(OutputStream out, String header) throws IOException {
        getResolvedProperties().store(out, header);
    }

    /**
     * Returns a String containing the configuration properties currently set. Useful for debugging, e. g. by calling
     * <P>
     * <CODE>System.out.println( MCRConfiguration.instance() );</CODE>
     * </P>
     *
     * @return a String containing the configuration properties currently set
     */
    @Override
    public String toString() {
        return getResolvedProperties().toString();
    }

}
