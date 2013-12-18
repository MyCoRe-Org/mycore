/**
 * 
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
package org.mycore.common.events;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.xml.MCRURIResolver;

/**
 * is a shutdown hook for the current <code>ServletContext</code>.
 * 
 * For this class to register itself as a shutdown hook to the current ServletContext please add the following code to your web.xml (allready done in MyCoRe-shipped version):
 * <pre>
      &lt;listener&gt;
            &lt;listener-class&gt;org.mycore.common.events.MCRServletContextListener&lt;/listener-class&gt;
      &lt;/listener&gt;
 * </pre> 
 * 
 * @author Thomas Scheffler (yagee)
 * @see org.mycore.common.events.MCRShutdownHandler
 * @since 1.3
 */
public class MCRServletContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(MCRServletContextListener.class);

    public void contextInitialized(ServletContextEvent sce) {
        MCRStartupHandler.startUp(sce.getServletContext());
        //Make sure logging is configured
        //initialize MCRURIResolver
        MCRURIResolver.init(sce.getServletContext());
        // register to MCRShutdownHandler
        LOGGER.info("Register ServletContextListener to MCRShutdownHandler");
        try {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources("META-INF/web-fragment.xml");
            while (resources.hasMoreElements()) {
                LOGGER.info("Found: " + resources.nextElement().toString());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        MCRShutdownHandler.getInstance().isWebAppRunning = true;
        LOGGER.info("This class is here: " + getSource(this.getClass()));
        LOGGER.info("I have these components for you: " + MCRRuntimeComponentDetector.getAllComponents());
        LOGGER.info("I have these mycore components for you: " + MCRRuntimeComponentDetector.getMyCoReComponents());
        LOGGER.info("I have these app modules for you: " + MCRRuntimeComponentDetector.getApplicationModules());
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // shutdown event
        MCRShutdownHandler.getInstance().shutDown();
    }

    private static String getKey(ServletContext sc) {
        String systemContext = System.getProperty("mycore.context");
        String contextPath = sc.getContextPath();
        if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        } else {
            //ROOT context
            contextPath = sc.getInitParameter("defaultContext");
        }
        if (contextPath.length() > 0) {

        }
        return null;
    }

    private static String getSource(Class clazz) {
        if (clazz == null) {
            return null;
        }
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            LOGGER.warn("Cannot get CodeSource.");
            return null;
        }
        URL location = codeSource.getLocation();
        String fileName = location.getFile();
        File sourceFile = new File(fileName);
        return sourceFile.getName();
    }
}
