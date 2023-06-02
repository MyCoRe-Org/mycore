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

package org.mycore.services.queuedjob;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

import jakarta.persistence.EntityManager;

/**
 * Provides commands to manage the job queue.
 * @author Sebastian Hofmann
 */
@MCRCommandGroup(name = "Job Queue Commands")
public class MCRJobQueueCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    static MCRJobDAOJPAImpl dao = new MCRJobDAOJPAImpl();


    /**
     * Lists all jobs with status MAX_TRIES.
     */
    @MCRCommand(
        syntax = "list max try jobs",
        help = "List all jobs with status MAX_TRIES",
        order = 10)
    public static void listMaxTryJobs() {
        List<MCRJob> jobs = dao.getJobs(null, null, List.of(MCRJobStatus.MAX_TRIES), null, null);

        jobs.forEach(job -> {
            LOGGER.info("{}: {} {} {} {} {}", job.getId(), job.getAction(), job.getStatus(), job.getTries(),
                job.getAdded(), job.getStart());
            job.getParameters().forEach((key, value) -> {
                LOGGER.info("{}: {}={}", job.getId(), key, value);
            });
            LOGGER.info("Failed with Exception {}", job.getException());
        });
    }

    /**
     * Resets all jobs with status MAX_TRIES to status NEW.
     */
    @MCRCommand(
        syntax = "reset max try jobs",
        help = "Reset all jobs with status MAX_TRIES to status NEW",
        order = 20)
    public static void resetMaxTryJobs() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        int count = em
            .createQuery(
                "UPDATE MCRJob SET status = :status, tries = 0, exception = null WHERE status = :maxTriesStatus")
            .setParameter("status", MCRJobStatus.NEW)
            .setParameter("maxTriesStatus", MCRJobStatus.MAX_TRIES)
            .executeUpdate();

        LOGGER.info("Reset {} jobs", count);
    }

    /**
     * Resets all jobs with status MAX_TRIES and action {0} to status NEW.
     * @param action the action to reset
     */
    @MCRCommand(
        syntax = "reset max try jobs with action {0}",
        help = "Reset all jobs with status MAX_TRIES and action {0} to status NEW",
        order = 30)
    public static void resetMaxTryJobsWithAction(String action) {
        if(action == null || action.isEmpty()) {
            LOGGER.error("Action is required!");
            return;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        int count = em.createQuery(
            "UPDATE MCRJob SET status = :status, tries = 0, exception = null WHERE status = :maxTriesStatus " +
                    "AND action = :action")
            .setParameter("status", MCRJobStatus.NEW)
            .setParameter("maxTriesStatus", MCRJobStatus.MAX_TRIES)
            .setParameter("action", action)
            .executeUpdate();

        LOGGER.info("Reset {} jobs", count);
    }

    /**
     * Resets all jobs with status MAX_TRIES and id {0} to status NEW.
     * @param id the id to reset
     */
    @MCRCommand(
        syntax = "reset max try jobs with id {0}",
        help = "reset all jobs with status MAX_TRIES and id {0} to status NEW",
        order = 40)
    public static void resetMaxTryJobsWithId(String id) {
        if(id == null || id.isEmpty()) {
            LOGGER.error("Id is required!");
            return;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        int count = em.createQuery(
            "UPDATE MCRJob SET status = :status, tries = 0, exception = null WHERE status = :maxTriesStatus " +
                    "AND id = :id")
            .setParameter("status", MCRJobStatus.NEW)
            .setParameter("maxTriesStatus", MCRJobStatus.MAX_TRIES)
            .setParameter("id", id)
            .executeUpdate();

        LOGGER.info("Reset {} jobs", count);
    }

}
