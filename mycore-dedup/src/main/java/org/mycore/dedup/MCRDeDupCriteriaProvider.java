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

package org.mycore.dedup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Aggregates the {@link MCRDeDupCriterion}s of an {@link MCRObject} by dispatching to the
 * {@link MCRDeDupCriterionBuilder}s that are configured for the object's type.
 * <p>
 * For an object of type {@code <typeId>} all builders configured via properties of the form
 * <pre>
 * MCR.DeDup.CriterionBuilder.&lt;typeId&gt;.&lt;name&gt;.Class=org.example.MyCriterionBuilder
 * </pre>
 * are instantiated and their resulting criteria are merged. This makes the set of deduplication
 * criteria configurable per object type: a metadata model may contribute several builders and an
 * application can enable or disable each of them individually.
 * <p>
 * The provider itself is configurable through {@code MCR.DeDup.CriteriaProvider.Class} so that
 * applications may replace the default aggregation strategy.
 */
public class MCRDeDupCriteriaProvider {

    /**
     * Prefix of the properties configuring the {@link MCRDeDupCriterionBuilder}s per object type.
     * The object type id and a builder name are appended, followed by {@code .Class}.
     */
    public static final String BUILDER_PROPERTY_PREFIX = "MCR.DeDup.CriterionBuilder.";

    /**
     * @return the configured instance of the deduplication criteria provider
     */
    public static MCRDeDupCriteriaProvider obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRDeDupCriteriaProvider.class,
            "MCR.DeDup.CriteriaProvider.Class");
    }

    /**
     * Builds all deduplication criteria for the given object by merging the criteria of every
     * {@link MCRDeDupCriterionBuilder} configured for the object's type.
     *
     * @param object the object to build criteria for
     * @return the merged set of deduplication criteria, possibly empty
     */
    public Set<MCRDeDupCriterion> getCriteria(MCRObject object) {
        return getCriterionBuilders(object.getId().getTypeId()).stream()
            .flatMap(builder -> builder.build(object).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the {@link MCRDeDupCriterionBuilder}s configured for the given object type.
     *
     * @param typeId the object type id, e.g. {@code mods}
     * @return the configured criterion builders, in no particular order
     */
    public List<MCRDeDupCriterionBuilder> getCriterionBuilders(String typeId) {
        String prefix = BUILDER_PROPERTY_PREFIX + typeId + '.';
        return MCRConfiguration2.getInstances(MCRDeDupCriterionBuilder.class, prefix).values().stream()
            .map(MCRDeDupCriteriaProvider::call)
            .toList();
    }

    private static MCRDeDupCriterionBuilder call(Callable<MCRDeDupCriterionBuilder> supplier) {
        try {
            return supplier.call();
        } catch (Exception e) {
            throw new MCRException("Could not instantiate configured deduplication criterion builder", e);
        }
    }
}
