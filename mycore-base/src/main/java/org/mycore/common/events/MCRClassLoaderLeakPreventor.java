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
        ServiceRegistry.Filter classLoaderFilter = new ServiceRegistry.Filter() {
            @Override
            public boolean filter(Object provider) {
                //remove all service provider loaded by the current ClassLoader
                boolean loadedByWebApp = webClassLoader.equals(provider.getClass().getClassLoader());
                return loadedByWebApp;
            }
        };
        while (categories.hasNext()) {
            @SuppressWarnings("unchecked")
            Class<IIOServiceProvider> category = (Class<IIOServiceProvider>) categories.next();
            Iterator<IIOServiceProvider> serviceProviders = registry.<IIOServiceProvider> getServiceProviders(
                category,
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

}
