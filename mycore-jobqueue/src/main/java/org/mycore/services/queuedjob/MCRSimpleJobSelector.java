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

import static org.mycore.common.config.MCRConfiguration2.splitValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A {@link MCRSimpleJobSelector} is a {@link MCRJobSelector} that combines multiple simple but commonly required
 * selection criteria.
 * <p>
 * <ul>
 * <li> It can be used to select jobs that have an action that is one of or none of a given list of actions.
 * <li> It can be used to select jobs that have a status that is one of or none of a given list of statuses.
 * <li> It can be used to select jobs that were added at least a given number of days ago.
 * </ul>
 * All of the above criteria are always considered. To decide whether the action or the state of a job has to be one
 * of or none of a given list, {@link Mode} values are used.
 * <p>
 * If automatically configured:
 * <ul>
 * <li> The list of actions to be considered is configured as a comma separated list using the property suffix
 * {@link MCRSimpleJobSelector#ACTIONS_KEY}.
 * <li> The mode for the list of actions is configured using the property suffix
 * {@link MCRSimpleJobSelector#ACTION_MODE_KEY}.
 * <li> The list of statuses to be considered is configured as a comma separated list using the property suffix
 * {@link MCRSimpleJobSelector#STATUSES_KEY}.
 * <li> The mode for the list of statuses is configured using the property suffix
 * {@link MCRSimpleJobSelector#STATUS_MODE_KEY}.
 * <li> The number of days con be configured using the configuration property
 * {@link MCRSimpleJobSelector#AGE_DAYS_KEY}.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.services.queuedjob.MCRSimpleJobSelector
 * [...].Actions=foo.bar.FooAction,foo.bar.BarAction
 * [...].ActionMode=INCLUDE
 * [...].Statuses=NEW,PROCESSING
 * [...].StatusMode=EXCLUDE
 * [...].AgeDays=42
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRSimpleJobSelector.Factory.class)
public final class MCRSimpleJobSelector implements MCRJobSelector {

    public static final String ACTIONS_KEY = "Actions";

    public static final String ACTION_MODE_KEY = "ActionMode";

    public static final String STATUSES_KEY = "Statuses";

    public static final String STATUS_MODE_KEY = "StatusMode";

    public static final String AGE_DAYS_KEY = "AgeDays";

    private final List<Class<? extends MCRJobAction>> actions;

    private final Mode actionMode;

    private final List<MCRJobStatus> statuses;

    private final Mode statusMode;

    private final int ageDays;

    public MCRSimpleJobSelector(List<Class<? extends MCRJobAction>> actions, Mode actionMode,
        List<MCRJobStatus> statuses, Mode statusMode, int ageDays) {
        this.actions = new ArrayList<>(Objects.requireNonNull(actions, "Actions must not be null"));
        this.actions.forEach(obj -> Objects.requireNonNull(obj, "Action must not be null"));
        this.actionMode = Objects.requireNonNull(actionMode, "Action mode must not be null");
        this.statuses = new ArrayList<>(Objects.requireNonNull(statuses, "Statuses must not be null"));
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

            List<Class<? extends MCRJobAction>> actions = splitValue(this.actions)
                .map(this::toActionJobClass).collect(Collectors.toList());
            Mode actionMode = Mode.valueOf(this.actionMode);

            List<MCRJobStatus> statuses = splitValue(this.statuses)
                .map(MCRJobStatus::valueOf).toList();
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
