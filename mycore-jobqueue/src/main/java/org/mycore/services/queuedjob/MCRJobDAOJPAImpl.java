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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.mycore.backend.jpa.MCREntityManagerProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * JPA implementation of the {@link MCRJobDAO} interface.
 * @author Sebastian Hofmann
 */
public class MCRJobDAOJPAImpl implements MCRJobDAO {

    private static Optional<Predicate> getActionPredicate(Class<? extends MCRJobAction> action,
        CriteriaBuilder cb, Root<MCRJob> jobRoot,
        Predicate existingPredicate) {

        if (action != null) {
            if (existingPredicate == null) {
                return Optional.of(cb.equal(jobRoot.get(MCRJob_.action), action));
            } else {
                return Optional.of(cb.and(existingPredicate, cb.equal(jobRoot.get(MCRJob_.action), action)));
            }
        }

        return Optional.ofNullable(existingPredicate);
    }

    private static Optional<Predicate> getStatusPredicate(List<MCRJobStatus> statuses,
        CriteriaBuilder cb,
        Root<MCRJob> jobRoot) {
        if (statuses != null) {
            List<Predicate> statusPredicates = statuses.stream().map(status -> {
                Path<MCRJobStatus> statusPath = jobRoot.get(MCRJob_.status);
                return cb.equal(statusPath, status);
            }).toList();

            if (statusPredicates.size() > 0) {
                return Optional.of(cb.or(statusPredicates.toArray(new Predicate[0])));
            }
        }
        return Optional.empty();
    }

    private static CriteriaQuery<MCRJob> buildQuery(
        CriteriaBuilder cb,
        Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> statuses,
        BiConsumer<CriteriaQuery<MCRJob>, Root<MCRJob>> actionApplier) {

        CriteriaQuery<MCRJob> q = cb.createQuery(MCRJob.class);
        Root<MCRJob> jobRoot = q.from(MCRJob.class);
        actionApplier.accept(q, jobRoot);

        q.orderBy(cb.asc(jobRoot.get(MCRJob_.ADDED)));

        applyParamJoin(cb, params, jobRoot);

        Optional<Predicate> predicate;

        predicate = getStatusPredicate(statuses, cb, jobRoot);
        predicate = getActionPredicate(action, cb, jobRoot, predicate.orElse(null));
        predicate.ifPresent(q::where);

        return q;
    }

    private static CriteriaQuery<Long> buildCountQuery(
        CriteriaBuilder cb,
        Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> statuses) {

        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<MCRJob> jobRoot = q.from(MCRJob.class);

        q.select(cb.count(jobRoot));

        applyParamJoin(cb, params, jobRoot);

        Optional<Predicate> predicate;

        predicate = getStatusPredicate(statuses, cb, jobRoot);
        predicate = getActionPredicate(action, cb, jobRoot, predicate.orElse(null));
        predicate.ifPresent(q::where);

        return q;
    }

    private static void applyParamJoin(CriteriaBuilder cb, Map<String, String> params, Root<MCRJob> jobRoot) {
        if (params == null || params.isEmpty()) {
            return;
        }
        params.keySet().forEach(key -> {
            MapJoin<MCRJob, String, String> parameterJoin = jobRoot.join(MCRJob_.parameters, JoinType.INNER);
            Path<String> keyPath = parameterJoin.key();
            Path<String> valuePath = parameterJoin.value();
            parameterJoin.on(cb.equal(keyPath, key), cb.equal(valuePath, params.get(key)));
        });
    }

    @Override
    public List<MCRJob> getJobs(Class<? extends MCRJobAction> action, Map<String, String> params,
        List<MCRJobStatus> status, Integer maxResults, Integer offset) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRJob> q
            = buildQuery(cb, action, params, status, CriteriaQuery::select);
        TypedQuery<MCRJob> tq = em.createQuery(q);
        if (maxResults != null) {
            tq.setMaxResults(maxResults);
        }
        if (offset != null) {
            tq.setFirstResult(offset);
        }
        return tq.getResultList();
    }

    @Override
    public int getJobCount(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = buildCountQuery(cb, action, params, status);

        return em.createQuery(q).getSingleResult().intValue();
    }

    @Override
    public int removeJobs(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        List<MCRJob> jobs = getJobs(action, params, status, null, null);
        int jobCount = jobs.size();
        jobs.forEach(em::remove);
        return jobCount;
    }

    @Override
    public MCRJob getJob(Class<? extends MCRJobAction> action, Map<String, String> params, List<MCRJobStatus> status) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRJob> q
            = buildQuery(cb, action, params, status, CriteriaQuery::select);
        TypedQuery<MCRJob> tq = em.createQuery(q);
        try {
            return tq.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            throw new IllegalArgumentException("More than one job found for action " + action + " and params " + params,
                e);
        }
    }

    @Override
    public List<MCRJob> getNextJobs(Class<? extends MCRJobAction> action, Integer amount) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<MCRJob> oq = buildQuery(cb, action, Collections.emptyMap(), Stream.of(MCRJobStatus.NEW).toList(),
            (iq, root) -> {
                iq.select(root);
                iq.distinct(true);
            });

        TypedQuery<MCRJob> query = em.createQuery(oq);
        if (amount != null) {
            query.setMaxResults(amount);
        }

        return query
            .getResultList()
            .stream()
            .peek(em::detach)
            .toList();
    }

    @Override
    public int getRemainingJobCount(Class<? extends MCRJobAction> action) {
        return getJobCount(action, Collections.emptyMap(), Stream.of(MCRJobStatus.NEW).toList());
    }

    @Override
    public boolean updateJob(MCRJob job) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.merge(job);
        return true;
    }

    @Override
    public boolean addJob(MCRJob job) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(job);
        return true;
    }

    @Override
    public List<? extends Class<? extends MCRJobAction>> getActions() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Object> query = em.createNamedQuery("mcrjob.classes", Object.class);
        List<Object> resultList = query.getResultList();

        return resultList.stream().map(clazz -> (Class<? extends MCRJobAction>) clazz).toList();
    }

}
