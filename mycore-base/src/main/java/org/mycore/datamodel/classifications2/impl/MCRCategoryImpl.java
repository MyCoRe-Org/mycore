/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCRURIConverter;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
@Entity
@Table(name = "MCRCategory", indexes = {
    @Index(columnList = "ClassID, leftValue, rightValue", name = "ClassLeftRight"),
    @Index(columnList = "leftValue", name = "ClassesRoot")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = { "ClassID", "CategID" }, name = "ClassCategUnique"),
    @UniqueConstraint(columnNames = { "ClassID", "leftValue" }, name = "ClassLeftUnique"),
    @UniqueConstraint(columnNames = { "ClassID", "rightValue" }, name = "ClassRightUnique") })
@NamedQueries({
    @NamedQuery(name = "MCRCategory.updateLeft",
        query = "UPDATE MCRCategoryImpl cat SET cat.left=cat.left+:increment WHERE cat.id.rootID= :classID AND cat.left >= :left"),
    @NamedQuery(name = "MCRCategory.updateRight",
        query = "UPDATE MCRCategoryImpl cat SET cat.right=cat.right+:increment WHERE cat.id.rootID= :classID AND cat.right >= :left"),
    @NamedQuery(name = "MCRCategory.commonAncestor",
        query = "FROM MCRCategoryImpl as cat WHERE cat.id.rootID=:rootID AND cat.left < :left AND cat.right > :right ORDER BY cat.left DESC"),
    @NamedQuery(name = "MCRCategory.byNaturalId",
        query = "FROM MCRCategoryImpl as cat WHERE cat.id.rootID=:classID and (cat.id.ID=:categID OR cat.id.ID IS NULL AND :categID IS NULL)"),
    @NamedQuery(name = "MCRCategory.byLabelInClass", query = "FROM MCRCategoryImpl as cat "
        + "INNER JOIN cat.labels as label "
        + "  WHERE cat.id.rootID=:rootID AND "
        + "    cat.left BETWEEN :left and :right AND "
        + "    label.lang=:lang AND "
        + "    label.text=:text"),
    @NamedQuery(name = "MCRCategory.byLabel", query = "FROM MCRCategoryImpl as cat "
        + "  INNER JOIN cat.labels as label "
        + "  WHERE label.lang=:lang AND "
        + "    label.text=:text"),
    @NamedQuery(name = "MCRCategory.prefetchClassQuery", query = MCRCategoryDTO.SELECT
        + " WHERE cat.id.rootID=:classID ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchClassLevelQuery", query = MCRCategoryDTO.SELECT
        + " WHERE cat.id.rootID=:classID AND cat.level <= :endlevel ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchCategQuery", query = MCRCategoryDTO.SELECT
        + " WHERE cat.id.rootID=:classID AND (cat.left BETWEEN :left AND :right OR cat.left=0) ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.prefetchCategLevelQuery", query = MCRCategoryDTO.SELECT
        + " WHERE cat.id.rootID=:classID AND (cat.left BETWEEN :left AND :right OR cat.left=0) AND cat.level <= :endlevel ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.leftRightLevelQuery", query = MCRCategoryDTO.LRL_SELECT
        + " WHERE cat.id=:categID "),
    @NamedQuery(name = "MCRCategory.parentQuery", query = MCRCategoryDTO.SELECT
        + " WHERE cat.id.rootID=:classID AND (cat.left < :left AND cat.right > :right OR cat.id.ID=:categID) ORDER BY cat.left"),
    @NamedQuery(name = "MCRCategory.rootCategs", query = MCRCategoryDTO.SELECT
        + " WHERE cat.left = 0 ORDER BY cat.id.rootID"),
    @NamedQuery(name = "MCRCategory.rootIds", query = "SELECT cat.id FROM MCRCategoryImpl cat WHERE cat.left = 0"),
    @NamedQuery(name = "MCRCategory.childCount",
        query = "SELECT CAST(count(*) AS integer) FROM MCRCategoryImpl children WHERE children.parent=(SELECT cat.internalID FROM MCRCategoryImpl cat WHERE cat.id.rootID=:classID and (cat.id.ID=:categID OR cat.id.ID IS NULL AND :categID IS NULL))")
})

@Access(AccessType.PROPERTY)
public class MCRCategoryImpl extends MCRAbstractCategoryImpl implements Serializable {

    private static final long serialVersionUID = -7431317191711000317L;

    private static Logger LOGGER = LogManager.getLogger(MCRCategoryImpl.class);

    private int left, right, internalID;

    int level;

    public MCRCategoryImpl() {
    }

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

    @Column
    public int getLevel() {
        return level;
    }

