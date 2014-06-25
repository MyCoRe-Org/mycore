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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.collection.internal.PersistentList;
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
public class MCRCategoryImpl extends MCRAbstractCategoryImpl implements Serializable {

    private static final long serialVersionUID = -7431317191711000317L;

    private static Logger LOGGER = Logger.getLogger(MCRCategoryImpl.class);

    private int left, right, positionInParent = -1, internalID;

    int level;

    public MCRCategoryImpl() {
    }

    /**
     * @return the left
     */
    public int getLeft() {
        return left;
    }

    public int getLevel() {
        return level;
    }

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

    /**
    * @return the positionInParent
    */
    public int getPositionInParent() {
        LOGGER.debug("getposition called for " + getId() + " with: " + positionInParent);
        if (parent == null) {
            LOGGER.debug("getposition called with no parent set.");
            return positionInParent;
        }
        try {
            int position = parent.getChildren().indexOf(this);
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
        throw new IndexOutOfBoundsException("Position -1 is not valid.");
    }

    /**
     * @return the right
     */
    public int getRight() {
        return right;
    }

    /**
     * @param children
     *            the children to set
     */
    @Override
    public void setChildren(List<MCRCategory> children) {
        LOGGER.debug("Set children called for " + getId() + "list'" + children.getClass().getName() + "': " + children);
        childrenLock.writeLock().lock();
        if (children instanceof PersistentList) {
            this.children = children;
        } else {
            MCRCategoryChildList newChildren = new MCRCategoryChildList(root, this);
            newChildren.addAll(children);
            this.children = newChildren;
        }
        childrenLock.writeLock().unlock();
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
     * @param positionInParent
     *            the positionInParent to set
     */
    public void setPositionInParent(int positionInParent) {
        LOGGER.debug("Set position called for " + getId() + " with: " + positionInParent + " was: "
            + this.positionInParent);
        this.positionInParent = positionInParent;
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

    MCRCategoryImpl getLeftSiblingOrParent() {
        int index = getPositionInParent();
        MCRCategoryImpl parent = (MCRCategoryImpl) getParent();
        if (index == 0) {
            return parent;
        }
        return (MCRCategoryImpl) parent.getChildren().get(index - 1);
    }

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
     * @return the internalID
     */
    public int getInternalID() {
        return internalID;
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

    public String getRootID() {
        return getId() == null ? null : getId().getRootID();
    }

    public String getCategID() {
        return getId() == null ? null : getId().getID();
    }

}
