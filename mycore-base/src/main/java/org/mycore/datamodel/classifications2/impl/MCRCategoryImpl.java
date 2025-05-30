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

package org.mycore.datamodel.classifications2.impl;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.SortNatural;
import org.mycore.backend.jpa.MCRURIConverter;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * @since 2.0
 */
@Entity
@Table(name = "MCRCategory",
    indexes = {
        @Index(columnList = "ClassID, leftValue, rightValue", name = "ClassLeftRight"),
        @Index(columnList = "leftValue", name = "ClassesRoot")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "ClassID", "CategID" }, name = "ClassCategUnique"),
        @UniqueConstraint(columnNames = { "ClassID", "leftValue" }, name = "ClassLeftUnique"),
        @UniqueConstraint(columnNames = { "ClassID", "rightValue" }, name = "ClassRightUnique") })
@NamedQueries({
    @NamedQuery(name = "MCRCategory.updateLeft",
        query = "UPDATE MCRCategoryImpl cat SET cat.left=cat.left+:increment WHERE "
            + "cat.id.rootID= :classID AND cat.left >= :left"),
    @NamedQuery(name = "MCRCategory.updateRight",
        query = "UPDATE MCRCategoryImpl cat SET cat.right=cat.right+:increment WHERE "
            + "cat.id.rootID= :classID AND cat.right >= :left"),
    @NamedQuery(name = "MCRCategory.commonAncestor",
        query = "FROM MCRCategoryImpl as cat WHERE "
            + "cat.id.rootID=:rootID AND cat.left < :left AND cat.right > :right "
            + "ORDER BY cat.left DESC"),
    @NamedQuery(name = "MCRCategory.byNaturalId",
        query = "FROM MCRCategoryImpl as cat WHERE "
            + "cat.id.rootID=:classID and (cat.id.id=:categID OR cat.id.id IS NULL AND :categID IS NULL)"),
    @NamedQuery(name = "MCRCategory.byLabelInClass",
        query = "FROM MCRCategoryImpl as cat "
            + "INNER JOIN cat.labels as label "
            + "  WHERE cat.id.rootID=:rootID AND "
            + "    cat.left BETWEEN :left and :right AND "
            + "    label.lang=:lang AND "
            + "    label.text=:text"),
    @NamedQuery(name = "MCRCategory.byLabel",
        query = "FROM MCRCategoryImpl as cat "
            + "  INNER JOIN cat.labels as label "
            + "  WHERE label.lang=:lang AND "
            + "    label.text=:text"),
    @NamedQuery(name = "MCRCategory.byClassAndLang",
        query = "FROM MCRCategoryImpl as cat "
            + "  INNER JOIN cat.labels as label "
            + "  WHERE cat.id.rootID=:classID"
            + "  AND label.lang=:lang"),
    @NamedQuery(name = "MCRCategory.prefetchClassQuery",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.id.rootID=:classID ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchClassLevelQuery",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.id.rootID=:classID AND cat.level <= :endlevel ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchCategQuery",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.id.rootID=:classID AND (cat.left BETWEEN :left AND :right OR cat.left=0) ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchCategLevelQuery",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.id.rootID=:classID AND (cat.left BETWEEN :left AND :right OR cat.left=0) AND "
            + "cat.level <= :endlevel ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.leftRightLevelQuery",
        query = MCRCategoryDTO.LRL_SELECT
            + " WHERE cat.id=:categID "),
    @NamedQuery(name = "MCRCategory.parentQuery",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.id.rootID=:classID AND (cat.left < :left AND cat.right > :right OR cat.id.id=:categID) "
            + "ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.rootCategs",
        query = MCRCategoryDTO.SELECT
            + " WHERE cat.left = 0 ORDER BY cat.id.rootID"),
    @NamedQuery(name = "MCRCategory.rootIds", query = "SELECT cat.id FROM MCRCategoryImpl cat WHERE cat.left = 0"),
    @NamedQuery(name = "MCRCategory.childCount",
        query = "SELECT CAST(count(*) AS integer) FROM MCRCategoryImpl children WHERE "
            + "children.parent.internalID=(SELECT cat.internalID FROM MCRCategoryImpl cat WHERE "
            + "cat.id.rootID=:classID and (cat.id.id=:categID OR cat.id.id IS NULL AND :categID IS NULL))"),
})

