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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.services.queuedjob.action.MCRTestJobAction1;
import org.mycore.services.queuedjob.action.MCRTestJobAction2;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRJobQueueTest {

    private MCRMockJobDAO mockDAO;

    @BeforeEach
    public void setUp() {
        mockDAO = new MCRMockJobDAO();
    }

    @Test
    public void offer() throws InterruptedException {
        final List<MCRJob> offeredJobs = new ArrayList<>();
        final List<MCRJob> notifiedJobs = new ArrayList<>();

        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction1.class, new MCRConfiguration2JobConfig(), mockDAO);

        MCRJobQueueEventListener listener = notifiedJobs::add;

        queue.addListener(listener);

        MCRJob job;
        for (int c = 10; c > 0; c--) {
            if (c == 5) {
                queue.removeListener(listener);
            }

            job = new MCRJob();
            job.setParameter("count", Integer.toString(c));
            assertTrue(queue.offer(job), "job should be offered");
            Thread.sleep(2); // sleep to get different timestamps
            offeredJobs.add(job);
        }

        assertEquals(offeredJobs.size(), mockDAO.daoOfferedJobs.size(), "offered jobs should be 10");
        for (int c = 0; c < offeredJobs.size(); c++) {
            assertEquals(offeredJobs.get(c), mockDAO.daoOfferedJobs.get(c), "offered jobs should be equal");
        }

        assertEquals(5, notifiedJobs.size(), "notified jobs should be 5");
        for (int c = 0; c < notifiedJobs.size(); c++) {
            assertEquals(offeredJobs.get(c), notifiedJobs.get(c), "notified jobs should be equal");
        }

        // reading the same jobs should work, but the count needs to stay the same
        for (MCRJob j : offeredJobs) {
            assertTrue(queue.offer(j), "job should not be offered");
        }

        assertEquals(offeredJobs.size(), mockDAO.daoOfferedJobs.size(), "offered jobs should be 10");

        // the offered jobs should have the right action, the right status and a timestamp
        for (MCRJob j : offeredJobs) {
            assertEquals(MCRTestJobAction1.class, j.getAction(), "job action should be MCRTestJobAction");
            assertEquals(MCRJobStatus.NEW, j.getStatus(), "job status should be new");
            assertTrue(j.getAdded().getTime() > 0, "job added timestamp should be set");
        }

        // offer a job with different action should trigger an exception
        job = new MCRJob(MCRTestJobAction2.class);
        job.setParameter("count", Integer.toString(1));
        try {
            queue.offer(job);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void poll() {
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction1.class, new MCRConfiguration2JobConfig(), mockDAO);
        List<MCRJob> offeredJobs = new ArrayList<>();

        MCRJob job;
        for (int i = 0; i < 10; i++) {
            job = new MCRJob();
            job.setId((long) i + 1);
            job.setAction(MCRTestJobAction1.class);
            job.setParameter("count", Integer.toString(i));
            job.setStatus(MCRJobStatus.NEW);
            job.setAdded(new java.util.Date(1000000 - (i * 100000))); // reverse the actual order
            mockDAO.daoOfferedJobs.add(job);
            offeredJobs.add(job);
        }

        // poll all jobs
        List<MCRJob> polledJobs = new ArrayList<>();
        MCRJob polledJob;
        while ((polledJob = queue.poll()) != null) {
            polledJobs.add(polledJob);
        }

        // compare offered and polled jobs order
        offeredJobs.sort(Comparator.comparing(MCRJob::getAdded));
        assertEquals(offeredJobs.size(), polledJobs.size(), "polled jobs should be 10");
        for (int c = 0; c < offeredJobs.size(); c++) {
            assertEquals(offeredJobs.get(c), polledJobs.get(c), "polled jobs should be equal");
        }
    }

    @Test
    public void peek() {
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction1.class, new MCRConfiguration2JobConfig(), mockDAO);
        List<MCRJob> offeredJobs = new ArrayList<>();

        MCRJob job;
        for (int i = 0; i < 10; i++) {
            job = new MCRJob();
            job.setAction(MCRTestJobAction1.class);
            job.setParameter("count", Integer.toString(i));
            job.setStatus(MCRJobStatus.NEW);
            job.setAdded(new java.util.Date(1000000 - (i * 100000))); // reverse the actual order
            mockDAO.daoOfferedJobs.add(job);
            offeredJobs.add(job);
        }

        MCRJob peek = queue.peek();
        assertEquals(offeredJobs.get(9), peek, "peek should return the last added job");

        MCRJob peek2 = queue.peek();
        assertEquals(peek, peek2, "peek should return the same job");

        mockDAO.daoOfferedJobs.remove(peek);

        MCRJob peek3 = queue.peek();
        assertEquals(offeredJobs.get(8), peek3, "peek should return the next job");

        MCRJob peek4 = queue.peek();
        assertEquals(peek3, peek4, "peek should return the same job");
    }

}
