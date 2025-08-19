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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.services.queuedjob.action.MCRTestJobAction1;
import org.mycore.services.queuedjob.action.MCRTestJobAction2;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRJobResetterTest {

    @Test
    public void testResetJobsWithAction() {
        MCRMockJobDAO mockDAO = new MCRMockJobDAO();

        // save reset jobs to this queues
        Queue<MCRJob> reset1 = new ArrayDeque<>();
        Queue<MCRJob> reset2 = new ArrayDeque<>();

        MCRConfiguration2JobConfig config = new MCRConfiguration2JobConfig();

        // create resetter with mockDAO and reset queues
        MCRJobResetter resetter = new MCRJobResetter(mockDAO, (action) -> {
            switch (action.getSimpleName()) {
                case "MCRTestJobAction1" -> {
                    return reset1;
                }
                case "MCRTestJobAction2" -> {
                    return reset2;
                }
                default -> {
                    return null;
                }
            }
        }, config);

        long elevenMinutesAgo = new Date().getTime() - 60 * 1000 * 11;
        long nineMinutesAgo = new Date().getTime() - 60 * 1000 * 9;

        MCRJob job = new MCRJob();
        job.setAction(MCRTestJobAction1.class);
        job.setParameter("count", "1");
        job.setStatus(MCRJobStatus.ERROR);
        job.setAdded(new Date(elevenMinutesAgo));
        job.setStart(new Date(elevenMinutesAgo));
        mockDAO.daoOfferedJobs.add(job);

        MCRJob job2 = new MCRJob();
        job2.setAction(MCRTestJobAction2.class);
        job2.setParameter("count", "2");
        job2.setStatus(MCRJobStatus.ERROR);
        job2.setAdded(new Date(elevenMinutesAgo));
        job2.setStart(new Date(elevenMinutesAgo));
        mockDAO.daoOfferedJobs.add(job2);

        MCRJob job3 = new MCRJob();
        job3.setAction(MCRTestJobAction1.class);
        job3.setParameter("count", "3");
        job3.setStatus(MCRJobStatus.ERROR);
        job3.setAdded(new Date(nineMinutesAgo));
        job3.setStart(new Date(nineMinutesAgo));
        mockDAO.daoOfferedJobs.add(job3);

        MCRJob job4 = new MCRJob();
        job4.setAction(MCRTestJobAction2.class);
        job4.setParameter("count", "4");
        job4.setStatus(MCRJobStatus.ERROR);
        job4.setAdded(new Date(nineMinutesAgo));
        job4.setStart(new Date(nineMinutesAgo));
        mockDAO.daoOfferedJobs.add(job4);

        MCRJob job5 = new MCRJob();
        job5.setAction(MCRTestJobAction2.class);
        job5.setParameter("count", "5");
        job5.setStatus(MCRJobStatus.NEW);
        job5.setAdded(new Date(elevenMinutesAgo));
        job5.setStart(new Date(elevenMinutesAgo));
        mockDAO.daoOfferedJobs.add(job5);

        MCRJob job6 = new MCRJob();
        job6.setAction(MCRTestJobAction2.class);
        job6.setParameter("count", "5");
        job6.setStatus(MCRJobStatus.MAX_TRIES);
        job6.setAdded(new Date(elevenMinutesAgo));
        job6.setStart(new Date(elevenMinutesAgo));
        mockDAO.daoOfferedJobs.add(job5);

        assertEquals(6, mockDAO.daoOfferedJobs.size(), "offered jobs should be 6");
        assertEquals(0, reset1.size(), "resetted jobs in queue1 should be 0");
        assertEquals(0, reset2.size(), "resetted jobs in queue2 should be 0");

        resetter.resetJobsWithAction(MCRTestJobAction1.class);
        assertEquals(1, reset1.size(), "resetted jobs in queue1 should be 1");
        assertEquals(0, reset2.size(), "resetted jobs in queue2 should be 0");
        assertEquals("1", reset1.poll().getParameter("count"), "reseted job should have count 1");

        resetter.resetJobsWithAction(MCRTestJobAction2.class);
        assertEquals(0, reset1.size(), "resetted jobs in queue1 should be 0 (poll called)");
        assertEquals(1, reset2.size(), "resetted jobs in queue2 should be 1");
        assertEquals("2", reset2.poll().getParameter("count"), "reseted job should have count 2");

    }

}
