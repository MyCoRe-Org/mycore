/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.events;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor;
import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventorFactory;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRServletContainerInitializer implements ServletContainerInitializer {

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletContainerInitializer#onStartup(java.util.Set, jakarta.servlet.ServletContext)
     */
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        final boolean runClassLoaderLeakPreventor = runClassLoaderLeakPreventor();
        ClassLoaderLeakPreventor leakPreventor = null;
        if (runClassLoaderLeakPreventor) {
            final ClassLoaderLeakPreventorFactory leakPreventorFactory = new ClassLoaderLeakPreventorFactory();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            leakPreventor = leakPreventorFactory.newLeakPreventor(classLoader);
            leakPreventor.runPreClassLoaderInitiators();
        }
        MCRShutdownHandler shutdownHandler = MCRShutdownHandler.getInstance();
        shutdownHandler.isWebAppRunning = true;
        shutdownHandler.leakPreventor = leakPreventor;
        MCRStartupHandler.startUp(ctx);
        //Make sure logging is configured
        final Logger logger = LogManager.getLogger();
        if (logger.isDebugEnabled()) {
            try {
                Enumeration<URL> resources = MCRClassTools.getClassLoader().getResources("META-INF/web-fragment.xml");
                while (resources.hasMoreElements()) {
                    logger.debug("Found: {}", resources.nextElement());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.debug("This class is here: {}", getSource(this.getClass()));
        }
    }

    private static String getSource(final Class<? extends MCRServletContainerInitializer> clazz) {
        if (clazz == null) {
            return null;
        }
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            LogManager.getLogger().warn("Cannot get CodeSource.");
            return null;
        }
        URL location = codeSource.getLocation();
        String fileName = location.getFile();
        File sourceFile = new File(fileName);
        return sourceFile.getName();
    }

    private boolean runClassLoaderLeakPreventor() {
        //do not run ClassLoaderLeakPreventor by default on JRE 17
        String defaultValue = (Runtime.version().feature() > 11) ? Boolean.FALSE.toString() : Boolean.TRUE.toString();
        final String propValue = System.getProperty("MCR.ClassLoaderLeakPreventor", defaultValue);
        return Boolean.parseBoolean(propValue);
    }

}
