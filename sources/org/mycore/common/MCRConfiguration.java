/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  *** 
 * see http://mycore.uni-essen.de/ for details.
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
 * along with this program, in a file called license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.common;

import java.io.*;
import java.util.*;

/**
 * Provides methods to manage and read all configuration properties from the
 * MyCoRe configuration files. The class is implemented using the 
 * singleton pattern. Using this class is very easy, here is an example:
 * <PRE>
 * // Get a configuration property as a String:
 * String driver = MCRConfiguration.instance().getString( "mcr.jdbc.driver" );
 *
 * // Get a configuration property as an int, use 500 as default if not set:
 * int max = MCRConfiguration.instance().getInt( "mcr.cache.size", 500 );
 * </PRE>
 * As you see, the class provides methods to get configuration properties
 * as different data types and allows you to specify defaults. All MyCoRe
 * configuration properties should start with "<CODE>mcr.</CODE>" When
 * <CODE>instance()</CODE> is called first, the file 
 * <B><CODE>mycore.properties</CODE></B> is read. It can be located somewhere 
 * in the <CODE>CLASSPATH</CODE>, even in a jar or zip file. The properties 
 * file may have a property called <B><CODE>mcr.configuration.include</CODE></B> 
 * that contains a comma-separated list of other configuration files to read
 * subsequently. The class also reads any Java <B>system properties</B> that 
 * start with "<CODE>mcr.</CODE>" and that are set when the application starts. 
 * System properties will override properties read from the configuration files. 
 * Furthermore, the name of the main configuration file can be altered by 
 * specifying the system property <B><CODE>mcr.configuration.file</CODE></B>. 
 * Here is an example:
 * <PRE>
 * java -Dmcr.configuration.file=some_other.properties -Dmcr.foo=bar MyCoReSample
 * </PRE>
 * The class also provides methods for <B>listing or saving</B> all properties
 * to an <CODE>OutputStream</CODE> and for <B>reloading</B> all configuration 
 * properties at runtime. This allows servlets that run a long time to re-read 
 * the configuration files when client code tells them to do so, for example. 
 * Using the <CODE>set</CODE> methods allows client code to set new 
 * configuration properties or overwrite existing ones with new values. 
 * Finally, applications could also <B>subclass <CODE>MCRConfiguration</CODE></B>
 * to change or add behavior and use an instance of the subclass instead 
 * of this class. This is transparent for client code, they would still use 
 * <CODE>MCRConfiguration.instance()</CODE> to get the subclass instance. 
 * To use a subclass instead of <CODE>MCRConfiguration</CODE> itself, specify 
 * the system property <CODE>mcr.configuration.class</CODE>, e. g.
 * <PRE>
 * java -Dmcr.configuration.class=MCRConfigurationSubclass MyCoReSample
 * </PRE>
 *
 * @see #loadFromFile
 * @see #reload
 * @see #list
 * @see #store
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRConfiguration
{
/**
 * The single instance of this class that will be used at runtime
 */    
  protected static MCRConfiguration singleton;

/**
 * Returns the single instance of this class that can be used to
 * read and manage the configuration properties.
 *
 * @return the single instance of <CODE>MCRConfiguration</CODE> to be used
 */  
  public static synchronized MCRConfiguration instance()
  { 
    if( singleton == null ) createSingleton();     
    return singleton;
  }
    
/**
 * Instantiates the singleton by calling the protected constructor. If the
 * system property <CODE>mcr.configuration.class</CODE> is set when the
 * system starts, the class specified in that property will be instantiated
 * instead. This allows for subclassing <CODE>MCRConfiguration</CODE> to
 * change behaviour and use the subclass instead of
 * <CODE>MCRConfiguration</CODE>.
 */  
  protected static void createSingleton()
  {
    String name = System.getProperty( "mcr.configuration.class" );
    if( name != null )
    {
      try
      {
        singleton = (MCRConfiguration)( Class.forName( name ).newInstance() );
      }
      catch( Exception exc )
      {
        throw new RuntimeException
        ( "Could not create mcr.configuration.class singleton \"" + name + "\"" );
      }
    }
    else singleton = new MCRConfiguration();
  }
  
/**
 * The properties instance that stores the values
 * that have been read from every configuration file
 */  
  protected Properties properties;

/**
 * Protected constructor to create the singleton instance
 */  
  protected MCRConfiguration()
  { 
    properties = new Properties();
    reload( false );
    
    Enumeration names = System.getProperties().propertyNames();      
    while( names.hasMoreElements() )
    {
      String name = (String)( names.nextElement() );
      if( name.startsWith( "mcr." ) )
      {
        String value = System.getProperty( name );
        if( value != null ) set( name, value );
      }
    }
  }
    
