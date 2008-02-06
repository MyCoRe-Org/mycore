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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
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
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.TextFilterPlugin.";
        Properties props = config.getProperties(prefix);
        if (props == null) {
            return;
        }
        
        TextFilterPlugin filter = null;
        MCRFileContentType ct;
        
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements())
        {
          String propertyName = (String) e.nextElement();
          try
          {
            Object o = config.getInstanceOf(propertyName);
            if (null != o)
            {
              filter = (TextFilterPlugin)o;
              LOGGER.info(new StringBuffer(propertyName + "Loading TextFilterPlugin: ").append(filter.getName()).append(" v:").append(filter.getMajorNumber()).append('.').append(filter.getMinorNumber()).toString());
              for (Iterator CtIterator = filter.getSupportedContentTypes().iterator(); CtIterator.hasNext();) {
                // Add MIME Type filters to the basket
                ct = (MCRFileContentType) CtIterator.next();

                if (ct != null) {
                    CONTENT_TYPE_PLUGIN_BAG.put(ct, filter);
                }
            }

            PLUGINS.put(filter.getClass().getName(), filter);
            }
            else LOGGER.info("TextFilterPlugin not available: "+ propertyName + " with property: " + props.getProperty(propertyName));
          } catch (Exception e1)
          {
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
    public Collection getPlugins() {
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
        return (ct == null) ? null : (TextFilterPlugin) CONTENT_TYPE_PLUGIN_BAG.get(ct);
    }

    /**
     * checks whether content type is supported
     * 
     * @param ct content type
     *            of Inputstream
     * @return true if content type is supported, else false
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

}
