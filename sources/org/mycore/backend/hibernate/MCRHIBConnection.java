/*
 * $RCSfile$
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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.mapping.Table;
import org.hibernate.stat.Statistics;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.services.mbeans.MCRJMXBridge;

/**
 * Class for hibernate connection to selected database
 * 
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRHIBConnection implements Closeable, MCRSessionListener {
    static Configuration HIBCFG;

    static SessionFactory SESSION_FACTORY;

    static MCRHIBConnection SINGLETON;

    private static Logger LOGGER = Logger.getLogger(MCRHIBConnection.class);

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
            MCRSessionMgr.addSessionListener(this);
        }
    }

    /**
     * This method creates the configuration needed by hibernate
     */
    private void buildConfiguration() {
        String resource=System.getProperty("MCR.Hibernate.Configuration", "hibernate.cfg.xml");
        HIBCFG = new Configuration().configure(resource);
        System.out.println("Hibernate configured");
    }

    /**
     * This method creates the SessionFactory for hiberante
     */
    private static void buildSessionFactory() {
        if (SESSION_FACTORY == null) {
            SESSION_FACTORY = HIBCFG.buildSessionFactory();
        }
    }

    public static void buildSessionFactory(Configuration config) {
        SESSION_FACTORY.close();
        SESSION_FACTORY = config.buildSessionFactory();
        HIBCFG = config;
    }
    
    private static void registerStatisticsService(){
        StatisticsService stats=new StatisticsService();
        MCRJMXBridge.registerMe(stats, "Hibernate", "Statistics");
    }

    /**
     * This method returns the current session for queries on the database
     * through hibernate
     * 
     * @return Session current session object
     */
    public Session getSession() {
        Session session = SESSION_FACTORY.getCurrentSession();
        session.setFlushMode(FlushMode.COMMIT);
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
        Iterator it = HIBCFG.getTableMappings();
        while (it.hasNext()) {
            if (((Table) it.next()).getName().equals(tablename)) {
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

    public void close() {
        LOGGER.debug("Closing hibernate sessions.");
        Statistics stats = SESSION_FACTORY.getStatistics();
        if (stats.isStatisticsEnabled()) {
            try {
                handleStatistics(stats);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        SESSION_FACTORY.close();
    }

    public void handleStatistics(Statistics stats) throws FileNotFoundException, IOException {
        File statsFile = new File(MCRConfiguration.instance().getString("MCR.Save.FileSystem"), "hibernatestats.xml");
        Document doc = new Document(new Element("hibernatestats"));
        doc.getRootElement().addContent(new Element("metric").setAttribute("connectCount", String.valueOf(stats.getConnectCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("flushCount", String.valueOf(stats.getFlushCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("transactionCount", String.valueOf(stats.getTransactionCount())));
        doc.getRootElement()
                .addContent(new Element("metric").setAttribute("successfulTransactionCount", String.valueOf(stats.getSuccessfulTransactionCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("sessionOpenCount", String.valueOf(stats.getSessionOpenCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("sessionCloseCount", String.valueOf(stats.getSessionCloseCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("sessionOpenCount", String.valueOf(stats.getSessionOpenCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("QueryExecutionCount", String.valueOf(stats.getQueryExecutionCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("QueryExecutionMaxTime", String.valueOf(stats.getQueryExecutionMaxTime())));
        if (stats.getQueryExecutionMaxTimeQueryString() != null) {
            doc.getRootElement().addContent(new Element("longestQuery").setAttribute("value", stats.getQueryExecutionMaxTimeQueryString()));
        }
        doc.getRootElement().addContent(new Element("metric").setAttribute("CollectionFetchCount", String.valueOf(stats.getCollectionFetchCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("CollectionLoadCount", String.valueOf(stats.getCollectionLoadCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("CollectionRecreateCount", String.valueOf(stats.getCollectionRecreateCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("CollectionRemoveCount", String.valueOf(stats.getCollectionRemoveCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("CollectionUpdateCount", String.valueOf(stats.getCollectionUpdateCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("EntityDeleteCount", String.valueOf(stats.getEntityDeleteCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("EntityFetchCount", String.valueOf(stats.getEntityFetchCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("EntityLoadCount", String.valueOf(stats.getEntityLoadCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("EntityInsertCount", String.valueOf(stats.getEntityInsertCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("EntityUpdateCount", String.valueOf(stats.getEntityUpdateCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("queryCacheHitCount", String.valueOf(stats.getQueryCacheHitCount())));
        doc.getRootElement().addContent(new Element("metric").setAttribute("queryCacheMissCount", String.valueOf(stats.getQueryCacheMissCount())));
        doc.getRootElement().addContent(addStringArray(new Element("queries"), "query", "value", stats.getQueries()));
        new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileOutputStream(statsFile));
    }

    public Element addStringArray(Element base, String tagName, String attrName, String[] values) {
        for (String value : values) {
            base.addContent(new Element(tagName).setAttribute(attrName, value));
        }
        return base;
    }

    public SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public void sessionEvent(MCRSessionEvent event) {
        // TODO Auto-generated method stub
        
    }
    
    public void flushSession(){
        Session currentSession=getSession();
        currentSession.setFlushMode(FlushMode.MANUAL);
        currentSession.flush();
        currentSession.setFlushMode(FlushMode.COMMIT);
    }

}
