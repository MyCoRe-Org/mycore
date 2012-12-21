/**
 * 
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
package org.mycore.services.queuedjob;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;

/**
 * Resets jobs that took to long to perform action.
 * Set property <code>MCR.QueuedJob.TimeTillReset</code> to alter grace period.
 * 
 * @author Ren\u00E9 Adler
 */
public class MCRStalledJobResetter implements Runnable {
    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static HashMap<String, MCRStalledJobResetter> INSTANCES = new HashMap<String, MCRStalledJobResetter>();

    private static Logger LOGGER = Logger.getLogger(MCRStalledJobResetter.class);
    
    private int maxTimeDiff = MCRConfiguration.instance().getInt(MCRJobQueue.CONFIG_PREFIX + "TimeTillReset", 10);
    
    private Class<? extends MCRJobAction> action = null;

    private MCRStalledJobResetter(Class<? extends MCRJobAction> action) {
        if (action != null) {
            this.action = action;
            maxTimeDiff = MCRConfiguration.instance().getInt(MCRJobQueue.CONFIG_PREFIX + action.getSimpleName() + ".TimeTillReset", maxTimeDiff);
        }
    }
    
    public static MCRStalledJobResetter getInstance(Class<? extends MCRJobAction> action) {
        String key = action != null && !MCRJobQueue.singleQueue ? action.getName() : "single";
        
        MCRStalledJobResetter resetter = INSTANCES.get(key);
        if (resetter == null) {
            resetter = new MCRStalledJobResetter(MCRJobQueue.singleQueue ? null : action);
            INSTANCES.put(key, resetter);
        }
        
        return resetter;
    }

    /**
     * Resets jobs to {@link MCRJobStatus#NEW} that where in status {@link MCRJobStatus#PROCESSING} for to long time.
     */
    public void run() {
        boolean reset = false;
        Session session = sessionFactory.getCurrentSession();
        Transaction executorTransaction = session.beginTransaction();
        LOGGER.info("MCRJob is Checked for dead Entries");
        
        StringBuilder sb = new StringBuilder("FROM MCRJob WHERE ");
        if (action != null)
            sb.append("action='" + action.getName() + "' AND ");
        sb.append(" status='" + MCRJobStatus.PROCESSING + "' ORDER BY id ASC");

        Query query = session.createQuery(sb.toString());

        long start = 0;
        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> result = query.iterate();
        while (result.hasNext()) {
            MCRJob job = result.next();
            start = job.getStart().getTime() / 60000;
            if (current - start >= maxTimeDiff) {
                LOGGER.debug("->Resetting too long in queue");

                job.setStatus(MCRJobStatus.NEW);
                job.setStart(null);
                session.update(job);
                reset = true;
            } else {
                LOGGER.debug("->ok");
            }
        }
        try {
            executorTransaction.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (executorTransaction != null) {
                executorTransaction.rollback();
                reset = false;//No changes are applied, so no notification is needed as well
            }
        }
        //Only notify Listeners on Queue if really something is set back
        if (reset) {
            synchronized (MCRJobQueue.getInstance(action)) {
                MCRJobQueue.getInstance(action).notifyListener();
            }
        }
        session.close();
        LOGGER.info("MCRJob checking is done");
    }
}
