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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * is a shutdown hook for the current <code>ServletContext</code>. For this class to register itself as a shutdown hook
 * to the current ServletContext please add the following code to your web.xml (allready done in MyCoRe-shipped
 * version):
 * 
 * <pre>
 *       &lt;listener&gt;
 *             &lt;listener-class&gt;org.mycore.common.events.MCRServletContextListener&lt;/listener-class&gt;
 *       &lt;/listener&gt;
 * </pre>
 * 
 * @author Thomas Scheffler (yagee)
 * @see org.mycore.common.events.MCRShutdownHandler
 * @since 1.3
 */
public class MCRServletContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // shutdown event
        MCRShutdownHandler.getInstance().shutDown();
    }
}
