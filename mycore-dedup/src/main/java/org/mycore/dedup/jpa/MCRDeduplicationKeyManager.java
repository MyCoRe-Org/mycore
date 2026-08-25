package org.mycore.dedup.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jdom2.Element;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.MCRDeDupCriteriaBuilder;
import org.mycore.dedup.MCRDeDupCriterion;
import org.mycore.dedup.model.MCRPossibleDuplicate;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;


public final class MCRDeduplicationKeyManager {

    private static MCRDeduplicationKeyManager keyManager = new MCRDeduplicationKeyManager();

    public static MCRDeduplicationKeyManager getInstance() {
        return keyManager;
    }


    private MCRDeduplicationKeyManager() {

    }
    public enum SortOrder {
        ASC, DESC, NONE
    }

    public enum DeduplicationNoDuplicateOrderFields {
        MCR_ID_1, MCR_ID_2, CREATOR, DATE
    }
    private EntityManager getEntityManager() {
        return MCREntityManagerProvider.getCurrentEntityManager();
    }

    private static SingularAttribute getOrderColumn(DeduplicationNoDuplicateOrderFields by) {
        return switch (by) {
            case MCR_ID_1 -> MCRDeduplicationNoDuplicate_.mcrId1;
            case MCR_ID_2 -> MCRDeduplicationNoDuplicate_.mcrId2;
            case CREATOR -> MCRDeduplicationNoDuplicate_.creator;
            case DATE -> MCRDeduplicationNoDuplicate_.creationDate;
        };
    }


    public void addDeduplicationKey(String mcrId, String type, String key) {
        MCRDeduplicationKey dk = new MCRDeduplicationKey();
        dk.setMcrId(mcrId);
        dk.setDeduplicationType(type);
        dk.setDeduplicationKey(key);
        getEntityManager().persist(dk);
    }

    public void addDeduplicationKeyIfNotExists(String mcrId, String type, String key) {
        if (deduplicationKeyExists(mcrId, type, key)) {
            return;
        }
        addDeduplicationKey(mcrId, type, key);
    }

    public boolean deduplicationKeyExists(String mcrId, String type, String key) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<MCRDeduplicationKey> root = query.from(MCRDeduplicationKey.class);

