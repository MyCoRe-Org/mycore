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

package org.mycore.iview2.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class MCRJobStateTest {

    @Test
    public void testJobStates() {

        Set<MCRJobState> allStates = Stream.of(MCRJobState.values()).collect(Collectors.toSet());
        Set<MCRJobState> notCompleteStates = MCRJobState.notCompleteStates();
        Set<MCRJobState> completeStates = MCRJobState.completeStates();

        boolean notCompleteContainsComplete = notCompleteStates.stream().anyMatch(completeStates::contains);

        boolean completeContainsNotComplete = completeStates.stream().anyMatch(notCompleteStates::contains);

        boolean allStatesPresent = Stream.concat(notCompleteStates.stream(), completeStates.stream()).distinct()
            .count() == allStates.size();

        assertFalse(notCompleteContainsComplete || completeContainsNotComplete,
            "There is a element in MCRJobState.COMPLETE_STATES and MCRJobState.NOT_COMPLETE_STATES");

        assertTrue(allStatesPresent,
            "Not every JobState is present in MCRJobState.NOT_COMPLETE_STATES or MCRJobState.COMPLETE_STATES");
    }

}
