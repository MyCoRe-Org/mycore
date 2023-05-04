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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MCRMockJobDAO implements MCRJobDAO {

    public final List<MCRJob> daoOfferedJobs = new LinkedList<>();

    Long id = 1L;

    public boolean jobMatch(MCRJob job, Class<? extends MCRJobAction> action,
        Map<String, String> params, List<MCRJobStatus> status) {
        boolean actionEquals = action == null || Objects.equals(job.getAction(), action);
        boolean paramsEquals = params == null || job.getParameters().entrySet().containsAll(params.entrySet());
        boolean statusEquals = status == null || status.size() == 0 || status.contains(job.getStatus());

        return actionEquals && paramsEquals && statusEquals;
    }

    @Override
    public List<MCRJob> getJobs(Class<? extends MCRJobAction> action,
        Map<String, String> params, List<MCRJobStatus> status,
        Integer maxResults,
        Integer offset) {
        return daoOfferedJobs.stream().filter(j -> jobMatch(j, action, params, status))
            .skip(offset == null ? 0 : offset)
            .limit(maxResults == null ? daoOfferedJobs.size() : maxResults).toList();
    }

    @Override
    public int getJobCount(Class<? extends MCRJobAction> action, Map<String, String> params,
        List<MCRJobStatus> status) {
        return getJobs(action, params, status, null, null).size();
    }

    @Override
    public int removeJobs(Class<? extends MCRJobAction> action, Map<String, String> params,
        List<MCRJobStatus> status) {
        List<MCRJob> jobs = daoOfferedJobs.stream().filter(j -> jobMatch(j, action, params, status)).toList();
        daoOfferedJobs.removeAll(jobs);
        return jobs.size();
    }

    @Override
    public MCRJob getJob(Class<? extends MCRJobAction> action, Map<String, String> params,
        List<MCRJobStatus> status) {
        List<MCRJob> jobs = getJobs(action, params, status, 5, 0);
        if (jobs.size() > 1) {
            throw new IllegalStateException("more than one job found");
        }
        return jobs.stream().findFirst().orElse(null);
    }

    @Override
    public List<MCRJob> getNextJobs(Class<? extends MCRJobAction> action, Integer amount) {
        return daoOfferedJobs.stream()
            .filter(j -> Objects.equals(j.getAction(), action) && j.getStatus().equals(MCRJobStatus.NEW))
            .sorted(Comparator.comparing(MCRJob::getAdded))
            .limit(amount == null ? daoOfferedJobs.size() : amount).toList();
    }

    @Override
    public int getRemainingJobCount(Class<? extends MCRJobAction> action) {
        return (int) daoOfferedJobs.stream()
            .filter(j -> Objects.equals(j.getAction(), action) && j.getStatus().equals(MCRJobStatus.NEW))
            .count();
    }

    @Override
    public boolean updateJob(MCRJob job) {
        return daoOfferedJobs.remove(job) && daoOfferedJobs.add(job);
    }

    @Override
    public boolean addJob(MCRJob job) {
        job.setId(id++);
        daoOfferedJobs.add(job);
        return true;
    }

    @Override
    public List<? extends Class<? extends MCRJobAction>> getActions() {
        return List.of(MCRTestJobAction.class, MCRTestJobAction2.class);
    }
}
