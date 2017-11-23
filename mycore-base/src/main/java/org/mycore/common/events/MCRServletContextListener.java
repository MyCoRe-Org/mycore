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
