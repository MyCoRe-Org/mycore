/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.plugins;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFileContentType;

/**
 * Loads and manages plugins
 * 
 * @author Thomas Scheffler (yagee)
 */
public class TextFilterPluginManager {

	/** The logger */
	private static final Logger logger =
		Logger.getLogger(TextFilterPluginManager.class);
	/** The configuration */
	private static final MCRConfiguration conf = MCRConfiguration.instance();
	/** Pluginbasket */
	private static Hashtable ContentTypePluginBag = null;
	private static Hashtable Plugins = null;
	/** initialized */
	private static TextFilterPluginManager instance;

	/**
	 * 
	 */
	private TextFilterPluginManager() {
		init();
	}
	public static TextFilterPluginManager getInstance() {
		if (instance == null) {
			instance = new TextFilterPluginManager();
		}
		return instance;
	}
	private void init() {
		ContentTypePluginBag = new Hashtable();
		Plugins = new Hashtable();
		loadPlugins();
	}
	/**
	 * load TextFilterPlugins from the MCR.PluginDirectory
	 *
	 */
	public void loadPlugins() {
		URLClassLoader classLoader;
		try {
			classLoader =
				new URLClassLoader(
					getPluginURLs(),
					Thread.currentThread().getContextClassLoader());
		} catch (MalformedURLException e) {
			// should "never" happen
			throw new MCRException("Failure getting URLs from plugins!", e);
		}
		TextFilterPlugin filter = null;
		MCRFileContentType ct;
		for (Iterator iter = getProviders(TextFilterPlugin.class, classLoader);
			iter.hasNext();
			) {
			filter = (TextFilterPlugin) iter.next();
			//logger.debug("Loading TextFilterPlugin: "+filter.getName());
			System.err.println("Loading TextFilterPlugin: " + filter.getName());
			for (Iterator CtIterator =
				filter.getSupportedContentTypes().iterator();
				CtIterator.hasNext();
				) {
				//Add MIME Type filters to the basket
				ct = (MCRFileContentType) CtIterator.next();
				if (ct != null)
					ContentTypePluginBag.put(ct, filter);
			}
			Plugins.put(filter.getClass().getName(), filter);
		}
	}
	/**
	 * removes all plugins from the manager
	 *
	 */
	public void clear() {
		init();
	}
	/**
	 * removes all plugins and reload plugins after that
	 * 
	 * This is when you delete a plugin while the application is running,
	 * replacing one with a new version or if you just add one.
	 */
	public void reloadPlugins() {
		clear();
		loadPlugins();
	}
	/**
	 * returns a Collection of all loaded plugins.
	 * 
	 * @return a Collection of Plugins
	 */
	public Collection getPlugins() {
		return Plugins.values();
	}
	/**
	 * returns TextFilterPlugin to corresponding MIME type
	 * @param supported MIME type
	 * @return corresponding TextFilterPlugin or null if MIME is emtpy or null
	 */
	public TextFilterPlugin getPlugin(MCRFileContentType ct) {
		return (ct == null)
			? null
			: (TextFilterPlugin) ContentTypePluginBag.get(ct);
	}
	/**
	 * returns true if MIME type is supported
	 * @param MIME of Inputstream
	 * @return true if MIME type is supported, else false
	 */
	public boolean isSupported(MCRFileContentType ct) {
		return (ct == null) ? false : ContentTypePluginBag.containsKey(ct);
	}

	public boolean transform(
		MCRFileContentType ct,
		InputStream input,
		OutputStream output)
		throws FilterPluginTransformException {
		if (isSupported(ct)) {
			return getPlugin(ct).transform(ct, input, output);
		} else
			return false;
	}

