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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Stores the display title of an object that has deduplication keys, so that the deduplication API can
 * list the possible duplicates together with their titles without having to load the full object
 * metadata for every row. The title is maintained by the {@code MCRDeDupEventHandler} and is truncated
 * to {@link #MAX_TITLE_LENGTH} characters.
 */
@Entity
@Table(name = "MCRDeDupTitle",
    uniqueConstraints = {
        @UniqueConstraint(name = "MCRDEDUPTITLE_UNIQUE", columnNames = { "OBJECT_ID" })
    })
@NamedQueries({
    @NamedQuery(name = MCRDeDupTitle.DELETE_BY_OBJECT_ID,
        query = "DELETE FROM MCRDeDupTitle t WHERE t.objectId = :objectId")
})
public class MCRDeDupTitle {

    public static final String DELETE_BY_OBJECT_ID = "MCRDeDupTitle.deleteByObjectId";

    /** Maximum number of characters stored for a title; longer titles are truncated. */
    public static final int MAX_TITLE_LENGTH = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "OBJECT_ID", length = MCRObjectID.MAX_LENGTH, nullable = false)
    private String objectId;

    @Column(name = "TITLE", length = MAX_TITLE_LENGTH, nullable = false)
    private String title;

    public MCRDeDupTitle() {
    }

    public MCRDeDupTitle(String objectId, String title) {
        this.objectId = objectId;
        this.title = title;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
