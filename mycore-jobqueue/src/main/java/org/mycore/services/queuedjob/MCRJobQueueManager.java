package org.mycore.services.queuedjob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MCRJobQueueManager {

    private static final Logger LOGGER = LogManager.getLogger();

    final Map<String, MCRJobQueue> queueInstances = new ConcurrentHashMap<>();
    final Map<String, MCRJobThreadStarter> jobThreadStartInstances = new ConcurrentHashMap<>();
    private MCRJobDAO dao;
    private MCRJobConfig config;

    MCRJobQueueManager(MCRJobDAO dao, MCRJobConfig config) {
        this.dao = dao;
        this.config = config;
    }

    public static MCRJobQueueManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static boolean isJPAEnabled() {
        return MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)
            && MCREntityManagerProvider.getEntityManagerFactory() != null;
    }

    private void initializeAction(Class<? extends MCRJobAction> action) {
        String key = action.getName();
        MCRJobConfig config = getJobConfig();

        MCRJobQueue queue = queueInstances.computeIfAbsent(key, k -> {
            MCRJobQueue queue1 = new MCRJobQueue(action, config, getJobDAO());
            long count
                = MCRJobResetter.resetJobsWithAction(action, config, getJobDAO(), queue1, MCRJobStatus.PROCESSING);
            if (count > 0) {
                LOGGER.info("Resetted {} processing jobs for action {} on startup!", count, action.getName());
            }
            return queue1;
        });

        boolean jpaEnabled = isJPAEnabled();
        if (jpaEnabled) {
            jobThreadStartInstances.computeIfAbsent(key, k -> {
                MCRJobThreadStarter starter = new MCRJobThreadStarter(action, this.config, queue);
                Thread jobThreadStarterThread = new Thread(starter);
                jobThreadStarterThread.start();
                return starter;
            });
        }

        queue.setRunning(jpaEnabled);

    }

    /**
     * Returns a singleton instance of this class.
     *
     * @param action the {@link MCRJobAction} or <code>null</code>
     * @return the instance of this class
     */
    public MCRJobThreadStarter getJobThreadStarter(Class<? extends MCRJobAction> action) {
        initializeAction(action);
        String key = action.getName();
        return jobThreadStartInstances.get(key);
    }

    /**
     * Returns a singleton instance of {@link MCRJobQueue} for the given {@link MCRJobAction}.
     *
     * @param action the {@link MCRJobAction} or <code>null</code>
     * @return singleton instance of this class
     */
    public MCRJobQueue getJobQueue(Class<? extends MCRJobAction> action) {
        initializeAction(action);
        String key = action.getName();
        return queueInstances.get(key);
    }

    public List<MCRJobQueue> getJobQueues() {
        return new ArrayList<>(queueInstances.values());
    }

    /**
     * @return the {@link MCRJobDAO} instance
     */
    public MCRJobDAO getJobDAO() {
        return dao;
    }

    /**
     * @return the {@link MCRJobConfig} instance
     */
    public MCRJobConfig getJobConfig() {
        return config;
    }

    private static class InstanceHolder {
        private static final MCRJobQueueManager INSTANCE
            = new MCRJobQueueManager(new MCRJobDAOJPAImpl(), new MCRConfiguration2JobConfig());
    }

}
