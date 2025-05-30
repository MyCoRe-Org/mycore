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

import static org.mycore.common.MCRUtils.enumSetOf;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A {@link MCRSimpleJobSelector} is a {@link MCRJobSelector} that combines multiple simple
 * but commonly required selection criteria.
 * <ul>
 * <li> Jobs that have an action that is one of or none of a given set of actions.
 * <li> Jobs that have a status that is one of or none of a given set of statuses.
 * <li> Jobs that were added at least a given number of days ago.
 * </ul>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRSimpleJobSelector#ACTIONS_KEY} can be used to
 * specify the set of fully qualified action class names to be considered, as a comma-separated list.
 * <li> The property suffix {@link MCRSimpleJobSelector#ACTION_MODE_KEY} can be used to
 * specify the set {@link Mode} to be used in conjunction with the set of action class names.
 * <li> The property suffix {@link MCRSimpleJobSelector#STATUSES_KEY} can be used to
 * specify the set of {@link MCRJobStatus} names to be considered, as a comma-separated list.
 * <li> The property suffix {@link MCRSimpleJobSelector#STATUS_MODE_KEY} can be used to
 * specify the set {@link Mode} to be used in conjunction with the set of statuses.
 * <li> The property suffix {@link MCRSimpleJobSelector#AGE_DAYS_KEY} can be used to
 * specify the number of days to be used (where `0` disables the criteria).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.services.queuedjob.MCRSimpleJobSelector
 * [...].Actions=foo.bar.FooAction,foo.bar.BarAction
 * [...].ActionMode=INCLUDE
 * [...].Statuses=NEW,PROCESSING
 * [...].StatusMode=EXCLUDE
 * [...].AgeDays=42
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRSimpleJobSelector.Factory.class)
public final class MCRSimpleJobSelector implements MCRJobSelector {

    public static final String ACTIONS_KEY = "Actions";

    public static final String ACTION_MODE_KEY = "ActionMode";

    public static final String STATUSES_KEY = "Statuses";

    public static final String STATUS_MODE_KEY = "StatusMode";

    public static final String AGE_DAYS_KEY = "AgeDays";

    private final Set<Class<? extends MCRJobAction>> actions;

    private final Mode actionMode;

    private final Set<MCRJobStatus> statuses;

    private final Mode statusMode;

    private final int ageDays;

    public MCRSimpleJobSelector(Collection<Class<? extends MCRJobAction>> actions, Mode actionMode,
        Collection<MCRJobStatus> statuses, Mode statusMode, int ageDays) {
        this.actions = new HashSet<>(Objects.requireNonNull(actions, "Actions must not be null"));
        this.actions.forEach(obj -> Objects.requireNonNull(obj, "Action must not be null"));
        this.actionMode = Objects.requireNonNull(actionMode, "Action mode must not be null");
        this.statuses = enumSetOf(MCRJobStatus.class, Objects.requireNonNull(statuses, "Statuses must not be null"));
        this.statuses.forEach(obj -> Objects.requireNonNull(obj, "Status must not be null"));
        this.statusMode = Objects.requireNonNull(statusMode, "Status mode must not be null");
        if (ageDays < 0) {
            throw new IllegalArgumentException("Age [days] must not be negative");
        }
        this.ageDays = ageDays;
    }

    @Override
    public List<Predicate> getPredicates(CriteriaBuilder builder, Root<MCRJob> jobs) {

        List<Predicate> predicates = new LinkedList<>();

        if (!actions.isEmpty()) {
            Predicate predicate = jobs.get(MCRJob_.action).in(actions);
            if (actionMode == Mode.EXCLUDE) {
                predicate = builder.not(predicate);
            }
            predicates.add(predicate);
        } else if (actionMode == Mode.EXCLUDE) {
            predicates.add(builder.conjunction());
        }

        if (!statuses.isEmpty()) {
            Predicate predicate = jobs.get(MCRJob_.status).in(statuses);
            if (statusMode == Mode.EXCLUDE) {
                predicate = builder.not(predicate);
            }
            predicates.add(predicate);
        } else if (statusMode == Mode.EXCLUDE) {
            predicates.add(builder.conjunction());
        }

        if (ageDays != 0) {
            Date maximumAddedDate = Date.from(Instant.now().minus(ageDays, ChronoUnit.DAYS));
            predicates.add(builder.lessThan(jobs.get(MCRJob_.added), maximumAddedDate));
        }

        return predicates;

    }

    public enum Mode {

        INCLUDE,

        EXCLUDE;

    }

    public static class Factory implements Supplier<MCRSimpleJobSelector> {

        @MCRProperty(name = ACTIONS_KEY, defaultName = "MCR.QueuedJob.Selectors.Default.Actions")
        public String actions;

        @MCRProperty(name = ACTION_MODE_KEY, defaultName = "MCR.QueuedJob.Selectors.Default.ActionMode")
        public String actionMode;

        @MCRProperty(name = STATUSES_KEY, defaultName = "MCR.QueuedJob.Selectors.Default.Statuses")
        public String statuses;

        @MCRProperty(name = STATUS_MODE_KEY, defaultName = "MCR.QueuedJob.Selectors.Default.StatusMode")
        public String statusMode;

        @MCRProperty(name = AGE_DAYS_KEY, defaultName = "MCR.QueuedJob.Selectors.Default.AgeDays")
        public String ageDays;

        private String property;

        @MCRPostConstruction(MCRPostConstruction.Value.CANONICAL)
        public void init(String property) {
            this.property = property;
        }

        @Override
        public MCRSimpleJobSelector get() {

            Set<Class<? extends MCRJobAction>> actions = MCRConfiguration2.splitValue(this.actions)
                .map(this::toActionJobClass).collect(Collectors.toSet());
            Mode actionMode = Mode.valueOf(this.actionMode);

            Set<MCRJobStatus> statuses = MCRConfiguration2.splitValue(this.statuses)
                .map(MCRJobStatus::valueOf).collect(Collectors.toSet());
            Mode statusMode = Mode.valueOf(this.statusMode);

            int ageDays = Integer.parseInt(this.ageDays);

            return new MCRSimpleJobSelector(actions, actionMode, statuses, statusMode, ageDays);

        }

        private Class<? extends MCRJobAction> toActionJobClass(String action) {
            try {
                return MCRClassTools.forName(action);
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException("Missing class (" + action + ") configured in property: " +
                    property + "." + ACTIONS_KEY, e);
            }
        }

    }

}
