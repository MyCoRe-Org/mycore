/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import javax.transaction.Synchronization;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.classifications2.MCRTransactionalDAO;

/**
 * This implementation uses Hibernate transactions bound to the current running thread.
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 * @see SessionFactory#getCurrentSession()
 */
public class MCRTransactionalDAOImpl implements MCRTransactionalDAO {

    private static Logger LOGGER = Logger.getLogger(MCRTransactionalDAOImpl.class);

    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static ThreadLocal<Transaction> tx = new ThreadLocal<Transaction>() {
        protected synchronized Transaction initialValue() {
            return new Transaction() {
                public boolean isActive() {
                    return false;
                }

                public void begin() throws HibernateException {
                }

                public void commit() throws HibernateException {
                }

                public void registerSynchronization(Synchronization arg0) throws HibernateException {
                }

                public void rollback() throws HibernateException {
                }

                public void setTimeout(int arg0) {
                }

                public boolean wasCommitted() throws HibernateException {
                    return false;
                }

                public boolean wasRolledBack() throws HibernateException {
                    return false;
                }
            };
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRTransactionalDAO#beginTransaction()
     */
    public void beginTransaction() {
        if (!tx.get().isActive()) {
            LOGGER.debug("No active Transaction found, beginning new one.");
            tx.set(sessionFactory.getCurrentSession().beginTransaction());
        } else {
            LOGGER.warn("No nested Transaction supported. Transaction allready active. Skipping beginTransaction()");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRTransactionalDAO#commitTransaction()
     */
    public void commitTransaction() {
        if (tx.get().isActive()) {
            LOGGER.debug("Active Transaction found, committing work.");
            tx.get().commit();
            tx.remove();
        } else {
            LOGGER.warn("No active Transaction found. Skipping commitTransaction()");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRTransactionalDAO#rollBackTransaction()
     */
    public void rollBackTransaction() {
        if (tx.get().isActive()) {
            LOGGER.debug("Active Transaction found, committing work.");
            tx.get().commit();
            tx.remove();
        } else {
            LOGGER.warn("No active Transaction found. Skipping rollBackTransaction()");
        }
    }
}
