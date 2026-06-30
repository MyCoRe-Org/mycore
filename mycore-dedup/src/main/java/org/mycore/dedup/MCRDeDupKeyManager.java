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

package org.mycore.dedup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.backend.MCRDeDupKey;
import org.mycore.dedup.backend.MCRDeDupNoDuplicate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Reads and writes the deduplication data of objects in the database.
 * <p>
 * For every object the set of its {@link MCRDeDupCriterion}s is stored as {@link MCRDeDupKey} rows.
 * Two objects sharing a key are possible duplicates. Pairs of objects that have been manually
 * confirmed to be no duplicates are stored as {@link MCRDeDupNoDuplicate} rows and are excluded from
 * the duplicate queries.
 * <p>
 * The manager is configurable through {@code MCR.DeDup.KeyManager.Class}.
 */
public class MCRDeDupKeyManager {

    /**
     * @return the configured instance of the deduplication key manager
     */
    public static MCRDeDupKeyManager obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRDeDupKeyManager.class, "MCR.DeDup.KeyManager.Class");
    }

    /**
     * Replaces all stored deduplication keys of the given object with the given criteria. Criterion
     * values are truncated to {@link MCRDeDupKey#MAX_VALUE_LENGTH} characters; criteria that become
     * equal after truncation are stored only once.
     *
     * @param objectId the object the criteria belong to
     * @param criteria the criteria to store, may be empty
     */
    public void storeKeys(MCRObjectID objectId, Set<MCRDeDupCriterion> criteria) {
        EntityManager em = getEntityManager();
        removeManagedKeys(em, objectId.toString());
        em.flush();
        Set<MCRDeDupCriterion> truncated = truncate(criteria);
        for (MCRDeDupCriterion criterion : truncated) {
            em.persist(new MCRDeDupKey(objectId.toString(), criterion.type(), criterion.value()));
        }
    }

    /**
     * Removes all stored deduplication keys of the given object.
     *
     * @param objectId the object whose keys are removed
     */
    public void removeKeys(MCRObjectID objectId) {
        getEntityManager().createNamedQuery(MCRDeDupKey.DELETE_BY_OBJECT_ID)
            .setParameter("objectId", objectId.toString())
            .executeUpdate();
    }

    /**
     * Removes all no-duplicate markings that reference the given object.
     *
     * @param objectId the object whose no-duplicate markings are removed
     */
    public void removeNoDuplicates(MCRObjectID objectId) {
        getEntityManager().createNamedQuery(MCRDeDupNoDuplicate.DELETE_BY_OBJECT_ID)
            .setParameter("objectId", objectId.toString())
            .executeUpdate();
    }

    /**
     * Marks the unordered pair of the two given objects as confirmed non-duplicates.
     *
     * @param objectIdA one object
     * @param objectIdB the other object
     * @param creator   the id of the user that created the marking, may be {@code null}
     */
    public void addNoDuplicate(MCRObjectID objectIdA, MCRObjectID objectIdB, String creator) {
        ObjectIdPair pair = ObjectIdPair.of(objectIdA.toString(), objectIdB.toString());
        getEntityManager().persist(new MCRDeDupNoDuplicate(pair.first(), pair.second(), creator, Instant.now()));
    }

    /**
     * Removes a single no-duplicate marking by its database id.
     *
     * @param id the database id of the no-duplicate marking
     */
    public void removeNoDuplicate(long id) {
        EntityManager em = getEntityManager();
        MCRDeDupNoDuplicate noDuplicate = em.find(MCRDeDupNoDuplicate.class, id);
        if (noDuplicate != null) {
            em.remove(noDuplicate);
        }
    }

    /**
     * @return all stored no-duplicate markings
     */
    public List<MCRDeDupNoDuplicate> listNoDuplicates() {
        return getEntityManager()
            .createQuery("SELECT n FROM MCRDeDupNoDuplicate n", MCRDeDupNoDuplicate.class)
            .getResultList();
    }

    /**
     * Finds all objects that are possible duplicates of the given object, i.e. objects that share at
     * least one deduplication key with it and that are not marked as no-duplicates of it.
     *
     * @param objectId the object to find duplicates for
     * @return the possible duplicates, one entry per matching criterion, without duplicate entries
     */
    public List<MCRPossibleDuplicate> findDuplicates(MCRObjectID objectId) {
        String id = objectId.toString();
        List<Object[]> rows = getEntityManager().createQuery(
            "SELECT o.objectId, o.type, o.value FROM MCRDeDupKey o, MCRDeDupKey s "
                + "WHERE s.objectId = :id AND o.type = s.type AND o.value = s.value AND o.objectId <> :id "
                + "AND NOT EXISTS (SELECT 1 FROM MCRDeDupNoDuplicate n "
                + "  WHERE (n.objectId1 = :id AND n.objectId2 = o.objectId) "
                + "     OR (n.objectId2 = :id AND n.objectId1 = o.objectId))",
            Object[].class)
            .setParameter("id", id)
            .getResultList();
        return rows.stream()
            .map(row -> MCRPossibleDuplicate.of(id, (String) row[0],
                new MCRDeDupCriterion((String) row[1], (String) row[2])))
            .distinct()
            .toList();
    }

    /**
     * Finds all existing objects that share at least one of the given criteria.
     * <p>
     * Criterion values are truncated to {@link MCRDeDupKey#MAX_VALUE_LENGTH} characters before
     * matching, exactly as in {@link #storeKeys(MCRObjectID, Set)}.
     *
     * @param criteria the criteria to match, may be empty
     * @return matching objects mapped to the criteria they share
     */
    public Map<MCRObjectID, Set<MCRDeDupCriterion>> findDuplicates(Set<MCRDeDupCriterion> criteria) {
        List<MCRDeDupCriterion> truncated = new ArrayList<>(truncate(criteria));
        if (truncated.isEmpty()) {
            return Map.of();
        }

        StringBuilder query = new StringBuilder("SELECT k.objectId, k.type, k.value FROM MCRDeDupKey k WHERE ");
        for (int i = 0; i < truncated.size(); i++) {
            if (i > 0) {
                query.append(" OR ");
            }
            query.append("(k.type = :type").append(i).append(" AND k.value = :value").append(i).append(')');
        }
        query.append(" ORDER BY k.objectId, k.type, k.value");

        TypedQuery<Object[]> jpaQuery = getEntityManager().createQuery(query.toString(), Object[].class);
        for (int i = 0; i < truncated.size(); i++) {
            MCRDeDupCriterion criterion = truncated.get(i);
            jpaQuery
                .setParameter("type" + i, criterion.type())
                .setParameter("value" + i, criterion.value());
        }

        Map<MCRObjectID, Set<MCRDeDupCriterion>> result = new LinkedHashMap<>();
        for (Object[] row : jpaQuery.getResultList()) {
            result.computeIfAbsent(MCRObjectID.getInstance((String) row[0]), ignored -> new LinkedHashSet<>())
                .add(new MCRDeDupCriterion((String) row[1], (String) row[2]));
        }
        return result;
    }

    /**
     * Finds all pairs of objects that are possible duplicates of each other, i.e. that share at least
     * one deduplication key and that are not marked as no-duplicates of each other.
     * <p>
     * Instead of a self-join over the whole key table this loads only the keys that actually have a
     * matching partner (via an {@code EXISTS} semi-join) and groups them in memory, which avoids the
     * combinatorial blow-up of a plain self-join.
     *
     * @return the possible duplicate pairs, one entry per pair and matching criterion
     */
    public List<MCRPossibleDuplicate> findAllDuplicates() {
        EntityManager em = getEntityManager();
        List<Object[]> rows = em.createQuery(
            "SELECT k.objectId, k.type, k.value FROM MCRDeDupKey k "
                + "WHERE EXISTS (SELECT 1 FROM MCRDeDupKey o "
                + "  WHERE o.type = k.type AND o.value = k.value AND o.objectId <> k.objectId) "
                + "ORDER BY k.type, k.value, k.objectId",
            Object[].class)
            .getResultList();

        Set<ObjectIdPair> noDuplicatePairs = loadNoDuplicatePairs(em);
        List<MCRPossibleDuplicate> result = new ArrayList<>();

        int index = 0;
        while (index < rows.size()) {
            String type = (String) rows.get(index)[1];
            String value = (String) rows.get(index)[2];
            List<String> members = new ArrayList<>();
            while (index < rows.size() && Objects.equals(rows.get(index)[1], type)
                && Objects.equals(rows.get(index)[2], value)) {
                members.add((String) rows.get(index)[0]);
                index++;
            }
            MCRDeDupCriterion criterion = new MCRDeDupCriterion(type, value);
            for (int a = 0; a < members.size(); a++) {
                for (int b = a + 1; b < members.size(); b++) {
                    if (!noDuplicatePairs.contains(ObjectIdPair.of(members.get(a), members.get(b)))) {
                        result.add(MCRPossibleDuplicate.of(members.get(a), members.get(b), criterion));
                    }
                }
            }
        }
        return result;
    }

    private Set<ObjectIdPair> loadNoDuplicatePairs(EntityManager em) {
        return em.createQuery("SELECT n.objectId1, n.objectId2 FROM MCRDeDupNoDuplicate n", Object[].class)
            .getResultList().stream()
            .map(row -> ObjectIdPair.of((String) row[0], (String) row[1]))
            .collect(Collectors.toSet());
    }

    private void removeManagedKeys(EntityManager em, String objectId) {
        em.createQuery("SELECT k FROM MCRDeDupKey k WHERE k.objectId = :objectId", MCRDeDupKey.class)
            .setParameter("objectId", objectId)
            .getResultList()
            .forEach(em::remove);
    }

    private static Set<MCRDeDupCriterion> truncate(Set<MCRDeDupCriterion> criteria) {
        return criteria.stream()
            .map(criterion -> new MCRDeDupCriterion(criterion.type(), truncate(criterion.value())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String truncate(String value) {
        return value.length() > MCRDeDupKey.MAX_VALUE_LENGTH
            ? value.substring(0, MCRDeDupKey.MAX_VALUE_LENGTH)
            : value;
    }

    private static EntityManager getEntityManager() {
        return MCREntityManagerProvider.getCurrentEntityManager();
    }

    /**
     * An unordered pair of object ids, normalized so that {@code first <= second}. Used to look up
     * no-duplicate markings regardless of the order the two ids are given in.
     */
    private record ObjectIdPair(String first, String second) {

        static ObjectIdPair of(String objectIdA, String objectIdB) {
            return objectIdA.compareTo(objectIdB) <= 0
                ? new ObjectIdPair(objectIdA, objectIdB)
                : new ObjectIdPair(objectIdB, objectIdA);
        }
    }
}
