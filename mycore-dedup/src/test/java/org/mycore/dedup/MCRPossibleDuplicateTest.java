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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MCRPossibleDuplicateTest {

    @Test
    public void nullCriterionIsRejected() {
        LinkedHashSet<MCRDeDupCriterion> criteria = new LinkedHashSet<>(
            Arrays.asList(new MCRDeDupCriterion("identifier", "doi:10.1000/xyz"), null));

        assertThrows(NullPointerException.class,
            () -> new MCRPossibleDuplicate("mcr_test_00000001", "mcr_test_00000002", criteria));
    }

    @Test
    public void objectIdsAreSwappedIntoNormalizedOrder() {
        LinkedHashSet<MCRDeDupCriterion> criteria = new LinkedHashSet<>(
            List.of(new MCRDeDupCriterion("identifier", "doi:10.1000/xyz")));

        MCRPossibleDuplicate swapped = new MCRPossibleDuplicate("mcr_test_00000002", "mcr_test_00000001", criteria);

        assertEquals("mcr_test_00000001", swapped.objectId1());
        assertEquals("mcr_test_00000002", swapped.objectId2());
        assertEquals(new MCRPossibleDuplicate("mcr_test_00000001", "mcr_test_00000002", criteria), swapped);
    }
}