        query.select(cb.count(root)).where(cb.and(
                cb.equal(root.get(MCRDeduplicationKey_.mcrId), mcrId),
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationType), type),
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationKey), key)
        ));

        return getEntityManager().createQuery(query).getSingleResult() > 0;
    }

    public boolean hasDuplicate(String type, String key) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<MCRDeduplicationKey> root = query.from(MCRDeduplicationKey.class);

        query.select(cb.count(root)).where(cb.and(
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationType), type),
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationKey), key)
        ));

        return getEntityManager().createQuery(query).getSingleResult() > 1;
    }

    public void clearDeduplicationKeys(String mcrId) {
        String query = MCRDeduplicationKey.DEDUPLICATION_KEY_DELETE_BY_MCR_ID;
        getEntityManager()
                .createNamedQuery(query)
                .setParameter("mcrId", mcrId)
                .executeUpdate();
    }


    public List<MCRPossibleDuplicate> getDuplicates
            (SortOrder idSort, SortOrder typeSort, String duplicationTypeFilter) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<MCRDeduplicationKey> dk1 = query.from(MCRDeduplicationKey.class);
        Root<MCRDeduplicationKey> dk2 = query.from(MCRDeduplicationKey.class);

        Subquery<MCRDeduplicationNoDuplicate> subquery = query.subquery(MCRDeduplicationNoDuplicate.class);
        Root<MCRDeduplicationNoDuplicate> dfp = subquery.from(MCRDeduplicationNoDuplicate.class);
        Predicate falsePositive = cb.or(
                cb.and(cb.equal(dfp.get(MCRDeduplicationNoDuplicate_.mcrId1), dk1.get(MCRDeduplicationKey_.mcrId)),
                        cb.equal(dfp.get(MCRDeduplicationNoDuplicate_.mcrId2), dk2.get(MCRDeduplicationKey_.mcrId))),
                cb.and(cb.equal(dfp.get(MCRDeduplicationNoDuplicate_.mcrId1), dk2.get(MCRDeduplicationKey_.mcrId)),
                        cb.equal(dfp.get(MCRDeduplicationNoDuplicate_.mcrId2), dk1.get(MCRDeduplicationKey_.mcrId)))
        );
        subquery.select(dfp).where(falsePositive);

        List<Predicate> conditions = new ArrayList<>();
        conditions.add(cb.equal(dk1.get(MCRDeduplicationKey_.deduplicationKey),
                dk2.get(MCRDeduplicationKey_.deduplicationKey)));
        conditions.add(cb.equal(dk1.get(MCRDeduplicationKey_.deduplicationType),
                dk2.get(MCRDeduplicationKey_.deduplicationType)));
        conditions.add(cb.notEqual(dk1.get(MCRDeduplicationKey_.mcrId), dk2.get(MCRDeduplicationKey_.mcrId)));
        conditions.add(cb.lessThan(dk1.get(MCRDeduplicationKey_.mcrId), dk2.get(MCRDeduplicationKey_.mcrId)));
        conditions.add(cb.not(cb.exists(subquery)));

        if (duplicationTypeFilter != null) {
            conditions.add(cb.equal(dk1.get(MCRDeduplicationKey_.deduplicationType), duplicationTypeFilter));
        }

        query.multiselect(
                dk1.get(MCRDeduplicationKey_.mcrId).alias("MCR_ID_1"),
                dk2.get(MCRDeduplicationKey_.mcrId).alias("MCR_ID_2"),
                dk1.get(MCRDeduplicationKey_.deduplicationType),
                dk1.get(MCRDeduplicationKey_.deduplicationKey)
        ).where(cb.and(conditions.toArray(new Predicate[0])));

        List<Order> orderList = new ArrayList<>();
        if (typeSort == SortOrder.ASC) {
            orderList.add(cb.asc(dk1.get(MCRDeduplicationKey_.deduplicationType)));
        } else if (typeSort == SortOrder.DESC) {
            orderList.add(cb.desc(dk1.get(MCRDeduplicationKey_.deduplicationType)));
        }

        if (idSort == SortOrder.ASC) {
            orderList.add(cb.asc(dk1.get(MCRDeduplicationKey_.mcrId)));
        } else if (idSort == SortOrder.DESC) {
            orderList.add(cb.desc(dk2.get(MCRDeduplicationKey_.mcrId)));
        }

        if (!orderList.isEmpty()) {
            query.orderBy(orderList);
        }

        return getEntityManager().createQuery(query).getResultList().stream()
                .map(o -> new MCRPossibleDuplicate((String) o[0], (String) o[1], (String) o[2], (String) o[3]))
                .toList();
    }

    public List<MCRDeduplicationKey> getDuplicates(String mcrId, String type, String key) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<MCRDeduplicationKey> query = cb.createQuery(MCRDeduplicationKey.class);
        Root<MCRDeduplicationKey> root = query.from(MCRDeduplicationKey.class);

        Subquery<String> sub1 = query.subquery(String.class);
        Root<MCRDeduplicationNoDuplicate> dfp1 = sub1.from(MCRDeduplicationNoDuplicate.class);
        sub1.select(dfp1.get(MCRDeduplicationNoDuplicate_.mcrId2)).where(cb.and(
                cb.equal(dfp1.get(MCRDeduplicationNoDuplicate_.mcrId1), mcrId),
                cb.equal(dfp1.get(MCRDeduplicationNoDuplicate_.mcrId2), root.get(MCRDeduplicationKey_.mcrId))
        ));

        Subquery<String> sub2 = query.subquery(String.class);
        Root<MCRDeduplicationNoDuplicate> dfp2 = sub2.from(MCRDeduplicationNoDuplicate.class);
        sub2.select(dfp2.get(MCRDeduplicationNoDuplicate_.mcrId1)).where(cb.and(
                cb.equal(dfp2.get(MCRDeduplicationNoDuplicate_.mcrId2), mcrId),
                cb.equal(dfp2.get(MCRDeduplicationNoDuplicate_.mcrId1), root.get(MCRDeduplicationKey_.mcrId))
        ));

        query.select(root).where(cb.and(
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationKey), key),
                cb.equal(root.get(MCRDeduplicationKey_.deduplicationType), type),
                cb.notEqual(root.get(MCRDeduplicationKey_.mcrId), mcrId),
                cb.not(root.get(MCRDeduplicationKey_.mcrId).in(sub1)),
                cb.not(root.get(MCRDeduplicationKey_.mcrId).in(sub2))
        ));

        return getEntityManager().createQuery(query).getResultList();
    }

    public List<MCRDeduplicationKey> getDuplicates(String type, String... keys) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<MCRDeduplicationKey> query = cb.createQuery(MCRDeduplicationKey.class);
        Root<MCRDeduplicationKey> root = query.from(MCRDeduplicationKey.class);

        List<Predicate> keyPredicates = Stream.of(keys)
                .map(key -> cb.equal(root.get(MCRDeduplicationKey_.deduplicationKey), key))
                .toList();

        Predicate combined = cb.or(keyPredicates.toArray(new Predicate[0]));
        Predicate finalPredicate = type != null
                ? cb.and(cb.equal(root.get(MCRDeduplicationKey_.deduplicationType), type), combined)
                : combined;

        query.select(root).where(finalPredicate);
        return getEntityManager().createQuery(query).getResultList();
    }


    public void addNoDuplicate(String id, String duplicateOf, String creator, Date date) {
        MCRDeduplicationNoDuplicate noDuplicate = new MCRDeduplicationNoDuplicate();
        noDuplicate.setMcrId1(id);
        noDuplicate.setMcrId2(duplicateOf);
        noDuplicate.setCreator(creator);
        noDuplicate.setCreationDate(date);
        getEntityManager().persist(noDuplicate);
    }

    public void removeNoDuplicate(int id) {
        MCRDeduplicationNoDuplicate entity = getEntityManager().find(MCRDeduplicationNoDuplicate.class, id);
        getEntityManager().remove(entity);
    }

    public void clearNoDuplicates(String mcrId) {
        String query = MCRDeduplicationNoDuplicate.DEDUPLICATION_KEY_DELETE_BY_MCR_ID;
        getEntityManager().createNamedQuery(query)
                .setParameter("mcrId", mcrId)
                .executeUpdate();
    }

    public List<MCRDeduplicationNoDuplicate> getNoDuplicates(SortOrder order, DeduplicationNoDuplicateOrderFields by) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<MCRDeduplicationNoDuplicate> query = cb.createQuery(MCRDeduplicationNoDuplicate.class);
        Root<MCRDeduplicationNoDuplicate> root = query.from(MCRDeduplicationNoDuplicate.class);

        if (order != SortOrder.NONE) {
            SingularAttribute attr = getOrderColumn(by);
            query.orderBy(order == SortOrder.ASC ? cb.asc(root.get(attr)) : cb.desc(root.get(attr)));
        }

        return getEntityManager().createQuery(query).getResultList();
    }


    public void updateDeDupCriteria(Element mods, MCRObjectID id, MCRDeDupCriteriaBuilder builder) {
        for (Element extension : mods.getChildren("extension", MCRConstants.MODS_NAMESPACE)) {
            extension.removeChildren("dedup");
            if(extension.getChildren().isEmpty()) {
                extension.detach();
            }
        }

        if (mods.getName().equals("mods") || mods.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE) != null) {
            clearDeduplicationKeys(id.toString());
        }

        for (MCRDeDupCriterion criteria : builder.buildFromMyCoreObject(mods)) {
            addDeduplicationKeyIfNotExists(id.toString(), criteria.getType(), criteria.getKey());
        }
    }
}
