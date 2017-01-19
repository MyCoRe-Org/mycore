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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;

/**
 * Resets jobs that took to long to perform action.
 * Set property <code>MCR.QueuedJob.TimeTillReset</code> to alter grace period.
 * 
 * @author Ren\u00E9 Adler
 */
public class MCRStalledJobResetter implements Runnable {

    private static HashMap<String, MCRStalledJobResetter> INSTANCES = new HashMap<String, MCRStalledJobResetter>();

    private static Logger LOGGER = LogManager.getLogger(MCRStalledJobResetter.class);

    private int maxTimeDiff = MCRConfiguration.instance().getInt(MCRJobQueue.CONFIG_PREFIX + "TimeTillReset", 10);

    private Class<? extends MCRJobAction> action = null;

    private MCRStalledJobResetter(Class<? extends MCRJobAction> action) {
        if (action != null) {
            this.action = action;
            maxTimeDiff = MCRConfiguration.instance()
                    .getInt(MCRJobQueue.CONFIG_PREFIX + action.getSimpleName() + ".TimeTillReset", maxTimeDiff);
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
        EntityManager em = MCREntityManagerProvider.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        LOGGER.info("MCRJob is Checked for dead Entries");
        transaction.begin();

        StringBuilder sb = new StringBuilder("FROM MCRJob WHERE ");
        if (action != null)
            sb.append("action='" + action.getName() + "' AND ");
        sb.append(" status='" + MCRJobStatus.PROCESSING + "' ORDER BY id ASC");

        TypedQuery<MCRJob> query = em.createQuery(sb.toString(), MCRJob.class);

        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        boolean reset = query
                .getResultList()
                .stream()
                .map(job -> {
                    boolean ret = false;
                    long start = job.getStart().getTime() / 60000;
                    if (current - start >= maxTimeDiff) {
                        LOGGER.debug("->Resetting too long in queue");

                        job.setStatus(MCRJobStatus.NEW);
                        job.setStart(null);
                        ret = true;
                    } else {
                        LOGGER.debug("->ok");
                    }
                    return ret;
                })
                .reduce(Boolean::logicalOr)
                .orElse(false);
        try {
            transaction.commit();
        } catch (RollbackException e) {
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();
                reset = false;//No changes are applied, so no notification is needed as well
            }
        }
        //Only notify Listeners on Queue if really something is set back
        if (reset) {
            synchronized (MCRJobQueue.getInstance(action)) {
                MCRJobQueue.getInstance(action).notifyListener();
            }
        }
        em.close();
        LOGGER.info("MCRJob checking is done");
    }
}
