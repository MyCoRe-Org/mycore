package org.mycore.services.queuedjob;

import org.mycore.common.config.MCRConfiguration2;

import java.util.List;
import java.util.Optional;

public interface MCRJobConfig {
    Optional<Integer> timeTillReset(Class<? extends MCRJobAction> action);
    Optional<Integer> maxTryCount(Class<? extends MCRJobAction> action);

    /**
     * Retrieves the job thread count configured for this specific Manager, using {@link MCRConfiguration2}
     *
     * @return the optional count
     */
    Optional<Integer> maxJobThreadCount(Class<? extends MCRJobAction> action);
    Optional<Boolean> autostart(Class<? extends MCRJobAction> action);
    Optional<Boolean> activated(Class<? extends MCRJobAction> action);

    /**
     * Retrieves the default job thread count configured for all job threads, using {@link MCRConfiguration2}
     *
     * @return the count
     */
    Integer maxJobThreadCount();
    Integer timeTillReset();
    Integer maxTryCount();
    Boolean autostart();
    Boolean activated();

    List<MCRJobStatusListener> jobStatusListeners(Class<? extends MCRJobAction> action);
}
