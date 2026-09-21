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

import java.time.Instant;

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
 * Marks an unordered pair of objects as confirmed non-duplicates, so that possible duplicate matches
 * between them are suppressed. The two object ids are stored in a normalized order
 * ({@code objectId1 <= objectId2}) so that a pair is recorded only once regardless of the order it was
 * reported in.
 */
@Entity
@Table(name = "MCRDeDupNoDuplicate",
    uniqueConstraints = {
        @UniqueConstraint(name = "MCRDEDUPNODUP_UNIQUE", columnNames = { "OBJECT_ID_1", "OBJECT_ID_2" })
    },
    indexes = {
        @Index(name = "MCRDEDUPNODUP_OBJECT_1", columnList = "OBJECT_ID_1"),
        @Index(name = "MCRDEDUPNODUP_OBJECT_2", columnList = "OBJECT_ID_2")
    })
@NamedQueries({
    @NamedQuery(name = MCRDeDupNoDuplicate.DELETE_BY_OBJECT_ID,
        query = "DELETE FROM MCRDeDupNoDuplicate n WHERE n.objectId1 = :objectId OR n.objectId2 = :objectId")
})
public class MCRDeDupNoDuplicate {

    public static final String DELETE_BY_OBJECT_ID = "MCRDeDupNoDuplicate.deleteByObjectId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "OBJECT_ID_1", length = MCRObjectID.MAX_LENGTH, nullable = false)
    private String objectId1;

    @Column(name = "OBJECT_ID_2", length = MCRObjectID.MAX_LENGTH, nullable = false)
    private String objectId2;

    @Column(name = "CREATOR", length = 64)
    private String creator;

    @Column(name = "CREATION_DATE", nullable = false)
    private Instant created;

    public MCRDeDupNoDuplicate() {
    }

    public MCRDeDupNoDuplicate(String objectId1, String objectId2, String creator, Instant created) {
        this.objectId1 = objectId1;
        this.objectId2 = objectId2;
        this.creator = creator;
        this.created = created;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getObjectId1() {
        return objectId1;
    }

    public void setObjectId1(String objectId1) {
        this.objectId1 = objectId1;
    }

    public String getObjectId2() {
        return objectId2;
    }

    public void setObjectId2(String objectId2) {
        this.objectId2 = objectId2;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }
}
