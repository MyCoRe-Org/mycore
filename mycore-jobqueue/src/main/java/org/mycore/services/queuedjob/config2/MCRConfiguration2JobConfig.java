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

package org.mycore.services.queuedjob.config2;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.queuedjob.MCRJobConfig;
import org.mycore.services.queuedjob.MCRJobStatusListener;

/**
 * Reads the configuration for the job queue from mycore.properties using the {@link MCRConfiguration2} class.
 * @author Sebastian Hofmann
 */
public class MCRConfiguration2JobConfig implements MCRJobConfig {

    private static final String CONFIG_MAX_TRY = "MaxTry";

    private static final String CONFIG_TIME_TILL_RESET = "TimeTillReset";

    private static final String CONFIG_JOB_THREADS = "JobThreads";

    private static final String CONFIG_ACTIVATED = "activated";

    private static final String CONFIG_PREFIX = "MCR.QueuedJob.";

    private static final Logger LOGGER = LogManager.getLogger();

    private static String getActionConfigPrefix(Class<? extends MCRJobAction> action) {
        return CONFIG_PREFIX + action.getSimpleName() + ".";
    }

    @Override
    public Optional<Duration> timeTillReset(Class<? extends MCRJobAction> action) {
        return MCRConfiguration2.getInt(getActionConfigPrefix(action) + CONFIG_TIME_TILL_RESET)
                .map(Duration::ofMinutes);
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
    public Duration timeTillReset()  {
        String property = CONFIG_PREFIX + CONFIG_TIME_TILL_RESET;
        return MCRConfiguration2.getInt(property)
                .map(Duration::ofMinutes)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }

    @Override
    public Integer maxTryCount() {
        String property = CONFIG_PREFIX + CONFIG_MAX_TRY;
        return MCRConfiguration2.getInt(property)
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