/**
 * Reloads all properties from the configuration files.
 * If the system property <CODE>mcr.configuration.file</CODE> is set,
 * the file specified in this property will be used as main configuration
 * file, otherwise the default file <CODE>mycore.properties</CODE> will
 * be read. If the parameter <CODE>clear</CODE> is <CODE>true</CODE>,
 * all properties currently set will be deleted first, otherwise the
 * properties read from the configuration files will be added to the
 * properties currently set and they will overwrite existing properties
 * with the same name.
 *
 * @param clear if true, properties currently set will be deleted first
 */  
  public void reload( boolean clear )
  { 
    if( clear ) properties.clear();
    String fn = System.getProperty( "mcr.configuration.file", "mycore.properties" );
    loadFromFile( fn ); 
  }

/**
 * Loads configuration properties from a specified properties file and adds
 * them to the properties currently set. This method scans the
 * <CODE>CLASSPATH</CODE> for the properties file, it may be a plain file,
 * but may also be located in a zip or jar file. If the properties file
 * contains a property called <CODE>mcr.configuration.include</CODE>, the
 * files specified in that property will also be read. Multiple include
 * files have to be separated by spaces or colons.
 *
 * @param filename the properties file to be loaded
 */  
  public void loadFromFile( String filename )
  {
    if( ( filename == null ) || ( filename.trim().length() == 0 ) )
      throw new IllegalArgumentException
      ( "You specified an empty configuration file name" );

    try
    {
      InputStream in = this.getClass().getResourceAsStream( "/" + filename );
      properties.load( in );
      in.close();
    }
    catch( Exception exc )
    { 
      throw new RuntimeException
      ( "Could not load configuration file " + filename );
    }

    String include = getString( "mcr.configuration.include" );
    if( include != null )
    {
      StringTokenizer st = new StringTokenizer( include, ", " );
      set( "mcr.configuration.include", null );
      while( st.hasMoreTokens() ) loadFromFile( st.nextToken() );
    }
  }
  
/**
 * Returns the configuration property with the specified name as a String,
 * or null if the property is not set.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return the value of the configuration property as a String, or null
 */  
  public String getString( String name )
  {   
    if( ( name == null ) || ( name.trim().length() == 0 ) )
      throw new IllegalArgumentException
      ( "You specified an empty configuration property name" );
    return properties.getProperty( name );
  }

/**
 * Returns the configuration property with the specified name as a String,
 * or returns a given default value if the property is not set.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 * @return the value of the configuration property as a String
 */  
  public String getString( String name, String defaultValue )
  {
    String value = getString( name );
    return ( value == null ? defaultValue : value );
  }

/**
 * Returns the configuration property with the specified name as an <CODE>int</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return the value of the configuration property as an <CODE>int</CODE> value
 * @throws NumberFormatException if the configuration property is not set or is not an <CODE>int</CODE> value
 */  
  public int getInt( String name )
    throws NumberFormatException
  { return Integer.parseInt( getString( name ) ); }

/**
 * Returns the configuration property with the specified name as an
 * <CODE>int</CODE> value, or returns a given default value if the property
 * is not set.
 *
 * @return the value of the specified property as an <CODE>int</CODE> value
 * @param name the non-null and non-empty name of the configuration property
/**
 * Returns the configuration property with the specified name as an
 * <CODE>int</CODE> value, or returns a given default value if the property
 * is not set.
 *
 * @return the value of the specified property as an <CODE>int</CODE> value
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 * @throws NumberFormatException if the configuration property is set but is not an <CODE>int</CODE> value
 */  
  public int getInt( String name, int defaultValue )
    throws NumberFormatException
  {
    String value = getString( name );
    return ( value == null ? defaultValue : Integer.parseInt( value ) );
  }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>long</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return the value of the configuration property as a <CODE>long</CODE> value
 * @throws NumberFormatException if the configuration property is not set or is not a <CODE>long</CODE> value
 */  
  public long getLong( String name )
    throws NumberFormatException
  { return Long.parseLong( getString( name ) ); }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>long</CODE> value, or returns a given default value if the property
 * is not set.
 *
 * @return the value of the specified property as a <CODE>long</CODE> value
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 * @throws NumberFormatException if the configuration property is set but is not a <CODE>long</CODE> value
 */  
  public long getLong( String name, long defaultValue )
    throws NumberFormatException
  {
    String value = getString( name );
    return ( value == null ? defaultValue : Long.parseLong( value ) );
  }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>float</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return the value of the configuration property as a <CODE>float</CODE> value
 * @throws NumberFormatException if the configuration property is not set or is not a <CODE>float</CODE> value
 */  
  public float getFloat( String name )
    throws NumberFormatException
  { return Float.parseFloat( getString( name ) ); }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>float</CODE> value, or returns a given default value if the property
 * is not set.
 *
 * @return the value of the specified property as a <CODE>float</CODE> value
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 * @throws NumberFormatException if the configuration property is set but is not a <CODE>float</CODE> value
 */  
  public float getFloat( String name, float defaultValue )
    throws NumberFormatException
  {
    String value = getString( name );
    return ( value == null ? defaultValue : Float.parseFloat( value ) );
  }
  