@Access(AccessType.PROPERTY)
public class MCRCategoryImpl extends MCRAbstractCategoryImpl implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private int left;
    private int right;
    private int internalID;

    int level;

    //Mapping definition

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getInternalID() {
        return internalID;
    }

    @Column(name = "leftValue")
    public int getLeft() {
        return left;
    }

    @Column(name = "rightValue")
    public int getRight() {
        return right;
    }

    @Override
    @Column
    public int getLevel() {
        return level;
    }

    @Transient
    int getPositionInParent() {
        LOGGER.debug("getposition called for {}", this::getId);
        if (parent == null) {
            LOGGER.debug("getposition called with no parent set.");
            return -1;
        }
        try {
            int position = getParent().getChildren().indexOf(this);
            if (position == -1) {
                // sometimes indexOf does not find this instance so we need to
                // check for the ID here to
                position = getPositionInParentByID();
            }
            return position;
        } catch (RuntimeException e) {
            LOGGER.error("Cannot use parent.getChildren() here", e);
            throw e;
        }
    }

    //    @NaturalId
    @Override
    @Embedded
    public MCRCategoryID getId() {
        return super.getId();
    }

    @Override
    @OneToMany(targetEntity = MCRCategoryImpl.class,
        cascade = {
            CascadeType.ALL },
        mappedBy = "parent")
    @OrderColumn(name = "positionInParent")
    @Access(AccessType.FIELD)
    public List<MCRCategory> getChildren() {
        return super.getChildren();
    }

    @Override
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "MCRCategoryLabels",
        joinColumns = @JoinColumn(name = "category"),
        uniqueConstraints = {
            @UniqueConstraint(columnNames = { "category", "lang" }) })
    @SortNatural
    public SortedSet<MCRLabel> getLabels() {
        return super.getLabels();
    }

    @Override
    @Column
    @Convert(converter = MCRURIConverter.class)
    public URI getURI() {
        return super.getURI();
    }

    @Override
    @ManyToOne
    @JoinColumn(name = "parentID")
    public MCRCategoryImpl getParent() {
        return (MCRCategoryImpl) super.parent;
    }

    public void setParent(MCRCategoryImpl parent) {
        super.parent = parent;
    }

    //End of Mapping

    @Override
    public boolean hasChildren() {
        //if children is initialized and has objects use it and don't depend on db values
        if (children != null && !children.isEmpty()) {
            return true;
        }
        if (right != left) {
            return right - left > 1;
        }
        return super.hasChildren();
    }

    @Transient
    private int getPositionInParentByID() {
        int position = 0;
        for (MCRCategory sibling : parent.getChildren()) {
            if (getId().equals(sibling.getId())) {
                return position;
            }
            position++;
        }
        if (LOGGER.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("List of children of parent: ");
            sb.append(parent.getId()).append('\n');
            for (MCRCategory sibling : parent.getChildren()) {
                sb.append(sibling.getId()).append('\n');
            }
            LOGGER.debug(sb.toString());

        }
        throw new IndexOutOfBoundsException(
            "Position -1 is not valid: " + getId() + " parent:" + parent.getId() + " children: " + parent
                .getChildren()
                .stream()
                .map(MCRCategory::getId)
                .map(MCRCategoryID::getId)
                .collect(Collectors.joining(", ")));
    }

    /**
     * @param children
     *            the children to set
     */
    public void setChildren(List<MCRCategory> children) {
        LOGGER.debug("Set children called for {} list '{}': {}",
            this::getId, () -> children.getClass().getName(), () -> children);
        childGuard.write(() -> setChildrenUnlocked(children));
    }

    @Override
    protected void setChildrenUnlocked(List<MCRCategory> children) {
        List<MCRCategory> newChildren = new MCRCategoryChildList(root, this);
        newChildren.addAll(children);
        this.children = newChildren;
    }

    /**
     * @param labels
     *            the labels to set
     */
    public void setLabels(SortedSet<MCRLabel> labels) {
        this.labels = labels;
    }

    /**
     * @param left
     *            the left to set
     */
    public void setLeft(int left) {
        this.left = left;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @param right
     *            the right to set
     */
    public void setRight(int right) {
        this.right = right;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.datamodel.classifications2.MCRCategory#setRoot(org.mycore.datamodel.classifications2.MCRClassificationObject)
     */
    public void setRoot(MCRCategory root) {
        this.root = root;
        if (children != null) {
            setChildren(children);
        }
    }

    static Collection<MCRCategoryImpl> wrapCategories(Collection<? extends MCRCategory> categories, MCRCategory parent,
        MCRCategory root) {
        List<MCRCategoryImpl> list = new ArrayList<>(categories.size());
        for (MCRCategory category : categories) {
            list.add(wrapCategory(category, parent, root));
        }
        return list;
    }

    static MCRCategoryImpl wrapCategory(MCRCategory category, MCRCategory parent, MCRCategory root) {
        MCRCategory rootCategory = root;
        if (category.getParent() != null && !category.getParent().equals(parent)) {
            throw new MCRException("MCRCategory is already attached to a different parent.");
        }
        if (category instanceof MCRCategoryImpl catImpl) {
            // don't use setParent() as it call add() from ChildList
            catImpl.parent = parent;
            if (root == null) {
                rootCategory = catImpl;
            }
            catImpl.setRoot(rootCategory);
            if (parent != null) {
                catImpl.level = parent.getLevel() + 1;
            } else if (category.isCategory()) {
                LOGGER.warn("Something went wrong here, category has no parent and is no root category: {}",
                    category::getId);
            }
            // copy children to temporary list
            List<MCRCategory> children = new ArrayList<>(catImpl.getChildren().size());
            children.addAll(catImpl.getChildren());
            // remove old children
            catImpl.getChildren().clear();
            // add new wrapped children
            catImpl.getChildren().addAll(children);
            return catImpl;
        }
        LOGGER.debug("wrap Category: {}", category::getId);
        MCRCategoryImpl catImpl = new MCRCategoryImpl();
        catImpl.setId(category.getId());
        catImpl.labels = category.getLabels();
        catImpl.parent = parent;

        if (root == null) {
            rootCategory = catImpl;
        }
        catImpl.setRoot(rootCategory);
        catImpl.level = parent.getLevel() + 1;
        catImpl.children = new ArrayList<>(category.getChildren().size());
        catImpl.getChildren().addAll(category.getChildren());
        return catImpl;
    }

    @Transient
    MCRCategoryImpl getLeftSiblingOrOfAncestor() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = getParent();
        if (index > 0) {
            // has left sibling
            return (MCRCategoryImpl) parent.getChildren().get(index - 1);
        }
        if (parent.getParent() != null) {
            // recursive call to get left sibling of parent
            return parent.getLeftSiblingOrOfAncestor();
        }
        return parent;// is root
    }

    @Transient
    MCRCategoryImpl getLeftSiblingOrParent() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = getParent();
        if (index == 0) {
            return parent;
        }
        return (MCRCategoryImpl) parent.getChildren().get(index - 1);
    }

    @Transient
    MCRCategoryImpl getRightSiblingOrOfAncestor() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = getParent();
        if (index + 1 < parent.getChildren().size()) {
            // has right sibling
            return (MCRCategoryImpl) parent.getChildren().get(index + 1);
        }
        if (parent.getParent() != null) {
            // recursive call to get right sibling of parent
            return parent.getRightSiblingOrOfAncestor();
        }
        return parent;// is root
    }

    @Transient
    MCRCategoryImpl getRightSiblingOrParent() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = getParent();
        if (index + 1 == parent.getChildren().size()) {
            return parent;
        }
        // get Element at index that would be at index+1 after insert
        return (MCRCategoryImpl) parent.getChildren().get(index + 1);
    }

    /**
     * calculates left and right value throug the subtree rooted at
     * <code>co</code>.
     *
     * @param leftStart
     *            this.left will be set to this value
     * @param levelStart
     *            this.getLevel() will return this value
     * @return this.right
     */
    public int calculateLeftRightAndLevel(int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        setLeft(leftStart);
        setLevel(levelStart);
        for (MCRCategory child : getChildren()) {
            LOGGER.debug(child::getId);
            curValue = ((MCRCategoryImpl) child).calculateLeftRightAndLevel(++curValue, nextLevel);
        }
        setRight(++curValue);
        return curValue;
    }

    /**
     * @param internalID
     *            the internalID to set
     */
    public void setInternalID(int internalID) {
        this.internalID = internalID;
    }

    public void setRootID(String rootID) {
        if (getId() == null) {
            setId(new MCRCategoryID(rootID));
        } else if (!getId().getRootID().equals(rootID)) {
            setId(new MCRCategoryID(rootID, getId().getId()));
        }
    }

    public void setCategID(String categID) {
        if (getId() == null) {
            setId(new MCRCategoryID(null, categID));
        } else if (!getId().getId().equals(categID)) {
            setId(new MCRCategoryID(getId().getRootID(), categID));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + internalID;
        result = prime * result + left;
        result = prime * result + level;
        result = prime * result + right;
        result = prime * result + getId().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result;
        if (obj == null || getClass() != obj.getClass()) {
            result = false;
        } else if (this == obj) {
            result = true;
        } else {
            MCRCategoryImpl other = (MCRCategoryImpl) obj;
            result =
                internalID == other.internalID && left == other.left && level == other.level && right == other.right;
        }
        return result;
    }

    @Transient
    public String getRootID() {
        return getId().getRootID();
    }

}
