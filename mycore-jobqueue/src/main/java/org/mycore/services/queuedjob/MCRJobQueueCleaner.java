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

package org.mycore.services.queuedjob;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUserInformationResolver;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/// A [MCRJobQueueCleaner] performs bulk removal of persisted [MCRJob] instances. To do so, it uses
/// [MCRJobSelector] instances that each provide selection criteria in form of af a list of [Predicate]
/// instances. Each selector is processes separately. A job has to match all predicates provided by
/// that selector in order to be deleted.
/// 
/// A non-singular, globally available and centrally configured instance can be obtained with
/// [MCRJobQueueCleaner#createInstance()]. This instance should generally be used,
/// although custom instances can be created when necessary.
/// It is configured using the property prefix [MCRJobQueueCleaner#CLEANER_PROPERTY].
///
/// ```properties
/// MCR.QueuedJob.Cleaner.Class=org.mycore.services.queuedjob.MCRJobQueueCleaner
/// ```
/// 
/// The following configuration options are available, if configured automatically:
/// - The configuration suffix [MCRJobQueueCleaner#SELECTORS_KEY] can be used to specify
///   the map of selectors to be used.
/// - For each selector, the configuration suffix [MCRSentinel#ENABLED_KEY] can be used to
///   excluded that selector from the configuration.
/// - The configuration suffix [MCRJobQueueCleaner#ENABLED_KEY] can be used to
///   enable or disable all selectors.
/// 
/// Example:
/// ```properties
/// [...].Class=org.mycore.services.queuedjob.MCRJobQueueCleaner
/// [...].Selectors.foo.Class=foo.bar.FooSelector
/// [...].Selectors.foo.Enabled=true
/// [...].Selectors.foo.Key1=Value1
/// [...].Selectors.foo.Key2=Value2
/// [...].Selectors.bar.Class=foo.bar.BarSelector
/// [...].Selectors.bar.Enabled=false
/// [...].Selectors.bar.Key1=Value1
/// [...].Selectors.bar.Key2=Value2
/// [...].Enabled=true
/// ```
@MCRConfigurationProxy(proxyClass = MCRJobQueueCleaner.Factory.class)
public final class MCRJobQueueCleaner {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CLEANER_PROPERTY = "MCR.QueuedJob.Cleaner";

    public static final String SELECTORS_KEY = "Selectors";

    public static final String ENABLED_KEY = "Enabled";

    private final Map<String, MCRJobSelector> selectors;

    private final boolean enabled;

    public MCRJobQueueCleaner(Map<String, MCRJobSelector> selectors, boolean enabled) {

        this.selectors = new HashMap<>(Objects.requireNonNull(selectors, "Selectors must not be null"));
        this.selectors.forEach((name, selector) ->
            Objects.requireNonNull(selector, "Selector " + name + " must not be null"));
        this.enabled = enabled;

        LOGGER.info(() -> "Working with selectors: " + String.join(", ", selectors.keySet()));

    }

    /**
     * @deprecated Use {@link #createInstance()} instead
     */
    @Deprecated
    public static MCRJobQueueCleaner instantiate() {
        return createInstance();
    }

    public static MCRJobQueueCleaner createInstance() {
        String classProperty = CLEANER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRJobQueueCleaner.class, classProperty);
    }

    /**
     * Remove jobs from the job queue using all selectors.
     *
     * @return The total amount of removed jobs.
     */
    public int clean() {
        return doClean(null);
    }

    /**
     * Remove jobs from the job queue using a specific selectors.
     *
     * @param selectorName The name of the selector to be used.
     * @return The total amount of removed jobs.
     */
    public int clean(String selectorName) {

        if (!selectors.containsKey(selectorName)) {
            throw new MCRUsageException("Selector " + selectorName + " unavailable, got " +
                String.join(", ", this.selectors.keySet()));
        }

        return doClean(selectorName);

    }

    private int doClean(String selectorName) {

        if (!enabled) {
            LOGGER.info("Aborting, because cleaner is not enabled");
            return 0;
        }

        if (!MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)) {
            LOGGER.info("Aborting, because database is not enabled");
            return 0;
        }

        if (MCREntityManagerProvider.getEntityManagerFactory() == null) {
            LOGGER.info("Aborting, because database is not available");
            return 0;
        }

        return doClean(MCREntityManagerProvider.getCurrentEntityManager(), selectorName);

    }

    private int doClean(EntityManager manager, String selectorName) {

        int numberOfJobs = 0;
        for (Map.Entry<String, MCRJobSelector> entry : selectors.entrySet()) {
            MCRJobSelector selector = entry.getValue();
            String name = entry.getKey();
            if (selectorName == null || selectorName.equals(name)) {
                numberOfJobs += doClean(manager, name, selector);
            }
        }

        return numberOfJobs;

    }

    private int doClean(EntityManager manager, String name, MCRJobSelector selector) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaDelete<MCRJob> query = builder.createCriteriaDelete(MCRJob.class);
        Root<MCRJob> jobs = query.from(MCRJob.class);

        Predicate[] predicates = selector.getPredicates(builder, jobs).toArray(Predicate[]::new);

        if (predicates.length == 0) {
            LOGGER.info(() -> "Skipping selector " + name + ", because selector provided no predicates");
            return 0;
        }

        int numberOfJobs = manager.createQuery(query.where(predicates)).executeUpdate();
        LOGGER.info(() -> "Performed selector " + name + ", deleted " + numberOfJobs + " jobs");

        return numberOfJobs;

    }

    public static class Factory implements Supplier<MCRJobQueueCleaner> {

        @MCRInstanceMap(name = SELECTORS_KEY, valueClass = MCRJobSelector.class, sentinel = @MCRSentinel)
        public Map<String, MCRJobSelector> selectors;

        @MCRProperty(name = ENABLED_KEY)
        public String enabled;

        @Override
        public MCRJobQueueCleaner get() {
            return new MCRJobQueueCleaner(selectors, Boolean.parseBoolean(enabled));
        }

    }

}
