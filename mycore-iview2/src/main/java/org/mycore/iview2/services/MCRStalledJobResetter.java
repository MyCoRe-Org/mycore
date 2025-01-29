/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

/**
 * Resets jobs that took to long to tile.
 * Set property <code>MCR.Module-iview2.TimeTillReset</code> to alter grace period.
 * Set property <code>MCR.Module-iview2.MaxResetCount</code> to alter maximum tries per job.
 * @author Thomas Scheffler (yagee)
 */
public final class MCRStalledJobResetter implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile MCRStalledJobResetter instance;

    private static final int MAX_TIME_DIFF = Integer.parseInt(MCRIView2Tools.getIView2Property("TimeTillReset"));

    private static final int MAX_RESET_COUNT = Integer.parseInt(MCRIView2Tools.getIView2Property("MaxResetCount"));

    private final Map<Long, Integer> jobCounter;

    private MCRStalledJobResetter() {
        jobCounter = new HashMap<>();
    }

    public static MCRStalledJobResetter getInstance() {
        if(instance == null) {
            synchronized (MCRStalledJobResetter.class) {
                if(instance == null) {
                    instance = new MCRStalledJobResetter();
                }
            }
        }
        return instance;
    }

    /**
     * Resets jobs to {@link MCRJobState#NEW} that where in status {@link MCRJobState#PROCESSING} for to long time.
     */
    @Override
    public void run() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction executorTransaction = em.getTransaction();
        LOGGER.info("MCRTileJob is Checked for dead Entries");
        executorTransaction.begin();

        TypedQuery<MCRTileJob> query = em.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.PROCESSING.toChar()
            + "' ORDER BY id ASC", MCRTileJob.class);
        long current = new Date(System.currentTimeMillis()).getTime() / 60_000;

        boolean reset = query
            .getResultList()
            .stream()
            .map(job -> {
                long start = job.getStart().getTime() / 60_000;
                boolean ret = false;
                LOGGER.debug("checking {} {} …", job::getDerivate, job::getPath);
                if (current - start >= MAX_TIME_DIFF) {
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
        } catch (PersistenceException e) {
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
        if (++runs >= MAX_RESET_COUNT) {
            return true;
        }
        jobCounter.put(job.getId(), runs);
        return false;
    }
}
