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

package org.mycore.backend.jpa.objectinfo;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.classifications2.MCRCategLinkReference_;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryID_;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl_;
import org.mycore.datamodel.classifications2.impl.MCRCategoryLinkImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryLinkImpl_;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRObjectQuery;
import org.mycore.datamodel.common.MCRObjectQueryResolver;
import org.mycore.datamodel.ifs2.MCRObjectIDDateImpl;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.objectinfo.MCRObjectInfo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

public class MCRObjectInfoEntityQueryResolver implements MCRObjectQueryResolver {

    private static final MCRObjectQuery.SortBy SORT_BY_DEFAULT = MCRObjectQuery.SortBy.created;

    protected TypedQuery<MCRObjectInfoEntity> convertQuery(MCRObjectQuery query) {
        Objects.requireNonNull(query, "The Query cant be null");
        int offset = query.offset();

        if (offset != -1 && query.afterId() != null) {
            throw new IllegalArgumentException("offset and after_id should not be combined!");
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<MCRObjectInfoEntity> criteriaQuery = criteriaBuilder.createQuery(MCRObjectInfoEntity.class);
        Root<MCRObjectInfoEntity> oe = criteriaQuery.from(MCRObjectInfoEntity.class);
        criteriaQuery.select(oe);

        List<Predicate> filters = getFilter(query, criteriaBuilder, oe);

        applyClassificationFilter(query, criteriaBuilder, criteriaQuery, oe, filters, em);

        if (query.afterId() != null) {
            applyLastId(query, criteriaBuilder, criteriaQuery, oe, filters);
        } else {
            applySort(query, criteriaBuilder, criteriaQuery, oe);
        }

        if (filters.size() > 0) {
            criteriaQuery.where(criteriaBuilder.and(filters.toArray(new Predicate[0])));
        }

        TypedQuery<MCRObjectInfoEntity> typedQuery = em.createQuery(criteriaQuery);

        if (offset != -1) {
            typedQuery.setFirstResult(offset);
        }

        int limit = query.limit();
        if (limit != -1) {
            typedQuery.setMaxResults(limit);
        }

        return typedQuery;
    }

    private <T> void applyClassificationFilter(MCRObjectQuery query, CriteriaBuilder criteriaBuilder,
        CriteriaQuery<T> criteriaQuery, Root<MCRObjectInfoEntity> oe, List<Predicate> filters, EntityManager em) {
        if (query.getIncludeCategories().size() > 0) {
            List<MCRCategoryImpl> idImplMap = getCategories(query, em);

            if (idImplMap.size() != query.getIncludeCategories().size()) {
                throw new IllegalArgumentException(
                    "some of " + String.join(", ", query.getIncludeCategories()) + " do not exist!");
            }

            // TODO: add child categories
            Predicate[] categoryPredicates = idImplMap.stream().map(cat -> {
                Root<MCRCategoryLinkImpl> cl = criteriaQuery.from(MCRCategoryLinkImpl.class);
                Root<MCRCategoryImpl> c = criteriaQuery.from(MCRCategoryImpl.class);

                Predicate linkToObject = criteriaBuilder.equal(
                    cl.get(MCRCategoryLinkImpl_.objectReference).get(MCRCategLinkReference_.OBJECT_ID),
                    oe.get(MCRObjectInfoEntity_.ID));

                Predicate categoryToLink = criteriaBuilder.equal(cl.get(MCRCategoryLinkImpl_.CATEGORY),
                    c.get(MCRCategoryImpl_.INTERNAL_ID));

                Predicate between = criteriaBuilder.between(
                    c.get(MCRCategoryImpl_.LEFT),
                    cat.getLeft(),
                    cat.getRight());

                Predicate rootIdEqual = criteriaBuilder
                    .equal(c.get(MCRCategoryImpl_.id).get(MCRCategoryID_.ROOT_ID), cat.getRootID());

                return criteriaBuilder.and(linkToObject, categoryToLink, rootIdEqual, between);
            }).toArray(Predicate[]::new);

            filters.add(criteriaBuilder.and(categoryPredicates));
        }
    }

    private List<MCRCategoryImpl> getCategories(MCRObjectQuery query, EntityManager em) {
        CriteriaBuilder categCb = em.getCriteriaBuilder();
        CriteriaQuery<MCRCategoryImpl> categQuery = categCb.createQuery(MCRCategoryImpl.class);
        Root<MCRCategoryImpl> classRoot = categQuery.from(MCRCategoryImpl.class);
        categQuery.select(classRoot);
        List<MCRCategoryID> categoryIDList = query.getIncludeCategories().stream()
            .map(MCRCategoryID::fromString)
            .collect(Collectors.toList());

        categQuery.where(classRoot.get("id").in(categoryIDList));
        TypedQuery<MCRCategoryImpl> typedQuery = em.createQuery(categQuery);

        return typedQuery.getResultList();
    }

    protected void applySort(MCRObjectQuery query, CriteriaBuilder criteriaBuilder,
        CriteriaQuery<MCRObjectInfoEntity> criteriaQuery, Root<MCRObjectInfoEntity> source) {
        MCRObjectQuery.SortBy sf = query.sortBy() == null ? SORT_BY_DEFAULT : query.sortBy();

        SingularAttribute<MCRObjectInfoEntity, ?> attribute = switch (sf) {
            case id -> MCRObjectInfoEntity_.id;
            case created -> MCRObjectInfoEntity_.createDate;
            case modified -> MCRObjectInfoEntity_.modifyDate;
        };

        if (query.sortAsc() == null || query.sortAsc() == MCRObjectQuery.SortOrder.asc) {
            criteriaQuery.orderBy(criteriaBuilder.asc(source.get(attribute)));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(source.get(attribute)));
        }
    }