    @Transient
    int getPositionInParent() {
        LOGGER.debug("getposition called for " + getId());
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
    @OneToMany(targetEntity = MCRCategoryImpl.class, cascade = {
        CascadeType.ALL }, mappedBy = "parent")
    @OrderColumn(name = "positionInParent")
    @Access(AccessType.FIELD)
    public List<MCRCategory> getChildren() {
        return super.getChildren();
    }

    @Override
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "MCRCategoryLabels", joinColumns = @JoinColumn(name = "category"), uniqueConstraints = {
        @UniqueConstraint(columnNames = { "category", "lang" }) })
    public Set<MCRLabel> getLabels() {
        return super.getLabels();
    }

    @Override
    @Column
    @Convert(converter = MCRURIConverter.class)
    public URI getURI() {
        return super.getURI();
    }

    @ManyToOne(optional = true, targetEntity = MCRCategoryImpl.class)
    @JoinColumn(name = "parentID")
    @Access(AccessType.FIELD)
    public MCRCategory getParent() {
        return super.getParent();
    }

    //End of Mapping

    @Override
    public boolean hasChildren() {
        //if children is initialized and has objects use it and don't depend on db values
        if (children != null && children.size() > 0) {
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
                .map(MCRCategoryID::getID)
                .collect(Collectors.joining(", ")));
    }

    /**
     * @param children
     *            the children to set
     */
    public void setChildren(List<MCRCategory> children) {
        LOGGER.debug("Set children called for " + getId() + "list'" + children.getClass().getName() + "': " + children);
        childGuard.write(() -> setChildrenUnlocked(children));
    }

    @Override
    protected void setChildrenUnlocked(List<MCRCategory> children) {
        MCRCategoryChildList newChildren = new MCRCategoryChildList(root, this);
        newChildren.addAll(children);
        this.children = newChildren;
    }

    /**
     * @param labels
     *            the labels to set
     */
    public void setLabels(Set<MCRLabel> labels) {
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
        List<MCRCategoryImpl> list = new ArrayList<MCRCategoryImpl>(categories.size());
        for (MCRCategory category : categories) {
            list.add(wrapCategory(category, parent, root));
        }
        return list;
    }

    static MCRCategoryImpl wrapCategory(MCRCategory category, MCRCategory parent, MCRCategory root) {
        MCRCategoryImpl catImpl;
        if (category.getParent() != null && category.getParent() != parent) {
            throw new MCRException("MCRCategory is already attached to a different parent.");
        }
        if (category instanceof MCRCategoryImpl) {
            catImpl = (MCRCategoryImpl) category;
            // don't use setParent() as it call add() from ChildList
            catImpl.parent = parent;
            if (root == null) {
                root = catImpl;
            }
            catImpl.setRoot(root);
            if (parent != null) {
                catImpl.level = parent.getLevel() + 1;
            } else if (category.isCategory()) {
                LOGGER.warn("Something went wrong here, category has no parent and is no root category: "
                    + category.getId());
            }
            // copy children to temporary list
            List<MCRCategory> children = new ArrayList<MCRCategory>(catImpl.getChildren().size());
            children.addAll(catImpl.getChildren());
            // remove old children
            catImpl.getChildren().clear();
            // add new wrapped children
            catImpl.getChildren().addAll(children);
            return catImpl;
        }
        LOGGER.debug("wrap Category: " + category.getId());
        catImpl = new MCRCategoryImpl();
        catImpl.setId(category.getId());
        catImpl.labels = category.getLabels();
        catImpl.parent = parent;
        if (root == null) {
            root = catImpl;
        }
        catImpl.setRoot(root);
        catImpl.level = parent.getLevel() + 1;
        catImpl.children = new ArrayList<MCRCategory>(category.getChildren().size());
        catImpl.getChildren().addAll(category.getChildren());
        return catImpl;
    }

    @Transient
    MCRCategoryImpl getLeftSiblingOrOfAncestor() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = (MCRCategoryImpl) getParent();
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
        MCRCategoryImpl parent = (MCRCategoryImpl) getParent();
        if (index == 0) {
            return parent;
        }
        return (MCRCategoryImpl) parent.getChildren().get(index - 1);
    }

    @Transient
    MCRCategoryImpl getRightSiblingOrOfAncestor() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = (MCRCategoryImpl) getParent();
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
        MCRCategoryImpl parent = (MCRCategoryImpl) getParent();
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
            LOGGER.debug(child.getId());
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
            setId(MCRCategoryID.rootID(rootID));
        } else if (!getId().getRootID().equals(rootID)) {
            setId(new MCRCategoryID(rootID, getId().getID()));
        }
    }

    public void setCategID(String categID) {
        if (getId() == null) {
            setId(new MCRCategoryID(null, categID));
        } else if (!getId().getID().equals(categID)) {
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MCRCategoryImpl other = (MCRCategoryImpl) obj;
        if (internalID != other.internalID)
            return false;
        if (left != other.left)
            return false;
        if (level != other.level)
            return false;
        if (right != other.right)
            return false;
        return true;
    }

    @Transient
    public String getRootID() {
        return getId().getRootID();
    }

}
