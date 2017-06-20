/*
 * 
 * $Revision$ $Date$
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPropertiesResolver;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

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

    private Hashtable<SingletonKey, Object> instanceHolder = new Hashtable<SingletonKey, Object>();

    private File lastModifiedFile;

    static final Pattern PROPERTY_SPLITTER = Pattern.compile(",");

    /**
     * The properties instance that stores the values that have been read from every configuration file. These
     * properties are unresolved
     */
    protected MCRProperties baseProperties;

    /**
     * The same as baseProperties but all %properties% are resolved.
     */
    protected MCRProperties resolvedProperties;

    /**
     * List of deprecated properties with their new name
     */
    protected MCRProperties deprecatedProperties;

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
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
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
        return lastModifiedFile.lastModified();
    }

    /**
     * signalize that the system state has changed. Call this method when ever you changed the persistency layer.
     */
    public final void systemModified() {
        if (!lastModifiedFile.exists()) {
            try {
                createLastModifiedFile();
            } catch (IOException ioException) {
                throw new MCRException("Could not change modify date of file " + lastModifiedFile.getAbsolutePath(),
                    ioException);
            }
        } else if (!lastModifiedFile.setLastModified(System.currentTimeMillis())) {
            // a problem occurs, when a linux user other than the file owner
            // tries to change the last modified date
            // @see Java Bug:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4466073
            // fixable in Java7 with setTimes() method of new file system API
            // workaround for now: try to recreate the file
            // @author Robert Stephan
            FileOutputStream fout = null;
            try {
                try {
                    fout = new FileOutputStream(lastModifiedFile);
                    fout.write(new byte[0]);
                    lastModifiedFile.setWritable(true, false);
                } finally {
                    if (fout != null) {
                        fout.close();
                    }
                }
            } catch (IOException e) {
                throw new MCRException("Could not change modify date of file " + lastModifiedFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Protected constructor to create the singleton instance
     */
    protected MCRConfiguration() throws IOException {
        baseProperties = new MCRProperties();
        resolvedProperties = new MCRProperties();
        deprecatedProperties = new MCRProperties();
        loadDeprecatedProperties();
        createLastModifiedFile();
    }

    /**
     * Creates a new .systemTime file in MCR.datadir.
     */
    protected void createLastModifiedFile() throws IOException {
        final String dataDirKey = "MCR.datadir";
        if (getResolvedProperties().containsKey(dataDirKey)) {
            File dataDir = new File(getString(dataDirKey));
            if (dataDir.exists() && dataDir.isDirectory()) {
                lastModifiedFile = new File(getString(dataDirKey), ".systemTime");
            } else {
                System.err.println("WARNING: MCR.dataDir does not exist: " + dataDir.getAbsolutePath());
            }
        }
        if (lastModifiedFile == null) {
            try {
                lastModifiedFile = File.createTempFile("MyCoRe", ".systemTime");
                lastModifiedFile.deleteOnExit();
            } catch (IOException e) {
                throw new MCRException("Could not create temporary file, please set property MCR.datadir");
            }
        }
        if (!lastModifiedFile.exists()) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(lastModifiedFile);
                fout.write(new byte[0]);
            } finally {
                if (fout != null) {
                    fout.close();
                }
            }
            //allow other users to change this file
            lastModifiedFile.setWritable(true, false);
        }
    }

    private void debug() {
        Properties tmp = null;
        String comments = "Active mycore properties";
        File resolvedPropertiesFile = MCRConfigurationDir.getConfigFile("mycore.resolved.properties");
        if (resolvedPropertiesFile != null) {
            tmp = MCRConfiguration.sortProperties(getResolvedProperties());
            try (FileOutputStream fout = new FileOutputStream(resolvedPropertiesFile)) {
                tmp.store(fout, comments + "\nDo NOT edit this file!");
            } catch (IOException e) {
                LogManager.getLogger()
                    .warn("Could not store resolved properties to " + resolvedPropertiesFile.getAbsolutePath(), e);
            }
        }

        Logger logger = LogManager.getLogger();
        if (logger.isDebugEnabled()) {
            try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
                tmp = tmp == null ? MCRConfiguration.sortProperties(getResolvedProperties()) : tmp;
                tmp.store(out, comments);
                out.flush();
                sw.flush();
                logger.debug(sw.toString());
            } catch (IOException e) {
                logger.debug("Error while debugging mycore properties.", e);
            }
        }
    }

    /**
     * Substitute all %properties%.
     */
    protected synchronized void resolveProperties() {
        MCRProperties tmpProperties = MCRProperties.copy(getBaseProperties());
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(tmpProperties);
        resolvedProperties = MCRProperties.copy(resolver.resolveAll(tmpProperties));
    }

    /**
     * Loads file deprecated.properties that can be used to rename old properties. The file contains a list of renamed
     * properties: OldPropertyName=NewPropertyName. The old property is automatically replaced with the new name, so
     * that existing mycore.properties files must not be migrated immediately.
     */
    private void loadDeprecatedProperties() {
        InputStream in = this.getClass().getResourceAsStream("/deprecated.properties");
        if (in == null) {
            return;
        }
        try {
            getDeprecatedProperties().load(in);
            in.close();
        } catch (Exception exc) {
            throw new MCRConfigurationException("Could not load configuration file deprecated.properties", exc);
        }
    }

    private void checkForDeprecatedProperties(Map<String, String> props) {
        Map<String, String> depUsedProps = props.entrySet().stream()
            .filter(e -> getDeprecatedProperties().containsKey(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, e -> getDeprecatedProperties().getAsMap().get(e.getKey())));
        if (!depUsedProps.isEmpty()) {
            throw new MCRConfigurationException(
                depUsedProps.entrySet().stream().map(e -> e.getKey() + " ==> " + e.getValue())
                    .collect(Collectors.joining("\n",
                        "Found deprecated properties that are defined but will NOT BE USED. Please use the replacements:\n",
                        "\n")));
        }
    }

    private void checkForDeprecatedProperty(String name) throws MCRConfigurationException {
        if (getDeprecatedProperties().containsKey(name)) {
            throw new MCRConfigurationException("Cannot set deprecated property " + name + ". Please use "
                + getDeprecatedProperties().getProperty(name) + " instead.");
        }
    }

    private MCRProperties getResolvedProperties() {
        return resolvedProperties;
    }

    private MCRProperties getBaseProperties() {
        return baseProperties;
    }

    public MCRProperties getDeprecatedProperties() {
        return deprecatedProperties;
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
        String classname = getString(name, defaultname);
        if (classname == null) {
            throw new MCRConfigurationException("Configuration property missing: " + name);
        }

        return this.<T> loadClass(classname);
    }

    <T> T loadClass(String classname) {
        LogManager.getLogger().debug("Loading Class: " + classname);

        T o = null;
        Class<? extends T> cl;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> forName = (Class<? extends T>) Class.forName(classname);
            cl = forName;
        } catch (ClassNotFoundException ex) {
            throw new MCRConfigurationException("Could not load class " + classname, ex);
        }

        try {
            try {
                o = cl.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                // check for singleton
                Method[] querymethods = cl.getMethods();

                for (Method querymethod : querymethods) {
                    if (querymethod.getName().toLowerCase(Locale.ROOT).equals("instance")
                        || querymethod.getName().toLowerCase(Locale.ROOT).equals("getinstance")) {
                        Object[] ob = new Object[0];
                        @SuppressWarnings("unchecked")
                        T invoke = (T) querymethod.invoke(cl, ob);
                        o = invoke;
                        break;
                    }
                }
                if (o == null) {
                    throw e;
                }
            }
        } catch (Throwable t) {
            String msg = "Could not instantiate class " + classname;
            if (t instanceof ExceptionInInitializerError) {
                Throwable t2 = ((ExceptionInInitializerError) t).getException();
                throw new MCRConfigurationException(msg, t2);
            } else {
                throw new MCRConfigurationException(msg, t);
            }
        }
        return o;
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
        String classname = getString(name, null);
        if (classname == null) {
            return defaultObj;
        }

        return this.<T> loadClass(classname);
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
        return getInstanceOf(name, (String) null);
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
        String className = defaultname == null ? getString(name) : getString(name, defaultname);
        SingletonKey key = new SingletonKey(name, className);
        @SuppressWarnings("unchecked")
        T inst = (T) instanceHolder.get(key);
        if (inst != null) {
            return inst;
        }
        inst = this.<T> getInstanceOf(name, defaultname); // we need a new instance, get it
        instanceHolder.put(key, inst); // save the instance in the hashtable
        return inst;
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
        return getSingleInstanceOf(name, (String) null);
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
        String value = getString(name, null);

        if (value == null) {
            throw new MCRConfigurationException("Configuration property " + name + " is not set");
        }

        return value.trim();
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
        String value = getString(name);
        return splitString(value);
    }

    private List<String> splitString(String value) {
        return PROPERTY_SPLITTER.splitAsStream(value)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
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
        String value = getString(name, null);
        return value == null ? defaultValue : splitString(value);
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
        if (getBaseProperties().isEmpty()) {
            throw new MCRConfigurationException("MCRConfiguration is still not initialized");
        }
        checkForDeprecatedProperty(name);
        return getResolvedProperties().getProperty(name, defaultValue);
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
        return Integer.parseInt(getString(name));
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
        String value = getString(name, null);

        return value == null ? defaultValue : Integer.parseInt(value);
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
        return Long.parseLong(getString(name));
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
        String value = getString(name, null);

        return value == null ? defaultValue : Long.parseLong(value);
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
        return Float.parseFloat(getString(name));
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
        String value = getString(name, null);

        return value == null ? defaultValue : Float.parseFloat(value);
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
        return Double.parseDouble(getString(name));
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
        String value = getString(name, null);

        return value == null ? defaultValue : Double.parseDouble(value);
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
        String value = getString(name);

        return "true".equals(value.trim());
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
        String value = getString(name, null);

        return value == null ? defaultValue : "true".equals(value.trim());
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
        checkForDeprecatedProperty(name);
        if (value == null) {
            getBaseProperties().remove(name);
        } else {
            getBaseProperties().setProperty(name, value);
        }
        resolveProperties();
    }

    public synchronized void initialize(Map<String, String> props, boolean clear) {
        checkForDeprecatedProperties(props);
        HashMap<String, String> copy = new HashMap<>(props);
        copy.remove(null);
        if (clear) {
            getBaseProperties().clear();
        } else {
            Map<String, String> nullValues = Maps.filterValues(copy, Predicates.<String> isNull());
            for (String key : nullValues.keySet()) {
                getBaseProperties().remove(key);
            }
        }
        Map<String, String> notNullValues = Maps.filterValues(copy, Predicates.notNull());
        for (Entry<String, String> entry : notNullValues.entrySet()) {
            getBaseProperties().setProperty(entry.getKey(), entry.getValue());
        }
        resolveProperties();
        debug();
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

    private static class SingletonKey {
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
