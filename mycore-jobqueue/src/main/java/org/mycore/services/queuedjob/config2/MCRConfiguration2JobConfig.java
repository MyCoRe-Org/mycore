package org.mycore.services.queuedjob.config2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.queuedjob.MCRJobConfig;
import org.mycore.services.queuedjob.MCRJobStatusListener;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MCRConfiguration2JobConfig implements MCRJobConfig {

    public static final String CONFIG_MAX_TRY = "MaxTry";

    public static final String CONFIG_TIME_TILL_RESET = "TimeTillReset";

    public static final String CONFIG_JOB_THREADS = "JobThreads";

    public static final String CONFIG_AUTOSTART = "autostart";
    private static final String CONFIG_ACTIVATED = "activated";

    protected static String CONFIG_PREFIX = "MCR.QueuedJob.";

    private static final Logger LOGGER = LogManager.getLogger();

    private static String getActionConfigPrefix(Class<? extends MCRJobAction> action) {
        return CONFIG_PREFIX + action.getSimpleName() + ".";
    }

    @Override
    public Optional<Integer> timeTillReset(Class<? extends MCRJobAction> action) {
        return MCRConfiguration2.getInt(getActionConfigPrefix(action) + CONFIG_TIME_TILL_RESET);
    }

    @Override
    public Optional<Integer> maxTryCount(Class<? extends MCRJobAction> action)  {
        return MCRConfiguration2.getInt(getActionConfigPrefix(action) + CONFIG_MAX_TRY);
    }

    @Override
    public Optional<Integer> maxJobThreadCount(Class<? extends MCRJobAction> action) {
        return MCRConfiguration2.getInt(getActionConfigPrefix(action) + CONFIG_JOB_THREADS);
    }

    @Override
    public Optional<Boolean> autostart(Class<? extends MCRJobAction> action) {
        return MCRConfiguration2.getBoolean(CONFIG_PREFIX + CONFIG_AUTOSTART);
    }

    @Override
    public Optional<Boolean> activated(Class<? extends MCRJobAction> action){
        return MCRConfiguration2.getBoolean(getActionConfigPrefix(action) + CONFIG_ACTIVATED);
    }

    @Override
    public Integer maxJobThreadCount() {
        String property = CONFIG_PREFIX + CONFIG_JOB_THREADS;
        return MCRConfiguration2.getInt(property)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }
    @Override
    public Integer timeTillReset()  {
        String property = CONFIG_PREFIX + CONFIG_TIME_TILL_RESET;
        return MCRConfiguration2.getInt(property)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }

    @Override
    public Integer maxTryCount() {
        String property = CONFIG_PREFIX + CONFIG_MAX_TRY;
        return MCRConfiguration2.getInt(property)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }

    @Override
    public Boolean autostart() {
        String property = MCRConfiguration2JobConfig.CONFIG_PREFIX + MCRConfiguration2JobConfig.CONFIG_AUTOSTART;
        return MCRConfiguration2.getBoolean(property)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }

    @Override
    public Boolean activated() {
        String property = MCRConfiguration2JobConfig.CONFIG_PREFIX + MCRConfiguration2JobConfig.CONFIG_ACTIVATED;
        return MCRConfiguration2.getBoolean(property)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));    }

    @Override
    public List<MCRJobStatusListener> jobStatusListeners(Class<? extends MCRJobAction> action) {
        return MCRConfiguration2.getString("MCR.QueuedJob." + action.getSimpleName() + ".Listeners")
            .stream()
            .flatMap(MCRConfiguration2::splitValue)
            .map(className -> {
                try {
                    return (MCRJobStatusListener) MCRClassTools.forName(className)
                        .getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    LOGGER.error("Could not load class {}", className, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
