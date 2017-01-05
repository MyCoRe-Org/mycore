/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.services.queuedjob;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

        MCRJob job = new MCRJob(MCRTestJobAction.class);
        job.setParameter("count", "1");

        assertTrue("job should be offered", queue.offer(job));

        endTransaction();
        Thread.sleep(1000);
        startNewTransaction();

        Map<String, String> params = new HashMap<>();
        params.put("count", "1");

        job = queue.getJob(params);

        assertNotNull("job shouldn't null", job);
        assertTrue("job should be done", Boolean.parseBoolean(job.getParameter("done")));
    }

}
