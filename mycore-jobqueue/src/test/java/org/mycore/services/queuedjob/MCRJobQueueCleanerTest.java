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

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.services.queuedjob.action.MCRTestJobAction1;
import org.mycore.services.queuedjob.action.MCRTestJobAction2;
import org.mycore.services.queuedjob.action.MCRTestJobAction3;
import org.mycore.services.queuedjob.action.MCRTestJobAction4;
import org.mycore.services.queuedjob.action.MCRTestJobAction5;

import jakarta.persistence.EntityManager;

import static org.junit.Assert.assertEquals;
import static org.mycore.services.queuedjob.MCRSimpleJobSelector.Mode.EXCLUDE;
import static org.mycore.services.queuedjob.MCRSimpleJobSelector.Mode.INCLUDE;

public class MCRJobQueueCleanerTest extends MCRJPATestCase {

    public static final Class<MCRTestJobAction1> ACTION_1 = MCRTestJobAction1.class;
    public static final Class<MCRTestJobAction2> ACTION_2 = MCRTestJobAction2.class;
    public static final Class<MCRTestJobAction3> ACTION_3 = MCRTestJobAction3.class;
    public static final Class<MCRTestJobAction4> ACTION_4 = MCRTestJobAction4.class;
    public static final Class<MCRTestJobAction5> ACTION_5 = MCRTestJobAction5.class;

    public static final MCRJobStatus STATUS_1 = MCRJobStatus.NEW;
    public static final MCRJobStatus STATUS_2 = MCRJobStatus.PROCESSING;
    public static final MCRJobStatus STATUS_3 = MCRJobStatus.FINISHED;
    public static final MCRJobStatus STATUS_4 = MCRJobStatus.MAX_TRIES;
    public static final MCRJobStatus STATUS_5 = MCRJobStatus.ERROR;

    private MCRJobDAOJPAImpl dao;

    private List<MCRJob> jobs;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        dao = new MCRJobDAOJPAImpl();

        Date baseTime = new Date();
        long day = ChronoUnit.DAYS.getDuration().getSeconds() * 1000L;

        MCRJob job1 = new MCRJob(ACTION_1);
        job1.setStatus(STATUS_1);
        job1.setAdded(new Date(baseTime.getTime() - 1 * day));

        MCRJob job2 = new MCRJob(ACTION_2);
        job2.setStatus(STATUS_2);
        job2.setAdded(new Date(baseTime.getTime() - 2 * day));

        MCRJob job3 = new MCRJob(ACTION_3);
        job3.setStatus(STATUS_3);
        job3.setAdded(new Date(baseTime.getTime() - 3 * day));

        MCRJob job4 = new MCRJob(ACTION_4);
        job4.setStatus(STATUS_4);
        job4.setAdded(new Date(baseTime.getTime() - 4 * day));

        MCRJob job5 = new MCRJob(ACTION_5);
        job5.setStatus(STATUS_5);
        job5.setAdded(new Date(baseTime.getTime() - 5 * day));

        jobs = List.of(job1, job2, job3, job4, job5);

    }

    @Test
    public void testIncludeNothing() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(0, numberOfJobs);
        assertEquals(jobs.size(), remainingJobs.size());

    }

    @Test
    public void testExcludeNothing() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(5, numberOfJobs);
        assertEquals(0, remainingJobs.size());

    }

    @Test
    public void testExcludeNothingWithLargeAge() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 10));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(0, numberOfJobs);
        assertEquals(5, remainingJobs.size());

    }


    @Test
    public void testExcludeNothingWithMediumAge() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 3));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(3, numberOfJobs);
        assertEquals(2, remainingJobs.size());

    }

    @Test
    public void testExcludeNothingWithSmallAge() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 1));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(5, numberOfJobs);
        assertEquals(0, remainingJobs.size());

    }


    @Test
    public void testIncludeOneJobType() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1);
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(1, numberOfJobs);
        Predicate<MCRJob> predicate = job -> actions.contains(job.getAction());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testIncludeMultipleJobTypes() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1, ACTION_2);
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(2, numberOfJobs);
        Predicate<MCRJob> predicate = job -> actions.contains(job.getAction());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeOneJobType() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1);
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(4, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !actions.contains(job.getAction());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeMultipleJobTypes() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1, ACTION_2);
        List<MCRJobStatus> statuses = Collections.emptyList();
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(3, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !actions.contains(job.getAction());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testIncludeOneStatus() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = List.of(STATUS_1);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(1, numberOfJobs);
        Predicate<MCRJob> predicate = job -> statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testIncludeMultipleStatuses() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = List.of(STATUS_1, STATUS_3);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(2, numberOfJobs);
        Predicate<MCRJob> predicate = job -> statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeOneStatus() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = List.of(STATUS_1);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, EXCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(4, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeMultipleStatuses() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = Collections.emptyList();
        List<MCRJobStatus> statuses = List.of(STATUS_1, STATUS_2);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, EXCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(3, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testIncludeJobTypeIncludeStatusWithoutMatch() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1);
        List<MCRJobStatus> statuses = List.of(STATUS_5);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(0, numberOfJobs);
        Predicate<MCRJob> predicate = job -> actions.contains(job.getAction()) && statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testIncludeJobTypeIncludeStatusWithMatches() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1, ACTION_5);
        List<MCRJobStatus> statuses = List.of(STATUS_1, STATUS_5);
        selectors.put("selector", new MCRSimpleJobSelector(actions, INCLUDE, statuses, INCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(2, numberOfJobs);
        Predicate<MCRJob> predicate = job -> actions.contains(job.getAction()) && statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeJobTypeExcludeStatusWithoutMatch() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_1, ACTION_2, ACTION_3);
        List<MCRJobStatus> statuses = List.of(STATUS_3, STATUS_4, STATUS_5);
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(0, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !actions.contains(job.getAction()) && !statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @Test
    public void testExcludeJobTypeExcludeStatusWithMatches() {

        insertAllJobs();

        Map<String, MCRJobSelector> selectors = new HashMap<>();
        List<Class<? extends MCRJobAction>> actions = List.of(ACTION_2, ACTION_3, ACTION_4);
        List<MCRJobStatus> statuses = List.of(STATUS_2, STATUS_3, STATUS_4);
        selectors.put("selector", new MCRSimpleJobSelector(actions, EXCLUDE, statuses, EXCLUDE, 0));

        int numberOfJobs = new MCRJobQueueCleaner(selectors, true).clean();
        List<MCRJob> remainingJobs = getAllJobs();

        assertEquals(2, numberOfJobs);
        Predicate<MCRJob> predicate = job -> !actions.contains(job.getAction()) && !statuses.contains(job.getStatus());
        assertEquals(jobs.stream().filter(predicate.negate()).count(), remainingJobs.size());

    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void insertAllJobs() {
        EntityManager manager = getEntityManager().get();
        jobs.forEach(manager::persist);
    }

    private List<MCRJob> getAllJobs() {
        return dao.getJobs(null, null, null, null, null);
    }

}
