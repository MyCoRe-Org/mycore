package org.mycore.services.queuedjob;

import jakarta.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

import java.util.List;

@MCRCommandGroup(name = "Job Queue Commands")
public class MCRJobQueueCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    static MCRJobDAOJPAImpl dao = new MCRJobDAOJPAImpl();

    @MCRCommand(
        syntax = "start test jobs {0}",
        help = "Starts {0} test jobs",
        order = 0)
    public static void startTestJobs(String number) {
        int i = Integer.parseInt(number);
        for (int j = 0; j < i; j++) {
            MCRJob job = new MCRJob();
            job.setAction(MCRTestJobAction.class);
            job.setParameter("count", String.valueOf(j));
            MCRJobQueueManager.getInstance()
                .getJobQueue(MCRTestJobAction.class)
                .offer(job);
        }
    }

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

    @MCRCommand(
        syntax = "reset max try jobs",
        help = "Reset all jobs with status MAX_TRIES",
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

    @MCRCommand(
        syntax = "reset max try jobs with action {0}",
        help = "Reset all jobs with status MAX_TRIES and action {0}",
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

    @MCRCommand(
        syntax = "reset max try jobs with id {0}",
        help = "reset all jobs with status MAX_TRIES and id {0}",
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
