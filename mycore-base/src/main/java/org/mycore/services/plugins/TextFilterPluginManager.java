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

package org.mycore.services.plugins;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFileContentType;

/**
 * Loads and manages plugins
 * 
 * @author Thomas Scheffler (yagee)
 */
public class TextFilterPluginManager {
    /** The logger */
    private static final Logger LOGGER = Logger.getLogger(TextFilterPluginManager.class);

    /** Pluginbasket */
    private static Hashtable<MCRFileContentType, TextFilterPlugin> CONTENT_TYPE_PLUGIN_BAG = null;

    private static Hashtable<String, TextFilterPlugin> PLUGINS = null;

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
        CONTENT_TYPE_PLUGIN_BAG = new Hashtable<MCRFileContentType, TextFilterPlugin>();
        PLUGINS = new Hashtable<String, TextFilterPlugin>();
        loadPlugins();
    }

    /**
     * load TextFilterPlugins from the MCR.PluginDirectory
     * 
     */
    public void loadPlugins() {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.TextFilterPlugin.";
        Map<String, String> props = config.getPropertiesMap(prefix);

        for (Entry<String, String> entry : props.entrySet()) {
            try {
                TextFilterPlugin filter = config.getInstanceOf(entry.getKey());
                if (null != filter) {
                    LOGGER.info(entry.getKey() + "Loading TextFilterPlugin: " + filter.getName() + " v:"
                        + filter.getMajorNumber() + '.' + filter.getMinorNumber());
                    for (MCRFileContentType ct : filter.getSupportedContentTypes()) {
                        // Add MIME Type filters to the basket
                        CONTENT_TYPE_PLUGIN_BAG.put(ct, filter);
                    }

                    PLUGINS.put(filter.getClass().getName(), filter);
                } else {
                    LOGGER.info("TextFilterPlugin not available: " + entry.getKey() + " with property: "
                        + entry.getValue());
                }
            } catch (Exception e1) {
                LOGGER.info(e1.toString());
            }

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
    public Collection<TextFilterPlugin> getPlugins() {
        return PLUGINS.values();
    }

    /**
     * returns TextFilterPlugin to corresponding MIME type
     * 
     * @param ct supported
     *            MIME type
     * @return corresponding TextFilterPlugin or null if MIME is emtpy or null
     */
    public TextFilterPlugin getPlugin(MCRFileContentType ct) {
        return ct == null ? null : CONTENT_TYPE_PLUGIN_BAG.get(ct);
    }

    /**
     * checks whether content type is supported
     * 
     * @param ct content type
     *            of Inputstream
     * @return true if content type is supported, else false
     */
    public boolean isSupported(MCRFileContentType ct) {
        return ct != null && CONTENT_TYPE_PLUGIN_BAG.containsKey(ct);
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
            try {
                return getPlugin(ct).transform(ct, input);
            } catch (Exception ex) {
                LOGGER.warn("Exception in text filter plug-in:", ex);
                return new StringReader("");
            }
        }

        return null;
    }

}
