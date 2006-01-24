/*
 * $RCSfile$
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

package org.mycore.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Provides methods to manage and read all configuration properties from the
 * MyCoRe configuration files. The class is implemented using the singleton
 * pattern. Using this class is very easy, here is an example:
 * 
 * <PRE>// Get a configuration property as a String: String driver =
 * MCRConfiguration.instance().getString( "MCR.jdbc.driver" ); // Get a
 * configuration property as an int, use 500 as default if not set: int max =
 * MCRConfiguration.instance().getInt( "MCR.cache.size", 500 );
 * 
 * </PRE>
 * 
 * As you see, the class provides methods to get configuration properties as
 * different data types and allows you to specify defaults. All MyCoRe
 * configuration properties should start with "<CODE>MCR.</CODE>" When
 * <CODE>instance()</CODE> is called first, the file <B><CODE>
 * mycore.properties</CODE> </B> is read. It can be located somewhere in the
 * <CODE>CLASSPATH</CODE>, even in a jar or zip file. The properties file may
 * have a property called <B><CODE>MCR.configuration.include</CODE> </B> that
 * contains a comma-separated list of other configuration files to read
 * subsequently. The class also reads any Java <B>system properties </B> that
 * start with "<CODE>MCR.</CODE>" and that are set when the application
 * starts. System properties will override properties read from the
 * configuration files. Furthermore, the name of the main configuration file can
 * be altered by specifying the system property <B><CODE>
 * MCR.configuration.file</CODE> </B>. Here is an example:
 * 
 * <PRE>
 * 
 * java -DMCR.configuration.file=some_other.properties -DMCR.foo=bar
 * MyCoReSample
 * 
 * </PRE>
 * 
 * The class also provides methods for <B>listing or saving </B> all properties
 * to an <CODE>OutputStream</CODE> and for <B>reloading </B> all configuration
 * properties at runtime. This allows servlets that run a long time to re-read
 * the configuration files when client code tells them to do so, for example.
 * Using the <CODE>set</CODE> methods allows client code to set new
 * configuration properties or overwrite existing ones with new values. Finally,
 * applications could also <B>subclass <CODE>MCRConfiguration</CODE> </B> to
 * change or add behavior and use an instance of the subclass instead of this
 * class. This is transparent for client code, they would still use <CODE>
 * MCRConfiguration.instance()</CODE> to get the subclass instance. To use a
 * subclass instead of <CODE>MCRConfiguration</CODE> itself, specify the
 * system property <CODE>MCR.configuration.class</CODE>, e. g.
 * 
 * <PRE>
 * 
 * java -DMCR.configuration.class=MCRConfigurationSubclass MyCoReSample
 * 
 * </PRE>
 * 
 * @see #loadFromFile
 * @see #reload
 * @see #list
 * @see #store
 * 
 * @author Frank L?tzenkirchen
 * @version $Revision$ $Date$
 */
public class MCRConfiguration {
    /**
     * The single instance of this class that will be used at runtime
     */
    protected static MCRConfiguration singleton;

    private static Hashtable instanceHolder;

    /**
     * Returns the single instance of this class that can be used to read and
     * manage the configuration properties.
     * 
     * @return the single instance of <CODE>MCRConfiguration</CODE> to be used
     */
    public static synchronized MCRConfiguration instance() {
        if (singleton == null) {
            createSingleton();
        }

        return singleton;
    }

    /**
     * Instantiates the singleton by calling the protected constructor. If the
     * system property <CODE>MCR.configuration.class</CODE> is set when the
     * system starts, the class specified in that property will be instantiated
     * instead. This allows for subclassing <CODE>MCRConfiguration</CODE> to
     * change behaviour and use the subclass instead of <CODE>MCRConfiguration
     * </CODE>.
     */
    protected static void createSingleton() {
        String name = System.getProperty("MCR.configuration.class");

        if (name != null) {
            try {
                singleton = (MCRConfiguration) (Class.forName(name).newInstance());
            } catch (Exception exc) {
                throw new MCRConfigurationException("Could not create MCR.configuration.class singleton \"" + name + "\"", exc);
            }
        } else {
            singleton = new MCRConfiguration();
        }
    }

