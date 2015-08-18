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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * Called by {@link MCRStartupHandler} on start up to setup {@link MCRConfiguration}.
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
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
        loadExternalLibs();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        Map<String, String> properties = configurationLoader.load();
        MCRConfiguration.instance().initialize(properties, true);
    }

    private void loadExternalLibs() {
        File libDir = MCRConfigurationDir.getConfigFile("lib");
        if (libDir != null && libDir.isDirectory()) {
            File[] listFiles = libDir.listFiles((FilenameFilter) (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (listFiles.length > 0) {
                ClassLoader classLoader = this.getClass().getClassLoader();
                Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
                try {
                    Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    addUrlMethod.setAccessible(true);
                    for (File jarFile : listFiles) {
                        logInfo("Adding to CLASSPATH: " + jarFile);
                        try {
                            addUrlMethod.invoke(classLoader, jarFile.toURI().toURL());
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | MalformedURLException e) {
                            logError("Could not add " + jarFile + " to current classloader.", e);
                            return;
                        }
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    logWarn(classLoaderClass + " does not support adding additional JARs at runtime", e);
                }
            }
        }
    }

    private static void logError(String msg, Throwable e) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRConfigurationInputStream.class).error(msg, e);
        } else {
            System.out.printf(Locale.ROOT,"ERROR: %s\n", msg + toString(e));
        }
    }

    private static String toString(Throwable e) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            pw.println();
            e.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (IOException e1) {
            e1.printStackTrace();
            return "";
        }
    }

    private static void logWarn(String msg, Throwable e) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRConfigurationInputStream.class).warn(msg, e);
        } else {
            System.err.printf(Locale.ROOT,"WARN: %s\n", msg + toString(e));
        }
    }

    private static void logInfo(String msg) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRConfigurationInputStream.class).info(msg);
        } else {
            System.out.printf(Locale.ROOT, "INFO: %s\n", msg);
        }
    }
}
