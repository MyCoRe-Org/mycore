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

package org.mycore.services.plugins;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentType;

/**
 * Loads and manages plugins
 * 
 * @author Thomas Scheffler (yagee)
 */
public class TextFilterPluginManager {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(TextFilterPluginManager.class);

    /** The configuration */
    private static final MCRConfiguration CONF = MCRConfiguration.instance();

    /** Pluginbasket */
    private static Hashtable CONTENT_TYPE_PLUGIN_BAG = null;

    private static Hashtable PLUGINS = null;

    /** initialized */
    private static TextFilterPluginManager SINGLETON;

    /**
     * 
     */
    private TextFilterPluginManager() {
        init();
    }

    public static TextFilterPluginManager getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new TextFilterPluginManager();
        }

        return SINGLETON;
    }

    private void init() {
        CONTENT_TYPE_PLUGIN_BAG = new Hashtable();
        PLUGINS = new Hashtable();
        loadPlugins();
    }

    /**
     * load TextFilterPlugins from the MCR.PluginDirectory
     * 
     */
    public void loadPlugins() {
        URLClassLoader classLoader;

        try {
            classLoader = new URLClassLoader(getPluginURLs(), Thread.currentThread().getContextClassLoader());
        } catch (MalformedURLException e) {
            // should "never" happen
            throw new MCRException("Failure getting URLs from plugins!", e);
        }

        TextFilterPlugin filter = null;
        MCRFileContentType ct;

        for (Iterator iter = MCRUtils.getProviders(TextFilterPlugin.class, classLoader); iter.hasNext();) {
            filter = (TextFilterPlugin) iter.next();
            LOGGER.info(new StringBuffer("Loading TextFilterPlugin: ").append(filter.getName()).append(" v:").append(filter.getMajorNumber()).append('.').append(filter.getMinorNumber()).toString());

            for (Iterator CtIterator = filter.getSupportedContentTypes().iterator(); CtIterator.hasNext();) {
                // Add MIME Type filters to the basket
                ct = (MCRFileContentType) CtIterator.next();

                if (ct != null) {
                    CONTENT_TYPE_PLUGIN_BAG.put(ct, filter);
                }
            }

            PLUGINS.put(filter.getClass().getName(), filter);
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
        return PLUGINS.values();
    }

    /**
     * returns TextFilterPlugin to corresponding MIME type
     * 
     * @param supported
     *            MIME type
     * @return corresponding TextFilterPlugin or null if MIME is emtpy or null
     */
    public TextFilterPlugin getPlugin(MCRFileContentType ct) {
        return (ct == null) ? null : (TextFilterPlugin) CONTENT_TYPE_PLUGIN_BAG.get(ct);
    }

    /**
     * returns true if MIME type is supported
     * 
     * @param MIME
     *            of Inputstream
     * @return true if MIME type is supported, else false
     */
    public boolean isSupported(MCRFileContentType ct) {
        return (ct == null) ? false : CONTENT_TYPE_PLUGIN_BAG.containsKey(ct);
    }

    /**
     * returns a Reader for the characters of the InputStream
     * 
     * @param ct
     *            ContentType of the InputStream
     * @param input
     *            InputStream to be parsed
     * @return null if ContentType is unsupported, else a Reader for the parsed
     *         characters
     * @throws FilterPluginTransformException
     */
    public Reader transform(MCRFileContentType ct, InputStream input) throws FilterPluginTransformException {
        if (isSupported(ct)) {
            return getPlugin(ct).transform(ct, input);
        }

        return null;
    }

    /**
     * returns the URLs of all plugins found in MCR.PluginDirectory
     * 
     * @return Array of URL of plugin-JARs
     * @throws MalformedURLException
     */
    private final URL[] getPluginURLs() throws MalformedURLException {
        HashSet returnS = new HashSet();
        File pluginDir = new File(CONF.getString("MCR.PluginDirectory"));

        if ((pluginDir == null) || !pluginDir.isDirectory()) {
            LOGGER.info("PluginDirectory does not exist! " + pluginDir.getAbsolutePath());

            return new URL[0];
        }

        File[] plugins = pluginDir.listFiles();

        for (int i = 0; i < plugins.length; i++) {
            System.err.println(plugins[i].getName());

            if (plugins[i].isFile() && plugins[i].getName().toUpperCase().endsWith(".JAR")) {
                // This Jar file possibly contains a text filter plugin
                returnS.add(plugins[i].toURL());
            }
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
}
