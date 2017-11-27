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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRJobQueueTest extends MCRJPATestCase {

    @Test
    public void testOffer() throws InterruptedException {
        MCRJobQueue queue = MCRJobQueue.getInstance(MCRTestJobAction.class);

        MCRJob job;
        for (int c = 10; c > 0; c--) {
            job = new MCRJob(MCRTestJobAction.class);
            job.setParameter("count", Integer.toString(c));

            assertTrue("job should be offered", queue.offer(job));

            endTransaction();
            startNewTransaction();
        }

        Map<String, String> params = new HashMap<>();
        params.put("count", "1");

        job = queue.getJob(params);

        int abortAfter = 600;
        while (job.getStatus() != MCRJobStatus.FINISHED) {
            Thread.sleep(100);
            job = queue.getJob(params);
            endTransaction();
            startNewTransaction();
            if (abortAfter-- == 0) {
                fail("Couldn't finish MCRJobQueueTest in one minute.");
            }
        }
        assertNotNull("job shouldn't null", job);
        assertTrue("job should be done", Boolean.parseBoolean(job.getParameter("done")));
    }

}
