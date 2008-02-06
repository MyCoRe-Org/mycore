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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.services.mbeans.MCRDerivate;
import org.mycore.services.mbeans.MCRObject;

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
        //Make sure logging is configured
        MCRConfiguration.instance();
        // register to MCRShutdownHandler
        LOGGER.info("Register ServletContextListener to MCRShutdownHandler");
        MCRShutdownHandler.getInstance().isWebAppRunning = true;
        // register MBeans
        LOGGER.info("Register MBean: MCRObject");
        MCRObject.register();
        LOGGER.info("Register MBean: MCRDerivate");
        MCRDerivate.register();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // shutdown event
        MCRShutdownHandler.getInstance().shutDown();
    }
}