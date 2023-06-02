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

package org.mycore.services.queuedjob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;

public class MCRJobQueueTest extends MCRTestCase {

    private MCRMockJobDAO mockDAO;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDAO = new MCRMockJobDAO();
    }

    @Test
    public void offer() throws InterruptedException {
        final List<MCRJob> offeredJobs = new ArrayList<>();
        final List<MCRJob> notifiedJobs = new ArrayList<>();

        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction.class, new MCRConfiguration2JobConfig(), mockDAO);

        MCRJobQueueEventListener listener = notifiedJobs::add;

        queue.addListener(listener);

        MCRJob job;
        for (int c = 10; c > 0; c--) {
            if (c == 5) {
                queue.removeListener(listener);
            }

            job = new MCRJob();
            job.setParameter("count", Integer.toString(c));
            Assert.assertTrue("job should be offered", queue.offer(job));
            Thread.sleep(2); // sleep to get different timestamps
            offeredJobs.add(job);
        }

        Assert.assertEquals("offered jobs should be 10", offeredJobs.size(), mockDAO.daoOfferedJobs.size());
        for (int c = 0; c < offeredJobs.size(); c++) {
            Assert.assertEquals("offered jobs should be equal", offeredJobs.get(c), mockDAO.daoOfferedJobs.get(c));
        }

        Assert.assertEquals("notified jobs should be 5", 5, notifiedJobs.size());
        for (int c = 0; c < notifiedJobs.size(); c++) {
            Assert.assertEquals("notified jobs should be equal", offeredJobs.get(c), notifiedJobs.get(c));
        }

        // reading the same jobs should work, but the count needs to stay the same
        for (MCRJob j : offeredJobs) {
            Assert.assertTrue("job should not be offered", queue.offer(j));
        }

        Assert.assertEquals("offered jobs should be 10", offeredJobs.size(), mockDAO.daoOfferedJobs.size());

        // the offered jobs should have the right action, the right status and a timestamp
        for (MCRJob j : offeredJobs) {
            Assert.assertEquals("job action should be MCRTestJobAction", MCRTestJobAction.class, j.getAction());
            Assert.assertEquals("job status should be new", MCRJobStatus.NEW, j.getStatus());
            Assert.assertTrue("job added timestamp should be set", j.getAdded().getTime() > 0);
        }

        // offer a job with different action should trigger an exception
        job = new MCRJob(MCRTestJobAction2.class);
        job.setParameter("count", Integer.toString(1));
        try {
            queue.offer(job);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void poll() {
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction.class, new MCRConfiguration2JobConfig(), mockDAO);
        List<MCRJob> offeredJobs = new ArrayList<>();

        MCRJob job;
        for (int i = 0; i < 10; i++) {
            job = new MCRJob();
            job.setId((long) i + 1);
            job.setAction(MCRTestJobAction.class);
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
        Assert.assertEquals("polled jobs should be 10", offeredJobs.size(), polledJobs.size());
        for (int c = 0; c < offeredJobs.size(); c++) {
            Assert.assertEquals("polled jobs should be equal", offeredJobs.get(c), polledJobs.get(c));
        }
    }

    @Test
    public void peek() {
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction.class, new MCRConfiguration2JobConfig(), mockDAO);
        List<MCRJob> offeredJobs = new ArrayList<>();

        MCRJob job;
        for (int i = 0; i < 10; i++) {
            job = new MCRJob();
            job.setAction(MCRTestJobAction.class);
            job.setParameter("count", Integer.toString(i));
            job.setStatus(MCRJobStatus.NEW);
            job.setAdded(new java.util.Date(1000000 - (i * 100000))); // reverse the actual order
            mockDAO.daoOfferedJobs.add(job);
            offeredJobs.add(job);
        }

        MCRJob peek = queue.peek();
        Assert.assertEquals("peek should return the last added job", offeredJobs.get(9), peek);

        MCRJob peek2 = queue.peek();
        Assert.assertEquals("peek should return the same job", peek, peek2);

        mockDAO.daoOfferedJobs.remove(peek);

        MCRJob peek3 = queue.peek();
        Assert.assertEquals("peek should return the next job", offeredJobs.get(8), peek3);

        MCRJob peek4 = queue.peek();
        Assert.assertEquals("peek should return the same job", peek3, peek4);
    }

    protected Map<String, String> getTestProperties() {
        return super.getTestProperties();
    }

}