/**
 * Returns the configuration property with the specified name as a
 * <CODE>double</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return the value of the configuration property as a <CODE>double</CODE> value
 * @throws NumberFormatException if the configuration property is not set or is not a <CODE>double</CODE> value
 */  
  public double getDouble( String name )
    throws NumberFormatException
  { return Double.parseDouble( getString( name ) ); }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>double</CODE> value, or returns a given default value if the property
 * is not set.
 *
 * @return the value of the specified property as a <CODE>double</CODE> value
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 * @throws NumberFormatException if the configuration property is set but is not a <CODE>double</CODE> value
 */  
  public double getDouble( String name, double defaultValue )
    throws NumberFormatException
  {
    String value = getString( name );
    return ( value == null ? defaultValue : Double.parseDouble( value ) );
  }

/**
 * Returns the configuration property with the specified name as a 
 * <CODE>boolean</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @return <CODE>true</CODE>, if and only if the specified property has the value <CODE>true</CODE>
 */  
  public boolean getBoolean( String name )
  { return getBoolean( name, false ); }

/**
 * Returns the configuration property with the specified name as a
 * <CODE>boolean</CODE> value, or returns a given default value if the property
 * is not set. If the property is set and its value is not <CODE>true</CODE>,
 * then <code>false</code> is returned.
 *
 * @return the value of the specified property as a <CODE>boolean</CODE> value
 * @param name the non-null and non-empty name of the configuration property
 * @param defaultValue the value to return if the configuration property is not set
 */  
  public boolean getBoolean( String name, boolean defaultValue )
  {
    String value = getString( name );
    return ( value == null ? defaultValue : "true".equals( value.trim() ) );
  }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>String</CODE> value. If the parameter <CODE>value</CODE> is 
 * <CODE>null</CODE>, the property will be deleted.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property, possibly <CODE>null</CODE>
 */  
  public void set( String name, String value )
  {
    if( ( name == null ) || ( name.trim().length() == 0 ) )
      throw new IllegalArgumentException
      ( "You specified an empty configuration property name" );
    
    if( value == null )
      properties.remove( name );
    else
      properties.setProperty( name, value );
  }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>int</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property
 */  
  public void set( String name, int value )
  { set( name, String.valueOf( value ) ); }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>long</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property
 */  
  public void set( String name, long value )
  { set( name, String.valueOf( value ) ); }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>float</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property
 */  
  public void set( String name, float value )
  { set( name, String.valueOf( value ) ); }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>double</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property
 */  
  public void set( String name, double value )
  { set( name, String.valueOf( value ) ); }
  
/**
 * Sets the configuration property with the specified name to a new 
 * <CODE>boolean</CODE> value.
 *
 * @param name the non-null and non-empty name of the configuration property
 * @param value the new value of the configuration property
 */  
  public void set( String name, boolean value )
  { set( name, String.valueOf( value ) ); }
  
/**
 * Lists all configuration properties currently set to a PrintStream.
 * Useful for debugging, e. g. by calling
 *
 * <P><CODE>MCRConfiguration.instance().list( System.out );</CODE></P>
 *
 * @see java.util.Properties#list( PrintStream )
 *
 * @param out the PrintStream to list the configuration properties on
 */  
  public void list( PrintStream out )
  { properties.list( out ); }
  
/**
 * Lists all configuration properties currently set to a PrintWriter.
 * Useful for debugging.
 *
 * @see java.util.Properties#list( PrintWriter )
 *
 * @param out the PrintWriter to list the configuration properties on
 */  
  public void list( PrintWriter out )
  { properties.list( out ); }

/**
 * Stores all configuration properties currently set to an OutputStream.
 *
 * @see java.util.Properties#store
 *
 * @param out the OutputStream to write the configuration properties to
 * @param header the header to prepend before writing the list of properties
 * @throws IOException if writing to the OutputStream throws an <CODE>IOException</CODE>
 */  
  public void store( OutputStream out, String header )
    throws IOException
  { properties.store( out, header ); }
  
/**
 * Returns a String containing the configuration properties currently set.
 * Useful for debugging, e. g. by calling
 *
 * <P><CODE>System.out.println( MCRConfiguration.instance() );</CODE></P>
 *
 * @return a String containing the configuration properties currently set
 */  
  public String toString()
  { return properties.toString(); }
} 