	/**
	 * returns the URLs of all plugins found in MCR.PluginDirectory
	 * @return	Array of URL of plugin-JARs
	 * @throws MalformedURLException
	 */
	private final URL[] getPluginURLs() throws MalformedURLException {
		HashSet returnS = new HashSet();
		File pluginDir = new File(conf.getString("MCR.PluginDirectory"));
		if (pluginDir == null || !pluginDir.isDirectory())
			return null;
		File[] plugins = pluginDir.listFiles();
		for (int i = 0; i < plugins.length; i++) {
			System.err.println(plugins[i].getName());
			if (plugins[i].isFile()
				&& plugins[i].getName().toUpperCase().endsWith(".JAR"))
				//This Jar file possibly contains a text filter plugin
				returnS.add(plugins[i].toURL());
		}
		URL[] returnU = new URL[returnS.size()];
		int i = 0;
		Iterator it = returnS.iterator();
		while (it.hasNext()) {
			returnU[i] = (URL) it.next();
			i++;
		}
		return returnU;
	}
	/**
	 * replacement for sun.misc.Service.provider(Class,ClassLoader) which is only available on sun jdk
	 * 
	 * @param service	Interface of instance needs to implement
	 * @param loader	URLClassLoader of Plugin
	 * @return Iterator over instances of service
	 */
	protected static final Iterator getProviders(
		Class service,
		URLClassLoader loader) {
		//we use a hashtable for this to keep controll of duplicates
		Hashtable classMap = new Hashtable();
		String name = "META-INF/services/" + service.getName();
		Enumeration services;
		try {
			services =
				(loader == null)
					? ClassLoader.getSystemResources(name)
					: loader.getResources(name);
		} catch (IOException ioe) {
			System.err.println("Service: cannot load " + name);
			return classMap.values().iterator();
		}
		//Put all class names matching Service in nameSet
		while (services.hasMoreElements()) {
			URL url = (URL) services.nextElement();
			System.out.println(url);
			InputStream input = null;
			BufferedReader reader = null;
			try {
				input = url.openStream();
				reader =
					new BufferedReader(new InputStreamReader(input, "utf-8"));
				Object classInstance = null;
				for (StringBuffer className =
					new StringBuffer().append(reader.readLine());
					(className.length() != 4
						&& className.toString().indexOf("null") == -1);
					className.delete(0, className.length()).append(
						reader.readLine())) {
					//System.out.println("processing String: "+className.toString());					               	
					//remove any comments
					int comPos = className.toString().indexOf("#");
					if (comPos != -1)
						className.delete(comPos, className.length());
					//trim String
					int st = 0;
					int sblen = className.length();
					int len = sblen - 1;
					while ((st < sblen) && className.charAt(st) <= ' ')
						st++;
					while ((st < len) && className.charAt(len) <= ' ')
						len--;
					className.delete(len + 1, sblen).delete(0, st);
					//end trim String	 
					//if space letter is included asume first word as class name
					int spacePos = className.toString().indexOf(" ");
					if (spacePos != -1)
						className =
							className.delete(spacePos, className.length());
					//trim String
					st = 0;
					sblen = className.length();
					len = sblen - 1;
					while ((st < sblen) && className.charAt(st) <= ' ')
						st++;
					while ((st < len) && className.charAt(len) <= ' ')
						len--;
					className.delete(len + 1, sblen).delete(0, st);
					//end trim String	 
					if (className.length() > 0) {
						//we should have a proper class name now
						try {
							classInstance =
								Class
									.forName(className.toString(), true, loader)
									.newInstance();
							if (service.isInstance(classInstance))
								classMap.put(
									className.toString(),
									classInstance);
							else {
								classInstance = null;
								logger.error(
									className.toString()
										+ " does not implement "
										+ service.getName()
										+ "! Class instance will not be used.");
							}
						} catch (ClassNotFoundException e) {
							System.err.println(
								"Service: cannot find class: " + className);
						} catch (InstantiationException e) {
							System.err.println(
								"Service: cannot instantiate: " + className);
						} catch (IllegalAccessException e) {
							System.err.println(
								"Service: illegal access to: " + className);
						} catch (NoClassDefFoundError e) {
							System.err.println(
								"Service: " + e + " for " + className);
						} catch (Exception e) {
							System.err.println(
								"Service: exception for: "
									+ className
									+ " "
									+ e);
						}
					}
				}
			} catch (IOException ioe) {
				System.err.println("Service: problem with: " + url);
			} finally {
				try {
					if (input != null)
						input.close();
					if (reader != null)
						reader.close();
				} catch (IOException ioe2) {
					System.err.println("Service: problem with: " + url);
				}
			}
		}
		return classMap.values().iterator();
	}
}
