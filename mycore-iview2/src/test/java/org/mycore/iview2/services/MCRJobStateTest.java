/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
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
