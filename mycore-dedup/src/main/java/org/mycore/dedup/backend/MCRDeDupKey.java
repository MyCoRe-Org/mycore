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

package org.mycore.dedup.backend;

import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Stores a single deduplication criterion (type and value) of an object in the database. Two objects
 * sharing a row with equal {@link #getType() type} and {@link #getValue() value} are considered
 * possible duplicates. The criterion value is stored unhashed and is truncated to
 * {@link #MAX_VALUE_LENGTH} characters so that it stays indexable across the supported databases.
 */
@Entity
@Table(name = "MCRDeDupKey",
    uniqueConstraints = {
        @UniqueConstraint(name = "MCRDEDUPKEY_UNIQUE", columnNames = { "OBJECT_ID", "DEDUP_TYPE", "DEDUP_VALUE" })
    },
    indexes = {
        @Index(name = "MCRDEDUPKEY_OBJECT", columnList = "OBJECT_ID"),
        @Index(name = "MCRDEDUPKEY_MATCH", columnList = "DEDUP_TYPE, DEDUP_VALUE")
    })
@NamedQueries({
    @NamedQuery(name = MCRDeDupKey.DELETE_BY_OBJECT_ID,
        query = "DELETE FROM MCRDeDupKey k WHERE k.objectId = :objectId"),
    @NamedQuery(name = MCRDeDupKey.FIND_BY_OBJECT_ID,
        query = "SELECT k FROM MCRDeDupKey k WHERE k.objectId = :objectId"),
    @NamedQuery(name = MCRDeDupKey.FIND_DUPLICATE_KEYS_OF_OBJECT,
        query = "SELECT o.objectId, o.type, o.value FROM MCRDeDupKey o, MCRDeDupKey s "
            + "WHERE s.objectId = :objectId AND o.type = s.type AND o.value = s.value "
            + "AND o.objectId <> :objectId "
            + "AND NOT EXISTS (SELECT 1 FROM MCRDeDupNoDuplicate n "
            + "  WHERE (n.objectId1 = :objectId AND n.objectId2 = o.objectId) "
            + "     OR (n.objectId2 = :objectId AND n.objectId1 = o.objectId)) "
            + "ORDER BY o.objectId, o.type, o.value"),
    @NamedQuery(name = MCRDeDupKey.FIND_SHARED_KEYS,
        query = "SELECT k.objectId, k.type, k.value FROM MCRDeDupKey k "
            + "WHERE EXISTS (SELECT 1 FROM MCRDeDupKey o "
            + "  WHERE o.type = k.type AND o.value = k.value AND o.objectId <> k.objectId) "
            + "ORDER BY k.type, k.value, k.objectId")
})
public class MCRDeDupKey {

    public static final String DELETE_BY_OBJECT_ID = "MCRDeDupKey.deleteByObjectId";

    /** Selects all keys of the object given as {@code objectId}. */
    public static final String FIND_BY_OBJECT_ID = "MCRDeDupKey.findByObjectId";

    /**
     * Selects {@code objectId, type, value} of all keys of other objects that match a key of the object
     * given as {@code objectId} and are not marked as no-duplicates of it, ordered by object id and criterion.
     */
    public static final String FIND_DUPLICATE_KEYS_OF_OBJECT = "MCRDeDupKey.findDuplicateKeysOfObject";

    /**
     * Selects {@code objectId, type, value} of all keys that are shared with at least one other object,
     * ordered by criterion and object id.
     */
    public static final String FIND_SHARED_KEYS = "MCRDeDupKey.findSharedKeys";

    /** Maximum number of characters stored for a criterion value; longer values are truncated. */
    public static final int MAX_VALUE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "OBJECT_ID", length = MCRObjectID.MAX_LENGTH, nullable = false)
    private String objectId;

    @Column(name = "DEDUP_TYPE", length = 64, nullable = false)
    private String type;

    @Column(name = "DEDUP_VALUE", length = MAX_VALUE_LENGTH, nullable = false)
    private String value;

    public MCRDeDupKey() {
    }

    public MCRDeDupKey(String objectId, String type, String value) {
        this.objectId = objectId;
        this.type = type;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
