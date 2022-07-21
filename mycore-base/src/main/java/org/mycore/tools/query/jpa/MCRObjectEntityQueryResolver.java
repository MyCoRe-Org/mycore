/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.tools.query.jpa;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.MCRObjectEntity;
import org.mycore.backend.jpa.MCRObjectEntity_;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.ifs2.MCRObjectIDDateImpl;
import org.mycore.tools.query.MCRObjectQuery;
import org.mycore.tools.query.MCRObjectQueryResolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

public class MCRObjectEntityQueryResolver implements MCRObjectQueryResolver {

    public TypedQuery<MCRObjectEntity> translateQuery(MCRObjectQuery query) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<MCRObjectEntity> criteriaQuery = criteriaBuilder.createQuery(MCRObjectEntity.class);
        Root<MCRObjectEntity> source = criteriaQuery.from(MCRObjectEntity.class);
        criteriaQuery.select(source);

        List<Predicate> filters = getFilter(query, criteriaBuilder, source);

        if (query.lastId() != null) {
            if (query.sortBy() != MCRObjectQuery.SortField.ID && query.sortBy() != null) {
                throw new UnsupportedOperationException("last id can not be used with " + query.sortBy());
            }
            if (query.sortAsc() == null && query.sortAsc() == MCRObjectQuery.SortDirection.ASC) {
                if (!query.lastId().equals("")) {
                    filters.add(criteriaBuilder.greaterThan(source.get(MCRObjectEntity_.objectId), query.lastId()));
                }
                criteriaQuery.orderBy(criteriaBuilder.asc(source.get(MCRObjectEntity_.objectId)));
            } else {
                if (!query.lastId().equals("")) {
                    filters.add(criteriaBuilder.lessThan(source.get(MCRObjectEntity_.objectId), query.lastId()));
                }
                criteriaQuery.orderBy(criteriaBuilder.desc(source.get(MCRObjectEntity_.objectId)));
            }
        } else {
            MCRObjectQuery.SortField sf = query.sortBy() == null ? MCRObjectQuery.SortField.CREATED : query.sortBy();

            SingularAttribute<MCRObjectEntity, ?> attribute = switch (sf) {
                case ID -> MCRObjectEntity_.objectId;
                case CREATED -> MCRObjectEntity_.createDate;
                case MODIFIED -> MCRObjectEntity_.modifyDate;
            };

            if (query.sortAsc() == null && query.sortAsc() == MCRObjectQuery.SortDirection.ASC) {
                criteriaQuery.orderBy(criteriaBuilder.asc(source.get(attribute)));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(source.get(attribute)));
            }
        }

        if (filters.size() > 0) {
            criteriaQuery.where(criteriaBuilder.and(filters.toArray(new Predicate[0])));
        }

        TypedQuery<MCRObjectEntity> typedQuery = em.createQuery(criteriaQuery);

        int offset = query.offset();
        if (offset != -1) {
            typedQuery.setFirstResult(offset);
        }

        int limit = query.limit();
        if (limit != -1) {
            typedQuery.setMaxResults(limit);
        }

        return typedQuery;
    }

    private List<Predicate> getFilter(MCRObjectQuery query, CriteriaBuilder criteriaBuilder,
        Root<MCRObjectEntity> source) {
        List<Predicate> predicates = new ArrayList<>();

        Optional.ofNullable(query.type())
            .map(type -> criteriaBuilder.equal(source.get(MCRObjectEntity_.objectType), type))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.project())
            .map(project -> criteriaBuilder.equal(source.get(MCRObjectEntity_.objectProject), project))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdBy())
            .map(creator -> criteriaBuilder.equal(source.get(MCRObjectEntity_.createdBy), creator))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedBy())
            .map(modifier -> criteriaBuilder.equal(source.get(MCRObjectEntity_.modifiedBy), modifier))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedBy())
            .map(deleter -> criteriaBuilder.equal(source.get(MCRObjectEntity_.deletedBy), deleter))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectEntity_.createDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectEntity_.createDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectEntity_.modifyDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectEntity_.modifyDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectEntity_.deleteddate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectEntity_.deleteddate), date))
            .ifPresent(predicates::add);

        Optional.of(query.numberGreater())
            .filter(numberBiggerThan -> numberBiggerThan > -1)
            .map(numberBiggerThan -> criteriaBuilder.greaterThan(source.get(MCRObjectEntity_.objectNumber),
                numberBiggerThan))
            .ifPresent(predicates::add);

        Optional.of(query.numberLess())
                .filter(numberLess -> numberLess > -1)
                .map(numberLess -> criteriaBuilder.lessThan(source.get(MCRObjectEntity_.objectNumber),
                        numberLess))
                .ifPresent(predicates::add);
        /*
         per default we only query not deleted objects, but if the use queries one of the deleted fields
         then deleted objects are included
         */
        if (Optional.ofNullable(query.deletedBy()).isEmpty() &&
            Optional.ofNullable(query.deletedBefore()).isEmpty() &&
            Optional.ofNullable(query.deletedAfter()).isEmpty()) {
            predicates.add(criteriaBuilder.isNull(source.get(MCRObjectEntity_.deleteddate)));
            predicates.add(criteriaBuilder.isNull(source.get(MCRObjectEntity_.deletedBy)));
        }

        return predicates;
    }

    @Override
    public List<String> getIds(MCRObjectQuery objectQuery) {
        TypedQuery<MCRObjectEntity> typedQuery = translateQuery(objectQuery);
        return typedQuery.getResultList()
            .stream()
            .map(MCRObjectEntity::getObjectId)
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRObjectIDDate> getIdDates(MCRObjectQuery objectQuery) {
        TypedQuery<MCRObjectEntity> typedQuery = translateQuery(objectQuery);
        return typedQuery.getResultList()
            .stream()
            .map(entity -> new MCRObjectIDDateImpl(Date.from(entity.getModifyDate()), entity.getObjectId()))
            .collect(Collectors.toList());
    }

    @Override
    public int count(MCRObjectQuery objectQuery) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Number> criteriaQuery = criteriaBuilder.createQuery(Number.class);
        Root<MCRObjectEntity> source = criteriaQuery.from(MCRObjectEntity.class);
        criteriaQuery.select(criteriaBuilder.count(source));

        List<Predicate> filters = getFilter(objectQuery, criteriaBuilder, source);
        if (filters.size() > 0) {
            criteriaQuery.where(criteriaBuilder.and(filters.toArray(new Predicate[0])));
        }

        return em.createQuery(criteriaQuery).getSingleResult().intValue();
    }
}
