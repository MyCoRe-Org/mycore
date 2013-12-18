/*
 * 
 * $Revision$ $Date$
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.stat.Statistics;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.services.mbeans.MCRJMXBridge;

/**
 * Class for hibernate connection to selected database
 * 
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRHIBConnection implements Closeable {
    private static Configuration HIBCFG;

    private ServiceRegistry serviceRegistry;

    private SessionFactory sessionFactory;

    static MCRHIBConnection SINGLETON;

    private static Logger LOGGER = Logger.getLogger(MCRHIBConnection.class);

    private static String DIALECT;

    @Override
    protected void finalize() throws Throwable {
        System.out.println("\n" + this.getClass() + "is finalized!\n");
        super.finalize();
    }

    public static synchronized MCRHIBConnection instance() throws MCRPersistenceException {
        if (SINGLETON == null) {
            SINGLETON = new MCRHIBConnection();
        }
        return SINGLETON;
    }

    /**
     * This method initializes the connection to the database
     * 
     * @throws MCRPersistenceException
     */
    protected MCRHIBConnection() throws MCRPersistenceException {
        try {
            buildConfiguration();
            buildSessionFactory();
            registerStatisticsService();
        } catch (Exception exc) {
            String msg = "Could not connect to database";
            throw new MCRPersistenceException(msg, exc);
        } finally {
            MCRShutdownHandler.getInstance().addCloseable(this);
        }
    }

    /**
     * This method creates the configuration needed by hibernate
     */
    private void buildConfiguration() {
        String resource = System.getProperty("MCR.Hibernate.Configuration", "hibernate.cfg.xml");
        HIBCFG = new Configuration().configure(resource);
        if (MCRConfiguration.instance().getBoolean("MCR.Hibernate.DialectQueries", false)) {
            String dialect = HIBCFG.getProperty("hibernate.dialect");
            DIALECT = dialect.substring(dialect.lastIndexOf('.') + 1);
        } else {
            DIALECT = null;
        }
        List<String> mappings = MCRConfiguration.instance().getStrings("MCR.Hibernate.Mappings");
        for (String className : mappings) {
            String resourceName = getResourceName(className);
            LOGGER.info("Add mapping: " + resourceName);
            HIBCFG.addResource(resourceName);
        }
        serviceRegistry = new ServiceRegistryBuilder().applySettings(HIBCFG.getProperties()).buildServiceRegistry();
        LOGGER.info("Hibernate configured");
    }

    private String getResourceName(String className) {
        return className.replaceAll("\\.", "/") + ".hbm.xml";
    }

    /**
     * This method creates the SessionFactory for hiberante
     */
    private void buildSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = HIBCFG.buildSessionFactory(serviceRegistry);
        }
    }

    public synchronized void buildSessionFactory(Configuration config) {
        sessionFactory.close();
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(HIBCFG.getProperties())
            .buildServiceRegistry();
        sessionFactory = config.buildSessionFactory(serviceRegistry);
        HIBCFG = config;
        this.serviceRegistry = serviceRegistry;
    }

    private void registerStatisticsService() {
        StatisticsService stats = new StatisticsService();
        stats.setSessionFactory(getSessionFactory());
        final String hibernateBaseName = "Hibernate";
        MCRJMXBridge.register(stats, hibernateBaseName, "Statistics");
    }

    /**
     * This method returns the current session for queries on the database
     * through hibernate
     * 
     * @return Session current session object
     */
    public Session getSession() {
        Session session = sessionFactory.getCurrentSession();
        if (!session.isOpen()) {
            LOGGER.warn(MessageFormat.format("Hibernate session {0} is closed, generating new session",
                Integer.toHexString(session.hashCode())));
            session = sessionFactory.openSession();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Returning session: {0} open: {1}",
                Integer.toHexString(session.hashCode()), session.isOpen()));
        }
        return session;
    }

    public Configuration getConfiguration() {
        return HIBCFG;
    }

    /**
     * This method checks existance of mapping for given sql-tablename
     * 
     * @param tablename
     *            sql-table name as string
     * @return boolean
     */
    public boolean containsMapping(String tablename) {
        Iterator<Table> it = HIBCFG.getTableMappings();
        while (it.hasNext()) {
            if (it.next().getName().equals(tablename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * helper mehtod: translates fieldtypes into hibernate types
     * 
     * @param type
     *            typename as string
     * @return hibernate type
     */
    public org.hibernate.type.Type getHibType(String type) {
        if (type.equals("integer")) {
            return new IntegerType();
        } else if (type.equals("date")) {
            return new DateType();
        } else if (type.equals("time")) {
            return new TimeType();
        } else if (type.equals("timestamp")) {
            return new TimestampType();
        } else if (type.equals("decimal")) {
            return new DoubleType();
        } else if (type.equals("boolean")) {
            return new BooleanType();
        } else {
            return new StringType();
        }
    }

    public void prepareClose() {
        // nothing to be done to prepare close()
    }

    public void close() {
        if (sessionFactory == null) {
            return;
        }
        LOGGER.debug("Closing hibernate sessions.");
        sessionFactory.close();
        sessionFactory = null;
        SINGLETON = null;
    }

    public void handleStatistics(Statistics stats) throws FileNotFoundException, IOException {
        File statsFile = new File(MCRConfiguration.instance().getString("MCR.Save.FileSystem"), "hibernatestats.xml");
        Document doc = new Document(new Element("hibernatestats"));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("connectCount", String.valueOf(stats.getConnectCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("flushCount", String.valueOf(stats.getFlushCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("transactionCount", String.valueOf(stats.getTransactionCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("successfulTransactionCount",
                String.valueOf(stats.getSuccessfulTransactionCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("sessionOpenCount", String.valueOf(stats.getSessionOpenCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("sessionCloseCount", String.valueOf(stats.getSessionCloseCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("sessionOpenCount", String.valueOf(stats.getSessionOpenCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("QueryExecutionCount", String.valueOf(stats.getQueryExecutionCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("QueryExecutionMaxTime",
                String.valueOf(stats.getQueryExecutionMaxTime())));
        if (stats.getQueryExecutionMaxTimeQueryString() != null) {
            doc.getRootElement().addContent(
                new Element("longestQuery").setAttribute("value", stats.getQueryExecutionMaxTimeQueryString()));
        }
        doc.getRootElement()
            .addContent(
                new Element("metric").setAttribute("CollectionFetchCount",
                    String.valueOf(stats.getCollectionFetchCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("CollectionLoadCount", String.valueOf(stats.getCollectionLoadCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("CollectionRecreateCount",
                String.valueOf(stats.getCollectionRecreateCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("CollectionRemoveCount",
                String.valueOf(stats.getCollectionRemoveCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("CollectionUpdateCount",
                String.valueOf(stats.getCollectionUpdateCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("EntityDeleteCount", String.valueOf(stats.getEntityDeleteCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("EntityFetchCount", String.valueOf(stats.getEntityFetchCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("EntityLoadCount", String.valueOf(stats.getEntityLoadCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("EntityInsertCount", String.valueOf(stats.getEntityInsertCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("EntityUpdateCount", String.valueOf(stats.getEntityUpdateCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("queryCacheHitCount", String.valueOf(stats.getQueryCacheHitCount())));
        doc.getRootElement().addContent(
            new Element("metric").setAttribute("queryCacheMissCount", String.valueOf(stats.getQueryCacheMissCount())));
        doc.getRootElement().addContent(addStringArray(new Element("queries"), "query", "value", stats.getQueries()));
        FileOutputStream fileOutputStream = new FileOutputStream(statsFile);
        try {
            new XMLOutputter(Format.getPrettyFormat()).output(doc, fileOutputStream);
        } finally {
            fileOutputStream.close();
        }
    }

    public Element addStringArray(Element base, String tagName, String attrName, String[] values) {
        for (String value : values) {
            base.addContent(new Element(tagName).setAttribute(attrName, value));
        }
        return base;
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory.isClosed()) {
            sessionFactory = null;
            buildSessionFactory();
        }

        return sessionFactory;
    }

    /**
     * returns the named query from the hibernate mapping.
     * 
     * if a query with name <code>name.&lt;DBDialect&gt;</code> exists it takes
     * precedence over a query named <code>name</code>
     * 
     * @param name
     * @return Query defined in mapping
     */
    public Query getNamedQuery(String name) {
        if (DIALECT != null) {
            String dialectQueryName = name + "." + DIALECT;
            if (HIBCFG.getNamedSQLQueries().containsKey(dialectQueryName)) {
                LOGGER.debug("Using query named:" + dialectQueryName);
                return getSession().getNamedQuery(dialectQueryName);
            }
        } else {
            LOGGER.debug("Dialect specific queries are not enabled");
        }
        LOGGER.debug("Using query named:" + name);
        return getSession().getNamedQuery(name);
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }
}
