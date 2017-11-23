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

package org.mycore.datamodel.classifications2.impl;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.QueryHints;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryLink;

/**
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
@Entity
@Table(name = "MCRCategoryLink",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "category", "objectID", "objectType" }) },
    indexes = { @Index(columnList = "objectID, objectType", name = "ObjectIDType") })
@NamedQueries({
    @NamedQuery(name = "MCRCategoryLink.ObjectIDByCategory",
        query = "SELECT objectReference.objectID FROM MCRCategoryLinkImpl WHERE category.id=:id"),
    @NamedQuery(name = "MCRCategoryLink.deleteByObjectCollection",
        query = "DELETE FROM MCRCategoryLinkImpl WHERE objectReference.objectID IN (:ids) and objectReference.type=:type"),
    @NamedQuery(name = "MCRCategoryLink.NumberPerClassID",
        query = "SELECT cat.id.ID, count(distinct link.objectReference.objectID) as num"
            + "  FROM MCRCategoryLinkImpl link, MCRCategoryImpl cat, MCRCategoryImpl cattree"
            + "  WHERE cattree.internalID = link.category"
            + "    AND cattree.id.rootID=:classID"
            + "    AND cat.id.rootID=:classID"
            + "    AND cattree.left BETWEEN cat.left AND cat.right"
            + "  GROUP BY cat.id.ID"),
    @NamedQuery(name = "MCRCategoryLink.NumberPerChildOfParentID",
        query = "SELECT cat.id.ID, count(distinct link.objectReference.objectID) as num"
            + "  FROM MCRCategoryLinkImpl link, MCRCategoryImpl cat, MCRCategoryImpl cattree"
            + "  WHERE cattree.internalID = link.category"
            + "    AND cattree.id.rootID=:classID"
            + "    AND cat.parent.internalID=:parentID"
            + "    AND cattree.left BETWEEN cat.left AND cat.right"
            + "  GROUP BY cat.id.ID"),
    @NamedQuery(name = "MCRCategoryLink.categoriesByObjectID",
        query = "SELECT category.id FROM MCRCategoryLinkImpl WHERE objectReference.objectID=:id and objectReference.type=:type"),
    @NamedQuery(name = "MCRCategoryLink.ObjectIDByCategoryAndType",
        query = "SELECT objectReference.objectID FROM MCRCategoryLinkImpl WHERE category.id=:id and objectReference.type=:type"),
    @NamedQuery(name = "MCRCategoryLink.NumberByTypePerClassID",
        query = "SELECT cat.id.ID, count(distinct link.objectReference.objectID) as num"
            + "  FROM MCRCategoryLinkImpl link, MCRCategoryImpl cat, MCRCategoryImpl cattree"
            + "  WHERE cattree.internalID = link.category"
            + "    AND link.objectReference.type=:type"
            + "    AND cattree.id.rootID=:classID"
            + "    AND cat.id.rootID=:classID"
            + "    AND cattree.left BETWEEN cat.left AND cat.right"
            + "  GROUP BY cat.id.ID"),
    @NamedQuery(name = "MCRCategoryLink.NumberByTypePerChildOfParentID",
        query = "SELECT cat.id.ID, count(distinct link.objectReference.objectID) as num"
            + "  FROM MCRCategoryLinkImpl link, MCRCategoryImpl cat, MCRCategoryImpl cattree"
            + "  WHERE cattree.internalID = link.category"
            + "    AND link.objectReference.type=:type"
            + "    AND cattree.id.rootID=:classID"
            + "    AND cat.parent.internalID=:parentID"
            + "    AND cattree.left BETWEEN cat.left AND cat.right"
            + "  GROUP BY cat.id.ID"),
    @NamedQuery(name = "MCRCategoryLink.deleteByObjectID",
        query = "DELETE FROM MCRCategoryLinkImpl WHERE objectReference.objectID=:id and objectReference.type=:type"),
    @NamedQuery(name = "MCRCategoryLink.CategoryAndObjectID",
        query = "SELECT link.objectReference.objectID"
            + "  FROM MCRCategoryLinkImpl link, MCRCategoryImpl cat, MCRCategoryImpl cattree"
            + "  WHERE cattree.internalID = link.category"
            + "    AND link.objectReference.objectID=:objectID"
            + "    AND link.objectReference.type=:type"
            + "    AND cattree.id.rootID=:rootID"
            + "    AND cat.id.rootID=:rootID"
            + "    AND cat.id.ID=:categID"
            + "    AND cattree.left BETWEEN cat.left AND cat.right",
        hints = { @QueryHint(name = QueryHints.READ_ONLY, value = "true") }),
    @NamedQuery(name = "MCRCategoryLink.linkedClassifications",
        query = "SELECT distinct node.id.rootID from MCRCategoryImpl as node, MCRCategoryLinkImpl as link where node.internalID=link.category")
})
class MCRCategoryLinkImpl implements MCRCategoryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(targetEntity = MCRCategoryImpl.class)
    @JoinColumn(name = "category")
    private MCRCategory category;

    @Embedded
    private MCRCategLinkReference objectReference;

    MCRCategoryLinkImpl() {
        this(null, null);
    }

    MCRCategoryLinkImpl(MCRCategory category, MCRCategLinkReference objectReference) {
        this.category = category;
        this.objectReference = objectReference;
    }

    @Override
    public MCRCategory getCategory() {
        return category;
    }

    public void setCategory(MCRCategoryImpl category) {
        this.category = category;
    }

    @Override
    public MCRCategLinkReference getObjectReference() {
        return objectReference;
    }

    public void setObjectReference(MCRCategLinkReference objectReference) {
        this.objectReference = objectReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (category == null ? 0 : category.hashCode());
        result = prime * result + (objectReference == null ? 0 : objectReference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRCategoryLinkImpl)) {
            return false;
        }
        final MCRCategoryLinkImpl other = (MCRCategoryLinkImpl) obj;
        if (category == null) {
            if (other.category != null) {
                return false;
            }
        } else if (!category.equals(other.category)) {
            return false;
        }
        if (objectReference == null) {
            if (other.objectReference != null) {
                return false;
            }
        } else if (!objectReference.equals(other.objectReference)) {
            return false;
        }
        return true;
    }

}
