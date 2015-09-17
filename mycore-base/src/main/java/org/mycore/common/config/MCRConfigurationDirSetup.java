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
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.web.Log4jServletContainerInitializer;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;
import org.mycore.common.log4j2.MCRSessionThreadContext;

/**
 * Called by {@link MCRStartupHandler} on start up to setup {@link MCRConfiguration}.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRConfigurationDirSetup implements AutoExecutable {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

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
        loadExternalLibs();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        Map<String, String> properties = configurationLoader.load();
        MCRConfiguration.instance().initialize(properties, true);
        String configFileKey = "log4j.configurationFile";
        if (System.getProperty(configFileKey) == null) {
            URL log4j2ConfigURL = MCRConfigurationDir.getConfigResource("log4j2.xml");
            if (log4j2ConfigURL != null) {
                System.setProperty(configFileKey, log4j2ConfigURL.toString());
            }
        }
        if (System.getProperty(configFileKey) != null) {
            System.out.printf(Locale.ROOT, "Using Log4J2 configuration at: %s\n", System.getProperty(configFileKey));
            if (servletContext != null) {
                Log4jServletContainerInitializer log4jInitializer = new Log4jServletContainerInitializer();
                try {
                    log4jInitializer.onStartup(null, servletContext);
                } catch (ServletException e) {
                    System.err.println("Could not start Log4J2 context");
                }
            }

            LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
            logCtx.reconfigure();
            MCRSessionMgr.addSessionListener(new MCRSessionThreadContext());
        }
    }

    private void loadExternalLibs() {
        File libDir = MCRConfigurationDir.getConfigFile("lib");
        if (libDir != null && libDir.isDirectory()) {
            File[] listFiles = libDir
                .listFiles((FilenameFilter) (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (listFiles.length > 0) {
                ClassLoader classLoader = this.getClass().getClassLoader();
                Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
                try {
                    Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    addUrlMethod.setAccessible(true);
                    for (File jarFile : listFiles) {
                        LOGGER.info("Adding to CLASSPATH: " + jarFile);
                        try {
                            addUrlMethod.invoke(classLoader, jarFile.toURI().toURL());
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | MalformedURLException e) {
                            LogManager.getLogger().error("Could not add " + jarFile + " to current classloader.", e);
                            return;
                        }
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    LogManager.getLogger(MCRConfigurationInputStream.class)
                        .warn(classLoaderClass + " does not support adding additional JARs at runtime", e);
                }
            }
        }
    }
}
