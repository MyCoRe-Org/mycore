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

package org.mycore.backend.hibernate;

import java.text.MessageFormat;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
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

    /**
     * returns the named query from the hibernate mapping. if a query with name <code>name.&lt;DBDialect&gt;</code>
     * exists it takes precedence over a query named <code>name</code>
     * 
     * @return Query defined in mapping
     */
    public Query<?> getNamedQuery(String name) {
        LOGGER.debug("Using query named:{}", name);
        return getSession().getNamedQuery(name);
    }

}
