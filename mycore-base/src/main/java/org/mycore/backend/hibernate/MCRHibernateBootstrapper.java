/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 17, 2013 $
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

package org.mycore.backend.hibernate;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRHibernateBootstrapper implements AutoExecutable {

    private static Logger LOGGER = LogManager.getLogger();

    private static URL getHibernateConfig() {
        File configFile = MCRConfigurationDir.getConfigFile(getHibernateConfigResourceName());
        if (configFile != null && configFile.canRead()) {
            try {
                return configFile.toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn("Error while looking for: " + configFile, e);
            }
        }
        return MCRConfigurationDir.getConfigResource(getHibernateConfigResourceName());
    }

    private static String getResourceName(String className) {
        return className.replaceAll("\\.", "/") + ".hbm.xml";
    }

    private static Class<?> getAnnotatedClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + className);
            return null;
        }
    }

    static String getHibernateConfigResourceName() {
        //do not query MCRConfiguration as it is maybe not yet initialized.
        return "hibernate.cfg.xml"; //standard anyway
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return "Hibernate schema updater";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return 1000;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp()
     */
    @Override
    public void startUp(ServletContext servletContext) {
        setup(MCRHibernateBootstrapper::updateSchema);
    }

    public static void setup(Consumer<Metadata> schemaupdater) {
        final URL hibernateConfig = getHibernateConfig();
        if (MCRConfiguration.instance().getBoolean("MCR.Persistence.Database.Enable", true)
            && hibernateConfig != null) {
            StandardServiceRegistry standardRegistry = getStandardRegistry(hibernateConfig);
            Metadata metadata = getMetadata(standardRegistry);
            SessionFactory sessionFactory = getSessionFactory(metadata);
            String dialect = getDialect(metadata);
            schemaupdater.accept(metadata);
            MCRHIBConnection.init(sessionFactory, (MetadataImplementor) metadata, dialect);
        } else {
            LogManager.getLogger().warn("Hibernate is disabled or unconfigured.");
        }
    }

    public static void updateSchema(Metadata metadata) {
        new SchemaUpdate((MetadataImplementor) metadata).execute(false, true);
    }

    private static String getDialect(Metadata metadata) {
        if (MCRConfiguration.instance().getBoolean("MCR.Hibernate.DialectQueries", false)) {
            return metadata.getDatabase().getDialect().getClass().getSimpleName();
        } else {
            return null;
        }
    }

    private static SessionFactory getSessionFactory(Metadata metadata) {
        return metadata.getSessionFactoryBuilder().build();
    }

    private static Metadata getMetadata(StandardServiceRegistry standardRegistry) {
        MetadataSources metadataSources = new MetadataSources(standardRegistry);
        return MCRConfiguration.instance()
            .getStrings("MCR.Hibernate.Mappings")
            .stream()
            .map(className -> addMapping(metadataSources, className))
            .reduce((l,r) -> r)
            .get()
            .getMetadataBuilder()
            .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
            .build();
    }

    private static MetadataSources addMapping(MetadataSources metadataSources, String className) {
        String resourceName = getResourceName(className);
        if (MCRHibernateBootstrapper.class.getClassLoader().getResource(resourceName) != null) {
            LOGGER.info("Add mapping: " + resourceName);
            return metadataSources.addResource(resourceName);
        } else {
            LOGGER.info("Add annotated class: " + className);
            return metadataSources.addAnnotatedClass(getAnnotatedClass(className));
        }
    }

    private static StandardServiceRegistry getStandardRegistry(URL hibernateConfigURL) {
        return new StandardServiceRegistryBuilder().configure(hibernateConfigURL).build();
    }

}
