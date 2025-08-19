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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.services.queuedjob.action.MCRTestJobAction1;
import org.mycore.services.queuedjob.action.MCRTestJobAction2;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;

import jakarta.persistence.EntityManager;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRJobDAOJPAImplTest {

    public static final String NEW_COUNT = "13";
    MCRJobDAOJPAImpl dao;
    MCRJob job1;
    MCRJob job2;
    MCRJob job3;
    MCRJob job4;
    MCRJob job5;

    MCRJob xJob6;
    MCRJob xJob7;
    MCRJob xJob8;
    HashMap<String, String> errorTrueParam;
    HashMap<String, String> errorFalseParam;
    List<MCRJob> allJobs;

    List<MCRJob> xJobs;

    private static void assertAllPresent(List<MCRJob> expected, List<MCRJob> jobs) {
        assertEquals(expected.size(), jobs.size(), "There should be " + expected.size() + " jobs");
        for (int i = 0; i < expected.size(); i++) {
            assertTrue(jobs.contains(expected.get(i)),
                "Job " + expected.get(i).toString() + " should be in the list "
                    + jobs.stream().map(MCRJob::toString).collect(Collectors.joining(", ")));
        }
    }

    @BeforeEach
    public void setUp() throws Exception {

        dao = new MCRJobDAOJPAImpl();

        Date baseTime = new Date();

        job1 = new MCRJob(MCRTestJobAction1.class);
        job1.setParameter("count", "1");
        job1.setParameter("error", "false");
        job1.setStatus(MCRJobStatus.NEW);
        job1.setAdded(new Date(baseTime.getTime() + 20));

        job2 = new MCRJob(MCRTestJobAction1.class);
        job2.setParameter("count", "2");
        job2.setParameter("error", "false");
        job2.setStatus(MCRJobStatus.PROCESSING);
        job2.setAdded(new Date(baseTime.getTime() + 40));

        job3 = new MCRJob(MCRTestJobAction1.class);
        job3.setParameter("count", "3");
        job3.setParameter("error", "true");
        job3.setStatus(MCRJobStatus.FINISHED);
        job3.setAdded(new Date(baseTime.getTime() + 60));

        job4 = new MCRJob(MCRTestJobAction2.class);
        job4.setParameter("count", "4");
        job4.setParameter("error", "true");
        job4.setStatus(MCRJobStatus.ERROR);
        job4.setAdded(new Date(baseTime.getTime() + 80));

        job5 = new MCRJob(MCRTestJobAction2.class);
        job5.setParameter("count", "5");
        job5.setParameter("error", "true");
        job5.setStatus(MCRJobStatus.MAX_TRIES);
        job5.setAdded(new Date(baseTime.getTime() + 100));

        xJob6 = new MCRJob(MCRTestJobAction1.class);
        xJob6.setParameter("count", "6");
        xJob6.setParameter("error", "false");
        xJob6.setStatus(MCRJobStatus.NEW);
        xJob6.setAdded(new Date(baseTime.getTime() + 120));

        xJob7 = new MCRJob(MCRTestJobAction1.class);
        xJob7.setParameter("count", "7");
        xJob7.setParameter("error", "false");
        xJob7.setStatus(MCRJobStatus.NEW);
        xJob7.setAdded(new Date(baseTime.getTime() + 140));

        xJob8 = new MCRJob(MCRTestJobAction1.class);
        xJob8.setParameter("count", "8");
        xJob8.setParameter("error", "false");
        xJob8.setStatus(MCRJobStatus.NEW);
        xJob8.setAdded(new Date(baseTime.getTime() + 160));

        errorTrueParam = new HashMap<>();
        errorTrueParam.put("error", "true");

        errorFalseParam = new HashMap<>();
        errorFalseParam.put("error", "false");

        allJobs = Stream.of(job1, job2, job3, job4, job5).collect(Collectors.toList());
        xJobs = Stream.of(xJob6, xJob7, xJob8).collect(Collectors.toList());
    }

    @Test
    public void getJobs() {
        EntityManager em = getEntityManager();

        allJobs.forEach(em::persist);

        List<MCRJob> jobs = dao.getJobs(null, null, null, null, null);
        assertAllPresent(allJobs, jobs);

        jobs = dao.getJobs(null, Collections.emptyMap(), null, null, null);
        assertAllPresent(allJobs, jobs);

        jobs = dao.getJobs(null, null, Collections.emptyList(), null, null);
        assertAllPresent(allJobs, jobs);

        jobs = dao.getJobs(null, null, null, 5, null);
        assertAllPresent(allJobs, jobs);

        jobs = dao.getJobs(null, null, null, 4, null);
        assertAllPresent(allJobs.subList(0, 4), jobs);

        jobs = dao.getJobs(null, null, null, 3, null);
        assertAllPresent(allJobs.subList(0, 3), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, null, null, null, null);
        assertAllPresent(Arrays.asList(job1, job2, job3), jobs);

        jobs = dao.getJobs(MCRTestJobAction2.class, null, null, null, null);
        assertAllPresent(Arrays.asList(job4, job5), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, Collections.emptyMap(),
            Stream.of(MCRJobStatus.NEW, MCRJobStatus.PROCESSING).toList(), null, null);
        assertAllPresent(Arrays.asList(job1, job2), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, Collections.emptyMap(),
            Stream.of(MCRJobStatus.NEW, MCRJobStatus.PROCESSING).toList(), 1, null);
        assertAllPresent(List.of(job1), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, job1.getParameters(), null, null, null);
        assertAllPresent(List.of(job1), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, job1.getParameters(), List.of(MCRJobStatus.PROCESSING), null, null);
        assertAllPresent(Collections.emptyList(), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, errorFalseParam, null, null, null);
        assertAllPresent(List.of(job1, job2), jobs);

        jobs = dao.getJobs(MCRTestJobAction1.class, errorTrueParam, null, null, null);
        assertAllPresent(List.of(job3), jobs);

        jobs = dao.getJobs(null, errorTrueParam, null, null, null);
        assertAllPresent(List.of(job3, job4, job5), jobs);

        jobs = dao.getJobs(MCRTestJobAction2.class, job1.getParameters(), null, null, null);
        assertAllPresent(Collections.emptyList(), jobs);

        jobs = dao.getJobs(MCRTestJobAction2.class, errorFalseParam, null, null, null);
        assertAllPresent(Collections.emptyList(), jobs);
    }

    @Test
    public void removeJobs() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        dao.removeJobs(MCRTestJobAction1.class, null, null);
        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Arrays.asList(job4, job5), resultList);

        dao.removeJobs(MCRTestJobAction2.class, null, null);
        resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Collections.emptyList(), resultList);
    }

    @Test
    public void removeJobs2() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        dao.removeJobs(null, errorTrueParam, null);
        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Arrays.asList(job1, job2), resultList);
    }

    @Test
    public void removeJobs3() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        dao.removeJobs(null, errorFalseParam, null);
        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Arrays.asList(job3, job4, job5), resultList);
    }

    @Test
    public void removeJobs4() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        dao.removeJobs(null, null, List.of(MCRJobStatus.NEW, MCRJobStatus.PROCESSING));
        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Arrays.asList(job3, job4, job5), resultList);
    }

    @Test
    public void removeJobs5() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        dao.removeJobs(null, null, List.of(MCRJobStatus.ERROR, MCRJobStatus.MAX_TRIES));
        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(Arrays.asList(job1, job2, job3), resultList);
    }

    @Test
    public void getJob() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        MCRJob job = dao.getJob(MCRTestJobAction1.class, job1.getParameters(), List.of(MCRJobStatus.NEW));
        assertEquals(job1, job, "Job 1 should be equal");

        job = dao.getJob(MCRTestJobAction1.class, job1.getParameters(), List.of(MCRJobStatus.PROCESSING));
        assertNull(job, "Job 1 should be null");

        job = dao.getJob(MCRTestJobAction1.class, job1.getParameters(),
            List.of(MCRJobStatus.NEW, MCRJobStatus.PROCESSING));
        assertEquals(job1, job, "Job 1 should be equal");

        job = dao.getJob(MCRTestJobAction1.class, job1.getParameters(), null);
        assertEquals(job1, job, "Job 1 should be equal");

        job = dao.getJob(MCRTestJobAction1.class, job1.getParameters(), Collections.emptyList());
        assertEquals(job1, job, "Job 1 should be equal");

        job = dao.getJob(null, job1.getParameters(), null);
        assertEquals(job1, job, "Job 1 should be equal");

        job = dao.getJob(MCRTestJobAction1.class, job2.getParameters(), null);
        assertEquals(job2, job, "Job 2 should be equal");

        try {
            job = dao.getJob(MCRTestJobAction1.class, null, null);
            fail("There should be an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void getNextJobs() {
        EntityManager em = getEntityManager();

        allJobs.forEach(em::persist);
        xJobs.forEach(em::persist);

        List<MCRJob> jobs = dao.getNextJobs(MCRTestJobAction1.class, 1);
        assertAllPresent(List.of(job1), jobs);

        MCRJob j1 = em.merge(job1);
        em.remove(j1);

        jobs = dao.getNextJobs(MCRTestJobAction1.class, 2);
        assertAllPresent(List.of(xJob6, xJob7), jobs);

        MCRJob j6 = em.merge(xJob6);
        MCRJob j7 = em.merge(xJob7);
        em.remove(j6);
        em.remove(j7);

        jobs = dao.getNextJobs(MCRTestJobAction1.class, 2);
        assertAllPresent(List.of(xJob8), jobs);
    }

    @Test
    public void remainingJobCount() {
        EntityManager em = getEntityManager();

        allJobs.forEach(em::persist);
        xJobs.forEach(em::persist);

        int jobs = dao.getRemainingJobCount(MCRTestJobAction1.class);
        assertEquals(4, jobs, "There should be 4 remaining jobs");

        MCRJob j1 = em.merge(job1);
        em.remove(j1);

        jobs = dao.getRemainingJobCount(MCRTestJobAction1.class);
        assertEquals(3, jobs, "There should be 2 remaining jobs");

        MCRJob j6 = em.merge(xJob6);
        MCRJob j7 = em.merge(xJob7);
        em.remove(j6);
        em.remove(j7);

        jobs = dao.getRemainingJobCount(MCRTestJobAction1.class);
        assertEquals(1, jobs, "There should be 1 remaining jobs");
    }

    @Test
    public void updateJob() {
        EntityManager em = getEntityManager();
        allJobs.forEach(em::persist);

        MCRJob job1 = dao.getJob(MCRTestJobAction1.class, this.job1.getParameters(), null);
        assertEquals(this.job1, job1, "Job 1 should be equal");
        job1.setStatus(MCRJobStatus.ERROR);
        dao.updateJob(job1);

        job1 = dao.getJob(MCRTestJobAction1.class, this.job1.getParameters(), null);
        assertEquals(MCRJobStatus.ERROR, job1.getStatus(), "Job 1 Status should be ERROR");

        job1.setParameter("count", NEW_COUNT);
        dao.updateJob(job1);
        job1 = dao.getJob(MCRTestJobAction1.class, this.job1.getParameters(), null);
        assertEquals(NEW_COUNT, job1.getParameter("count"), "Job 1 Count should be 13");
    }

    @Test
    public void addJob() {
        EntityManager em = getEntityManager();
        allJobs.forEach(job -> dao.addJob(job));

        List<MCRJob> resultList = em.createQuery("SELECT j FROM MCRJob j", MCRJob.class).getResultList();
        assertAllPresent(allJobs, resultList);
    }

    private EntityManager reset() {
        MCRJPATestHelper.startNewTransaction();
        return getEntityManager();
    }

    private EntityManager getEntityManager() {
        return MCREntityManagerProvider.getCurrentEntityManager();
    }

}
