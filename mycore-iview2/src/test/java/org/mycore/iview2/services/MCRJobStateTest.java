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

package org.mycore.iview2.services;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class MCRJobStateTest {

    @Test
    public final void testJobStates() {

        Set<MCRJobState> allStates = Stream.of(MCRJobState.values()).collect(Collectors.toSet());
        Set<MCRJobState> notCompleteStates = MCRJobState.notCompleteStates();
        Set<MCRJobState> completeStates = MCRJobState.completeStates();

        boolean notCompleteContainsComplete = notCompleteStates.stream().filter(completeStates::contains).findAny()
            .isPresent();

        boolean completeContainsNotComplete = completeStates.stream().filter(notCompleteStates::contains).findAny()
            .isPresent();

        boolean allStatesPresent = Stream.concat(notCompleteStates.stream(), completeStates.stream()).distinct()
            .count() == allStates.size();

        Assert.assertFalse("There is a element in MCRJobState.COMPLETE_STATES and MCRJobState.NOT_COMPLETE_STATES",
            notCompleteContainsComplete || completeContainsNotComplete);

        Assert.assertTrue(
            "Not every JobState is present in MCRJobState.NOT_COMPLETE_STATES or MCRJobState.COMPLETE_STATES",
            allStatesPresent);
    }

}
