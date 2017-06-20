/**
 * 
 */
package org.mycore.common.events;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.spi.ServiceRegistry;
import javax.servlet.ServletContextEvent;

import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor;

/**
 * A {@link ClassLoaderLeakPreventor} that also prevent ImageIO leaks on shutdown.
 * 
 * @author Thomas Scheffler 
 * @since 2015.02
 */
class MCRClassLoaderLeakPreventor extends ClassLoaderLeakPreventor {
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (!isJvmShuttingDown()) {
            clearImageIORegistry();
        }
        super.contextDestroyed(servletContextEvent);
    }

    protected void clearImageIORegistry() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator<Class<?>> categories = registry.getCategories();
        final ClassLoader webClassLoader = getWebApplicationClassLoader();
        ServiceRegistry.Filter classLoaderFilter = provider -> {
            //remove all service provider loaded by the current ClassLoader
            boolean loadedByWebApp = webClassLoader.equals(provider.getClass().getClassLoader());
            return loadedByWebApp;
        };
        while (categories.hasNext()) {
            @SuppressWarnings("unchecked")
            Class<IIOServiceProvider> category = (Class<IIOServiceProvider>) categories.next();
            Iterator<IIOServiceProvider> serviceProviders = registry.<IIOServiceProvider> getServiceProviders(category,
                classLoaderFilter, true);
            if (serviceProviders.hasNext()) {
                info("removing service provider of category: " + category.getSimpleName());
                //copy to list
                LinkedList<IIOServiceProvider> serviceProviderList = new LinkedList<>();
                while (serviceProviders.hasNext()) {
                    serviceProviderList.add(serviceProviders.next());
                }
                for (IIOServiceProvider serviceProvider : serviceProviderList) {
                    info(" - removing: " + serviceProvider.getDescription(Locale.ROOT));
                    registry.deregisterServiceProvider(serviceProvider);
                }
            }
        }
    }

    protected void initJarUrlConnection() {
        /*
         * this preventer was for the JDK bug id=4405789 -> which duplicate bug id=4353705 and id=4405807
         * bug id=4353705 was fixed in ver 1.3.1_03 see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4353705
         * bug id=4405807 was fixed in ver 1.4.2_06 no working link use use Google cache instead
         */
    };

}
