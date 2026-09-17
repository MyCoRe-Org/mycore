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
 * Test criterion builder that returns a single, constant criterion for every object, so that any two
 * objects it is applied to are reported as possible duplicates. Used to test the wiring of
 * {@link MCRDeDupCriteriaProvider} and {@link MCRDeDupEventHandler}.
 */
public class MCRDeDupTestCriterionBuilder implements MCRDeDupCriterionBuilder {

    public static final MCRDeDupCriterion CRITERION = new MCRDeDupCriterion("test", "shared");

    @Override
    public Set<MCRDeDupCriterion> build(MCRObject object) {
        return Set.of(CRITERION);
    }
}
