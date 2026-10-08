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

import java.util.Set;

import org.mycore.datamodel.metadata.MCRObject;

/**
 * Builds a set of {@link MCRDeDupCriterion} from the metadata of an {@link MCRObject}.
 * <p>
 * Implementations extract one specific kind of deduplication criterion (e.g. identifiers or a
 * combined title/author criterion) from objects of a metadata model they understand. Which builders
 * are applied to which object type is configured via properties of the form
 * {@code MCR.DeDup.CriterionBuilder.<typeId>.<name>.Class}, see {@link MCRDeDupCriteriaProvider}.
 * <p>
 * Builders must be robust against objects they cannot handle and return an empty set in that case
 * instead of throwing an exception.
 */
public interface MCRDeDupCriterionBuilder {

    /**
     * Builds the deduplication criteria contributed by this builder for the given object.
     *
     * @param object the object to build criteria for
     * @return the criteria extracted from the object, or an empty set if none apply
     */
    Set<MCRDeDupCriterion> build(MCRObject object);
}