    protected void applyLastId(MCRObjectQuery query, CriteriaBuilder criteriaBuilder,
        CriteriaQuery<MCRObjectInfoEntity> criteriaQuery, Root<MCRObjectInfoEntity> source, List<Predicate> filters) {
        if (query.sortBy() != MCRObjectQuery.SortBy.id && query.sortBy() != null) {
            throw new UnsupportedOperationException("last id can not be used with " + query.sortBy());
        }
        if (query.sortAsc() == null || query.sortAsc() == MCRObjectQuery.SortOrder.asc) {
            filters.add(criteriaBuilder.greaterThan(source.get(MCRObjectInfoEntity_.id), query.afterId()));
            criteriaQuery.orderBy(criteriaBuilder.asc(source.get(MCRObjectInfoEntity_.id)));
        } else {
            filters.add(criteriaBuilder.lessThan(source.get(MCRObjectInfoEntity_.id), query.afterId()));
            criteriaQuery.orderBy(criteriaBuilder.desc(source.get(MCRObjectInfoEntity_.id)));
        }
    }

    private List<Predicate> getFilter(MCRObjectQuery query, CriteriaBuilder criteriaBuilder,
        Root<MCRObjectInfoEntity> source) {
        List<Predicate> predicates = new ArrayList<>();

        Optional.ofNullable(query.type())
            .map(type -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.objectType), type))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.project())
            .map(project -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.objectProject), project))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdBy())
            .map(creator -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.createdBy), creator))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedBy())
            .map(modifier -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.modifiedBy), modifier))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedBy())
            .map(deleter -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.deletedBy), deleter))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectInfoEntity_.createDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.createdBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectInfoEntity_.createDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectInfoEntity_.modifyDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.modifiedBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectInfoEntity_.modifyDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedAfter())
            .map(date -> criteriaBuilder.greaterThanOrEqualTo(source.get(MCRObjectInfoEntity_.deleteDate), date))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.deletedBefore())
            .map(date -> criteriaBuilder.lessThanOrEqualTo(source.get(MCRObjectInfoEntity_.deleteDate), date))
            .ifPresent(predicates::add);

        Optional.of(query.numberGreater())
            .filter(numberBiggerThan -> numberBiggerThan > -1)
            .map(numberBiggerThan -> criteriaBuilder.greaterThan(source.get(MCRObjectInfoEntity_.objectNumber),
                numberBiggerThan))
            .ifPresent(predicates::add);

        Optional.of(query.numberLess())
            .filter(numberLess -> numberLess > -1)
            .map(numberLess -> criteriaBuilder.lessThan(source.get(MCRObjectInfoEntity_.objectNumber), numberLess))
            .ifPresent(predicates::add);

        Optional.ofNullable(query.status())
                .map(state -> criteriaBuilder.equal(source.get(MCRObjectInfoEntity_.state), state))
                .ifPresent(predicates::add);
        /*
         per default, we only query not deleted objects, but if the use queries one of the deleted fields
         then deleted objects are included
         */
        if (Optional.ofNullable(query.deletedBy()).isEmpty() &&
            Optional.ofNullable(query.deletedBefore()).isEmpty() &&
            Optional.ofNullable(query.deletedAfter()).isEmpty()) {
            predicates.add(criteriaBuilder.isNull(source.get(MCRObjectInfoEntity_.deleteDate)));
            predicates.add(criteriaBuilder.isNull(source.get(MCRObjectInfoEntity_.deletedBy)));
        }

        return predicates;
    }

    @Override
    public List<MCRObjectID> getIds(MCRObjectQuery objectQuery) {
        TypedQuery<MCRObjectInfoEntity> typedQuery = convertQuery(objectQuery);
        return typedQuery.getResultList()
            .stream()
            .map(MCRObjectInfoEntity::getId)
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRObjectIDDate> getIdDates(MCRObjectQuery objectQuery) {
        TypedQuery<MCRObjectInfoEntity> typedQuery = convertQuery(objectQuery);
        return typedQuery.getResultList()
            .stream()
            .map(entity -> new MCRObjectIDDateImpl(Date.from(entity.getModifyDate()), entity.getId().toString()))
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRObjectInfo> getInfos(MCRObjectQuery objectQuery) {
        TypedQuery<MCRObjectInfoEntity> typedQuery = convertQuery(objectQuery);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        return typedQuery.getResultList()
            .stream()
            .peek(em::detach)
            .collect(Collectors.toList());
    }

    @Override
    public int count(MCRObjectQuery objectQuery) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Number> criteriaQuery = criteriaBuilder.createQuery(Number.class);
        Root<MCRObjectInfoEntity> source = criteriaQuery.from(MCRObjectInfoEntity.class);
        criteriaQuery.select(criteriaBuilder.count(source));

        List<Predicate> filters = getFilter(objectQuery, criteriaBuilder, source);
        applyClassificationFilter(objectQuery, criteriaBuilder, criteriaQuery, source, filters, em);

        if (filters.size() > 0) {
            criteriaQuery.where(criteriaBuilder.and(filters.toArray(new Predicate[0])));
        }

        return em.createQuery(criteriaQuery).getSingleResult().intValue();
    }
}
