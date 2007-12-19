/**
 * $RCSfile$
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentList;

import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
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

    protected static class ChildList extends ArrayList<MCRCategory> {
        private static final long serialVersionUID = 5844882597476033744L;

        private MCRCategory root;

        private MCRCategory thisCategory;

        /**
         * @param root
         * @param thisCategory
         */
        public ChildList(MCRCategory root, MCRCategory thisCategory) {
            super();
            this.root = root;
            this.thisCategory = thisCategory;
        }

        @Override
        public void add(int index, MCRCategory element) {
            super.add(index, wrapCategory(element, thisCategory, root));
        }

        @Override
        public boolean add(MCRCategory e) {
            return super.add(wrapCategory(e, thisCategory, root));
        }

        @Override
        public boolean addAll(Collection<? extends MCRCategory> c) {
            return super.addAll(wrapCategories(c, thisCategory, root));
        }

        @Override
        public boolean addAll(int index, Collection<? extends MCRCategory> c) {
            return super.addAll(index, wrapCategories(c, thisCategory, root));
        }

        @Override
        public void clear() {
            for (int i = 0; i < size(); i++) {
                removeAncestorReferences(get(i));
            }
            super.clear();
        }

        @Override
        public MCRCategory remove(int index) {
            MCRCategory category = super.remove(index);
            removeAncestorReferences(category);
            return category;
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = super.remove(o);
            if (removed) {
                removeAncestorReferences((MCRCategory) o);
            }
            return removed;
        }

        /**
         * @param category
         */
        private void removeAncestorReferences(MCRCategory category) {
            if (category instanceof MCRAbstractCategoryImpl) {
                ((MCRAbstractCategoryImpl) category).parent = null;
                ((MCRAbstractCategoryImpl) category).root = null;
            }
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                removeAncestorReferences(get(i));
            }
            super.removeRange(fromIndex, toIndex);
        }

        @Override
        public MCRCategory set(int index, MCRCategory element) {
            MCRCategory category = super.set(index, element);
            if (category != element) {
                removeAncestorReferences(category);
            }
            return category;
        }

    }

    private static Logger LOGGER = Logger.getLogger(MCRCategoryImpl.class);

    private int left, right, positionInParent, internalID;

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

    /**
     * @return the positionInParent
     */
    public int getPositionInParent() {
        LOGGER.debug("getposition called for " + getId() + " with: " + this.positionInParent);
        if (parent == null) {
            LOGGER.debug("getposition called with no parent set.");
            return positionInParent;
        }
        try {
            int position = parent.getChildren().indexOf(this);
            if (position == -1) {
                // sometimes indexOf does not find this instance so we need to
                // check for the ID here to
                position = 0;
                for (MCRCategory sibling : parent.getChildren()) {
                    if (sibling.getId().equals(getId()))
                        return position;
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
            return position;
        } catch (RuntimeException e) {
            LOGGER.error("Cannot use parent.getChildren() here", e);
            throw e;
        }
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
    public void setChildren(List<MCRCategory> children) {
        LOGGER.debug("Set children called for " + getId() + "list'" + children.getClass().getName() + "': " + children);
        childrenLock.writeLock().lock();
        if (children instanceof PersistentList) {
            this.children = children;
        } else {
            ChildList newChildren = new ChildList(this.root, this);
            newChildren.addAll(children);
            this.children = newChildren;
        }
        childrenLock.writeLock().unlock();
    }

    /**
     * @param labels
     *            the labels to set
     */
    public void setLabels(Map<String, MCRLabel> labels) {
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
        LOGGER.debug("Set position called for " + getId() + " with: " + positionInParent + " was: " + this.positionInParent);
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

    static Collection<MCRCategoryImpl> wrapCategories(Collection<? extends MCRCategory> categories, MCRCategory parent, MCRCategory root) {
        List<MCRCategoryImpl> list = new ArrayList<MCRCategoryImpl>(categories.size());
        for (MCRCategory category : categories) {
            list.add(wrapCategory(category, parent, root));
        }
        return list;
    }

    static MCRCategoryImpl wrapCategory(MCRCategory category, MCRCategory parent, MCRCategory root) {
        MCRCategoryImpl catImpl;
        if (category.getParent()!=null && category.getParent()!=parent){
            throw new MCRException("MCRCategory is already attached to a different parent.");
        }
        if (category instanceof MCRCategoryImpl) {
            catImpl = (MCRCategoryImpl) category;
            // don't use setParent() as it call add() from ChildList
            catImpl.parent = parent;
            catImpl.setRoot(root);
            if (parent != null) {
                catImpl.level = parent.getLevel() + 1;
            } else if (category.isCategory()) {
                LOGGER.warn("Something went wrong here, category has no parent and is no root category: " + category.getId());
            }
            //copy children to temporary list
            List<MCRCategory> children=new ArrayList<MCRCategory>(catImpl.getChildren().size());
            children.addAll(catImpl.getChildren());
            //remove old children
            catImpl.getChildren().clear();
            //add new wrapped children
            catImpl.getChildren().addAll(wrapCategories(children, catImpl, root));
            return catImpl;
        }
        LOGGER.debug("wrap Category: " + category.getId());
        catImpl = new MCRCategoryImpl();
        catImpl.setId(category.getId());
        catImpl.labels = category.getLabels();
        catImpl.parent = parent;
        catImpl.setRoot(root);
        catImpl.level = parent.getLevel() + 1;
        catImpl.children = new ArrayList<MCRCategory>(category.getChildren().size());
        catImpl.getChildren().addAll(wrapCategories(category.getChildren(), catImpl, root));
        return catImpl;
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

}
