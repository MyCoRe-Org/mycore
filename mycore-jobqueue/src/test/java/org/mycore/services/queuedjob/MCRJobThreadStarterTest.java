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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;

import jakarta.persistence.EntityTransaction;

public class MCRJobThreadStarterTest extends MCRJPATestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static List<MCRJob> getAllJobs(MCRJobDAOJPAImpl dao, Class<? extends MCRJobAction> action) {
        return dao.getJobs(action, null, null, null, null);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testRun() {
        MCRConfiguration2JobConfig config = new MCRConfiguration2JobConfig();
        MCRJobDAOJPAImpl dao = new MCRJobDAOJPAImpl();
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction.class, config, dao);
        MCRJobThreadStarter starter = new MCRJobThreadStarter(MCRTestJobAction.class, config, queue);

        Date baseTime = new Date();

        MCRJob job1 = new MCRJob(MCRTestJobAction.class);
        job1.setParameter("count", "1");
        job1.setParameter("error", "false");
        job1.setStatus(MCRJobStatus.NEW);
        job1.setAdded(new Date(baseTime.getTime() + 20));
        queue.offer(job1);

        MCRJob job2 = new MCRJob(MCRTestJobAction.class);
        job2.setParameter("count", "2");
        job2.setParameter("error", "false");
        job2.setStatus(MCRJobStatus.NEW);
        job2.setAdded(new Date(baseTime.getTime() + 40));
        queue.offer(job2);

        MCRJob job3 = new MCRJob(MCRTestJobAction.class);
        job3.setParameter("count", "3");
        job3.setParameter("error", "true");
        job3.setStatus(MCRJobStatus.NEW);
        job3.setAdded(new Date(baseTime.getTime() + 60));
        queue.offer(job3);

        EntityTransaction transaction = getEntityManager().get().getTransaction();
        transaction.commit();
        transaction.begin();

        Thread thread = new Thread(starter);
        thread.start();

        try {
            int maxWait = 10000;
            int stepTime = 100;
            while (getAllJobs(dao, job1.getAction()).stream()
                .filter(j -> j.getStatus() == MCRJobStatus.FINISHED || j.getStatus() == MCRJobStatus.ERROR)
                .count() < 3 && maxWait > 0) {
                Thread.sleep(stepTime);
                LOGGER.info("waiting for jobs to finish time left: {}", maxWait);
                maxWait -= stepTime;
                transaction.rollback(); // rollback to get new data in the next iteration
                transaction.begin();
            }
        } catch (InterruptedException e) {
            throw new MCRException(e);
        }

        long finishedJobCount
            = getAllJobs(dao, job1.getAction()).stream().filter(j -> j.getStatus() == MCRJobStatus.FINISHED).count();
        long errorJobCount
            = getAllJobs(dao, job1.getAction()).stream().filter(j -> j.getStatus() == MCRJobStatus.ERROR).count();
        Assert.assertEquals("Finished Job count should be 2", 2, finishedJobCount);
        Assert.assertEquals("Error Job count should be 1", 1, errorJobCount);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> props = super.getTestProperties();
        props.put("MCR.Processable.Registry.Class", MCRCentralProcessableRegistry.class.getName());
        return props;
    }
}
