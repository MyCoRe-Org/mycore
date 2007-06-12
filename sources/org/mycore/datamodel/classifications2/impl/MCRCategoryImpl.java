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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

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
public class MCRCategoryImpl extends MCRAbstractCategoryImpl {

    protected static class ChildList extends ArrayList<MCRCategory> {
        private static final long serialVersionUID = 180424337316332676L;

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
            LOGGER.info("Add Element with index");
            super.add(index, wrapCategory(element));
        }

        @Override
        public boolean add(MCRCategory e) {
            LOGGER.info("Add Element: "+e.getId()+" "+this.size(),new MCRException(""));
            return super.add(wrapCategory(e));
        }

        @Override
        public boolean addAll(Collection<? extends MCRCategory> c) {
            LOGGER.info("Add Collection with size:"+c.size());
            return super.addAll(wrapCategories(c));
        }

        @Override
        public boolean addAll(int index, Collection<? extends MCRCategory> c) {
            LOGGER.info("Add Collection with index");
            return super.addAll(index, wrapCategories(c));
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

        private Collection<MCRCategoryImpl> wrapCategories(Collection<? extends MCRCategory> categories) {
            List<MCRCategoryImpl> list = new ArrayList<MCRCategoryImpl>(categories.size());
            for (MCRCategory category : categories) {
                list.add(wrapCategory(category));
            }
            return list;
        }

        private MCRCategoryImpl wrapCategory(MCRCategory category) {
            MCRCategoryImpl catImpl;
            if (category instanceof MCRCategoryImpl) {
                catImpl = (MCRCategoryImpl) category;
                //don't use setParent() as it call add() from ChildList
                catImpl.parent = thisCategory;
                catImpl.setRoot(root);
                return catImpl;
            }
            catImpl = new MCRCategoryImpl();
            catImpl.setId(category.getId());
            catImpl.labels = category.getLabels();
            catImpl.parent = thisCategory;
            catImpl.setRoot(root);
            catImpl.getChildren().addAll(category.getChildren());
            return catImpl;
        }

    }

    private static Logger LOGGER = Logger.getLogger(MCRCategoryImpl.class);

    private int left, right, positionInParent;

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
        if (parent == null) {
            return 0;
        }
        return parent.getChildren().indexOf(this);
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
        childrenLock.writeLock().lock();
        ChildList newChildren = new ChildList(this.root, this);
        newChildren.addAll(children);
        this.children = newChildren;
        childrenLock.writeLock().unlock();
    }

    /**
     * @param labels
     *            the labels to set
     */
    public void setLabels(Collection<MCRLabel> labels) {
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
        LOGGER.warn("Attempt to modify positionInParent to: " + positionInParent + " oldValue: " + getPositionInParent());
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

}
