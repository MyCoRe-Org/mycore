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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.query.Query;
import org.hibernate.stat.Statistics;
import org.jdom2.Element;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;

/**
 * Class for hibernate connection to selected database
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRHIBConnection {
    static MCRHIBConnection SINGLETON;

    private static Logger LOGGER = LogManager.getLogger(MCRHIBConnection.class);

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

    public static boolean isEnabled() {
        return MCRConfiguration.instance().getBoolean("MCR.Persistence.Database.Enable", true)
            && MCREntityManagerProvider.getEntityManagerFactory() != null;
    }

    /**
     * This method initializes the connection to the database
     */
    protected MCRHIBConnection() throws MCRPersistenceException {
    }

    /**
     * This method returns the current session for queries on the database through hibernate
     * 
     * @return Session current session object
     */
    public Session getSession() {
        EntityManager currentEntityManager = MCREntityManagerProvider.getCurrentEntityManager();
        Session session = currentEntityManager.unwrap(Session.class);
        if (!session.isOpen()) {
            LOGGER.warn(MessageFormat.format("Hibernate session {0} is closed.",
                Integer.toHexString(session.hashCode())));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Returning session: {0} open: {1}",
                Integer.toHexString(session.hashCode()), session.isOpen()));
        }
        return session;
    }

    public SessionFactory getSessionFactory() {
        return MCREntityManagerProvider.getEntityManagerFactory().unwrap(SessionFactory.class);
    }

    @Deprecated
    public Metadata getMetadata() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method checks existance of mapping for given sql-tablename
     * 
     * @param tablename
     *            sql-table name as string
     * @return boolean
     */
    @Deprecated
    public boolean containsMapping(String tablename) {
        return MCREntityManagerProvider
            .getEntityManagerFactory()
            .getMetamodel()
            .getEntities()
            .stream()
            .map(EntityType::getJavaType)
            .map(Class::getName)
            .filter(tablename::equals)
            .findFirst()
            .isPresent();
    }

    /**
     * helper mehtod: translates fieldtypes into hibernate types
     * 
     * @param type
     *            typename as string
     * @return hibernate type
     */
    @Deprecated
    public org.hibernate.type.Type getHibType(String type) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public void handleStatistics(Statistics stats) throws FileNotFoundException, IOException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public Element addStringArray(Element base, String tagName, String attrName, String[] values) {
        throw new UnsupportedOperationException();
    }

    /**
     * returns the named query from the hibernate mapping. if a query with name <code>name.&lt;DBDialect&gt;</code>
     * exists it takes precedence over a query named <code>name</code>
     * 
     * @return Query defined in mapping
     */
    public Query<?> getNamedQuery(String name) {
        LOGGER.debug("Using query named:" + name);
        return getSession().getNamedQuery(name);
    }

}