    /**
     * The properties instance that stores the values that have been read from
     * every configuration file
     */
    protected Properties properties;

    /**
     * Protected constructor to create the singleton instance
     */
    protected MCRConfiguration() {
        properties = new Properties();
        reload(false);

        Enumeration names = System.getProperties().propertyNames();

        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());

            if (name.startsWith("MCR.")) {
                String value = System.getProperty(name);

                if (value != null) {
                    set(name, value);
                }
            }
        }

        configureLogging();
    }

    /**
     * Reloads all properties from the configuration files. If the system
     * property <CODE>MCR.configuration.file</CODE> is set, the file specified
     * in this property will be used as main configuration file, otherwise the
     * default file <CODE>mycore.properties</CODE> will be read. If the
     * parameter <CODE>clear</CODE> is <CODE>true</CODE>, all properties
     * currently set will be deleted first, otherwise the properties read from
     * the configuration files will be added to the properties currently set and
     * they will overwrite existing properties with the same name.
     * 
     * @param clear
     *            if true, properties currently set will be deleted first
     * @throws MCRConfigurationException
     *             if the config files can not be loaded
     */
    public void reload(boolean clear) {
        if (clear) {
            properties.clear();
        }

        String fn = System.getProperty("MCR.configuration.file", "mycore.properties");
        loadFromFile(fn);

        if (clear) {
            configureLogging();
        }
    }

    /**
     * Loads configuration properties from a specified properties file and adds
     * them to the properties currently set. This method scans the <CODE>
     * CLASSPATH</CODE> for the properties file, it may be a plain file, but
     * may also be located in a zip or jar file. If the properties file contains
     * a property called <CODE>MCR.configuration.include</CODE>, the files
     * specified in that property will also be read. Multiple include files have
     * to be separated by spaces or colons.
     * 
     * @param filename
     *            the properties file to be loaded
     * @throws MCRConfigurationException
     *             if the file can not be loaded
     */
    public void loadFromFile(String filename) {
        MCRArgumentChecker.ensureNotEmpty(filename, "filename");
        File mycoreProperties=new File(filename);
        InputStream in;
        if (mycoreProperties.canRead()){
            try {
                in = new FileInputStream(mycoreProperties);
            } catch (FileNotFoundException e) {
                //should never happend, because we verified it allready with canRead() above
                String msg = "Could not find configuration file " + filename;
                throw new MCRConfigurationException(msg,e);
            }
        } else {
            in = this.getClass().getResourceAsStream("/" + filename);
        }
        if (in == null) {
            String msg = "Could not find configuration file " + filename + " in CLASSPATH";
            throw new MCRConfigurationException(msg);
        }

        try {
            properties.load(in);
            in.close();
        } catch (Exception exc) {
            throw new MCRConfigurationException("Exception while loading configuration file " + filename, exc);
        }

        String include = getString("MCR.configuration.include", null);

        if (include != null) {
            StringTokenizer st = new StringTokenizer(include, ", ");
            set("MCR.configuration.include", null);

            while (st.hasMoreTokens())
                loadFromFile(st.nextToken());
        }
    }

    /**
     * Returns all the properties
     * 
     * @return the list of properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns all the properties beginning with the specified string
     * 
     * @param startsWith
     *            the string all the returned properties start with
     * @return the list of properties
     */
    public Properties getProperties(String startsWith) {
        Properties properties = new Properties();

        Enumeration names = this.properties.propertyNames();

        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());

            if (name.startsWith(startsWith)) {
                String value = this.properties.getProperty(name);
                properties.setProperty(name, value);
            }
        }

        return properties;
    }

    /**
     * Configures Log4J based on the MCR.log4j properties
     */
    public synchronized void configureLogging() {
        Properties prop = new Properties();
        Enumeration names = this.properties.propertyNames();
        boolean reconfigure = false;

        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());

            if (name.startsWith("MCR.log4j")) {
                String value = this.properties.getProperty(name);
                prop.setProperty(name.substring(4), value);
                reconfigure = true;
            }
        }

        if (reconfigure) {
            System.out.println("MCRConfiguration reconfiguring Log4J logging...");
            org.apache.log4j.LogManager.resetConfiguration();
            PropertyConfigurator.configure(prop);
        }
    }

    /**
     * Returns a new instance of the class specified in the configuration
     * property with the given name.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String, or null
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    private Object getInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        MCRArgumentChecker.ensureNotEmpty(name, "name");

        String classname = properties.getProperty(name);

        if (classname == null) {
            if (defaultname != null) {
                classname = defaultname;
            }

            if (classname == null) {
                throw new MCRConfigurationException("Configuration property " + name + " is not set!");
            }
        }

        Class cl;

        Logger.getLogger(this.getClass()).debug("Loading Class: " + classname);

        try {
            cl = Class.forName(classname);
        } catch (Exception ex) {
            throw new MCRConfigurationException("Could not load class " + classname, ex);
        }

        Object o = null;

        try {
            try {
                o = cl.newInstance();
            } catch (Exception e) {
                // check for singleton
                Method[] querymethods = cl.getMethods();

                for (int i = 0; i < querymethods.length; i++) {
                    if (querymethods[i].getName().toLowerCase().equals("instance") || querymethods[i].getName().toLowerCase().equals("getinstance")) {
                        Object[] ob = new Object[0];
                        o = querymethods[i].invoke(cl, ob);

                        break;
                    }
                }
            }
        } catch (Throwable t) {
            String msg = "Could not instantiate class " + classname;

            if (t instanceof ExceptionInInitializerError) {
                Throwable t2 = ((ExceptionInInitializerError) t).getException();

                if (t2 instanceof Exception) {
                    throw new MCRConfigurationException(msg, (Exception) t2);
                }

                throw new MCRConfigurationException(msg + ": " + t2.getClass().getName() + " - " + t2.getMessage());
            } else if (t instanceof Exception) {
                t.printStackTrace();
                throw new MCRConfigurationException(msg, (Exception) t);
            } else {
                msg += (" because of: " + t.getMessage());
                msg += ("\n" + MCRException.getStackTraceAsString(t));
                throw new MCRConfigurationException(msg);
            }
        }

        return o;
    }

    /**
     * Returns a new instance of the class specified in the configuration
     * property with the given name.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String, or null
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public Object getInstanceOf(String name) throws MCRConfigurationException {
        return getInstanceOf(name, null);
    }

    /**
     * Returns a instance of the class specified in the configuration property
     * with the given name. If the class was prevously instantiated by this
     * method this instance is returned.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration
     *         propertyl
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public Object getSingleInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        if (instanceHolder == null) {
            instanceHolder = new Hashtable(); // initialize the hashtable if
        }
        // it's not yet
        else if (instanceHolder.containsKey(name)) {
            return instanceHolder.get(name); // we have an instance allready,
        }

        // return it
        Object inst = getInstanceOf(name, defaultname); // we need a new
                                                        // instance, get it
        instanceHolder.put(name, inst); // save the instance in the hashtable

        return inst;
    }

    /**
     * Returns a instance of the class specified in the configuration property
     * with the given name. If the class was prevously instantiated by this
     * method this instance is returned.
     * 
     * @param name
     *            non-null and non-empty name of the configuration property
     * @param default
     *            the default if the configuration for name isn't found
     * @return the instance of the class named by the value of the configuration
     *         propertyl
     * @throws MCRConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public Object getSingleInstanceOf(String name) {
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
        MCRArgumentChecker.ensureNotEmpty(name, "name");

        String value = properties.getProperty(name);

        if (value == null) {
            throw new MCRConfigurationException("Configuration property " + name + " is not set");
        }

        return value;
    }

    /**
     * Returns the configuration property with the specified name as a String,
     * or returns a given default value if the property is not set.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the configuration property as a String
     */
    public String getString(String name, String defaultValue) {
        MCRArgumentChecker.ensureNotEmpty(name, "name");

        String value = properties.getProperty(name);

        return ((value == null) ? defaultValue : value);
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as an <CODE>int</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not an <CODE>int</CODE>
     *             value
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public int getInt(String name) throws NumberFormatException {
        return Integer.parseInt(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value, or returns a given default value if the property is
     * not set.
     * 
     * @return the value of the specified property as an <CODE>int</CODE>
     *         value
     * @param name
     *            the non-null and non-empty name of the configuration property
     *            /** Returns the configuration property with the specified name
     *            as an <CODE>int</CODE> value, or returns a given default
     *            value if the property is not set.
     * 
     * @return the value of the specified property as an <CODE>int</CODE>
     *         value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @throws NumberFormatException
     *             if the configuration property is set but is not an <CODE>int
     *             </CODE> value
     */
    public int getInt(String name, int defaultValue) throws NumberFormatException {
        String value = getString(name, null);

        return ((value == null) ? defaultValue : Integer.parseInt(value));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>long</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>long</CODE>
     *             value
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public long getLong(String name) throws NumberFormatException {
        return Long.parseLong(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value, or returns a given default value if the property is
     * not set.
     * 
     * @return the value of the specified property as a <CODE>long</CODE>
     *         value
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

        return ((value == null) ? defaultValue : Long.parseLong(value));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>float</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>float</CODE>
     *             value
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public float getFloat(String name) throws NumberFormatException {
        return Float.parseFloat(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value, or returns a given default value if the property is
     * not set.
     * 
     * @return the value of the specified property as a <CODE>float</CODE>
     *         value
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

        return ((value == null) ? defaultValue : Float.parseFloat(value));
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
     *             if the configuration property is not a <CODE>double</CODE>
     *             value
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public double getDouble(String name) throws NumberFormatException {
        return Double.parseDouble(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * double</CODE> value, or returns a given default value if the property is
     * not set.
     * 
     * @return the value of the specified property as a <CODE>double</CODE>
     *         value
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

        return ((value == null) ? defaultValue : Double.parseDouble(value));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return <CODE>true</CODE>, if and only if the specified property has
     *         the value <CODE>true</CODE>
     * @throws MCRConfigurationException
     *             if the property with this name is not set
     */
    public boolean getBoolean(String name) {
        String value = getString(name);

        return "true".equals(value.trim());
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value, or returns a given default value if the property
     * is not set. If the property is set and its value is not <CODE>true
     * </CODE>, then <code>false</code> is returned.
     * 
     * @return the value of the specified property as a <CODE>boolean</CODE>
     *         value
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = getString(name, null);

        return ((value == null) ? defaultValue : "true".equals(value.trim()));
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
        MCRArgumentChecker.ensureNotEmpty(name, "name");

        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value);
        }
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
     * Lists all configuration properties currently set to a PrintStream. Useful
     * for debugging, e. g. by calling
     * 
     * <P>
     * <CODE>MCRConfiguration.instance().list( System.out );</CODE>
     * </P>
     * 
     * @see java.util.Properties#list( PrintStream )
     * 
     * @param out
     *            the PrintStream to list the configuration properties on
     */
    public void list(PrintStream out) {
        MCRArgumentChecker.ensureNotNull(out, "out");
        properties.list(out);
    }

    /**
     * Lists all configuration properties currently set to a PrintWriter. Useful
     * for debugging.
     * 
     * @see java.util.Properties#list( PrintWriter )
     * 
     * @param out
     *            the PrintWriter to list the configuration properties on
     */
    public void list(PrintWriter out) {
        MCRArgumentChecker.ensureNotNull(out, "out");
        properties.list(out);
    }

    /**
     * Stores all configuration properties currently set to an OutputStream.
     * 
     * @see java.util.Properties#store
     * 
     * @param out
     *            the OutputStream to write the configuration properties to
     * @param header
     *            the header to prepend before writing the list of properties
     * @throws IOException
     *             if writing to the OutputStream throws an <CODE>IOException
     *             </CODE>
     */
    public void store(OutputStream out, String header) throws IOException {
        MCRArgumentChecker.ensureNotNull(out, "out");
        properties.store(out, header);
    }

    /**
     * Returns a String containing the configuration properties currently set.
     * Useful for debugging, e. g. by calling
     * 
     * <P>
     * <CODE>System.out.println( MCRConfiguration.instance() );</CODE>
     * </P>
     * 
     * @return a String containing the configuration properties currently set
     */
    public String toString() {
        return properties.toString();
    }
}
