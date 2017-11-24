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
            return webClassLoader.equals(provider.getClass().getClassLoader());
        };
        while (categories.hasNext()) {
            @SuppressWarnings("unchecked")
            Class<IIOServiceProvider> category = (Class<IIOServiceProvider>) categories.next();
            Iterator<IIOServiceProvider> serviceProviders = registry.getServiceProviders(category,
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
    }

}
