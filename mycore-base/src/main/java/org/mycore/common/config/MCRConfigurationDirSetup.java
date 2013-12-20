/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 18, 2013 $
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
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRConfigurationDirSetup implements AutoExecutable {

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return "Setup of MCRConfigurationDir";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(javax.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        MCRConfigurationDir.setServletContext(servletContext);
        File libDir = MCRConfigurationDir.getConfigFile("libs");
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        Map<String, String> properties = configurationLoader.load();
        MCRConfiguration.instance().initialize(properties, true);
        if (libDir != null && libDir.isDirectory()) {
            File[] listFiles = libDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            });
            if (listFiles.length > 0) {
                Logger logger = Logger.getLogger(getClass());
                ClassLoader classLoader = this.getClass().getClassLoader();
                Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
                try {
                    Method addUrlMethod = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                    addUrlMethod.setAccessible(true);
                    for (File jarFile : listFiles) {
                        logger.info("Adding to CLASSPATH: " + jarFile);
                        try {
                            addUrlMethod.invoke(classLoader, jarFile.toURI().toURL());
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | MalformedURLException e) {
                            logger.error("Could not add " + jarFile + " to current classloader.", e);
                            return;
                        }
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    logger.warn(classLoaderClass + " does not support adding additional JARs at runtime");
                }
            }
        }
    }
}
