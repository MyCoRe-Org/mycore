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

package org.mycore.iview2.services;

import java.util.Date;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.mycore.backend.jpa.MCREntityManagerProvider;

/**
 * Resets jobs that took to long to tile.
 * Set property <code>MCR.Module-iview2.TimeTillReset</code> to alter grace period.
 * Set property <code>MCR.Module-iview2.MaxResetCount</code> to alter maximum tries per job.
 * @author Thomas Scheffler (yagee)
 */
public class MCRStalledJobResetter implements Runnable {
    private static MCRStalledJobResetter instance = new MCRStalledJobResetter();

    private static int maxTimeDiff = Integer.parseInt(MCRIView2Tools.getIView2Property("TimeTillReset"));

    private static Logger LOGGER = LogManager.getLogger(MCRStalledJobResetter.class);

    private static int maxResetCount = Integer.parseInt(MCRIView2Tools.getIView2Property("MaxResetCount"));

    private HashMap<Long, Integer> jobCounter;

    private MCRStalledJobResetter() {
        jobCounter = new HashMap<>();
    }

    public static MCRStalledJobResetter getInstance() {
        return instance;
    }

    /**
     * Resets jobs to {@link MCRJobState#NEW} that where in status {@link MCRJobState#PROCESSING} for to long time.
     */
    public void run() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction executorTransaction = em.getTransaction();
        LOGGER.info("MCRTileJob is Checked for dead Entries");
        executorTransaction.begin();

        TypedQuery<MCRTileJob> query = em.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.PROCESSING.toChar()
            + "' ORDER BY id ASC", MCRTileJob.class);
        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        boolean reset = query
            .getResultList()
            .stream()
            .map(job -> {
                long start = job.getStart().getTime() / 60000;
                boolean ret = false;
                LOGGER.debug("checking {} {} ...", job.getDerivate(), job.getPath());
                if (current - start >= maxTimeDiff) {
                    if (hasPermanentError(job)) {
                        LOGGER.warn("Job has permanent errors: {}", job);
                        job.setStatus(MCRJobState.ERROR);
                        jobCounter.remove(job.getId());
                    } else {
                        LOGGER.debug("->Resetting too long in queue");
                        job.setStatus(MCRJobState.NEW);
                        job.setStart(null);
                        ret = true;
                    }
                } else {
                    LOGGER.debug("->ok");
                }
                return ret;
            })
            .reduce(Boolean::logicalOr)
            .orElse(false);

        try {
            executorTransaction.commit();
        } catch (HibernateException e) {
            LOGGER.error(e);
            if (executorTransaction != null) {
                executorTransaction.rollback();
                reset = false;//No changes are applied, so no notification is needed as well
            }
        }
        //Only notify Listeners on TilingQueue if really something is set back
        if (reset) {
            synchronized (MCRTilingQueue.getInstance()) {
                MCRTilingQueue.getInstance().notifyListener();
            }
        }
        em.close();
        LOGGER.info("MCRTileJob checking is done");
    }

    private boolean hasPermanentError(MCRTileJob job) {
        int runs = 0;
        if (jobCounter.containsKey(job.getId())) {
            runs = jobCounter.get(job.getId());
        }
        if (++runs >= maxResetCount) {
            return true;
        }
        jobCounter.put(job.getId(), runs);
        return false;
    }
}
