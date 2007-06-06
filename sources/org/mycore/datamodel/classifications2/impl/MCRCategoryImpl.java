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
import java.util.Set;

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
        public boolean add(MCRCategory e) {
            final MCRCategoryImpl wrappedCategory = wrapCategory(e);
            wrappedCategory.setParent(thisCategory);
            wrappedCategory.setRoot(root);
            return super.add(wrappedCategory);
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
        public MCRCategory set(int index, MCRCategory element) {
            MCRCategory category = super.set(index, element);
            if (category != element) {
                removeAncestorReferences(category);
            }
            return category;
        }

        private MCRCategoryImpl wrapCategory(MCRCategory category) {
            if (category instanceof MCRCategoryImpl) {
                return (MCRCategoryImpl) category;
            }
            MCRCategoryImpl catImpl = new MCRCategoryImpl();
            catImpl.setId(category.getId());
            catImpl.labels = category.getLabels();
            catImpl.parent = category.getParent();
            catImpl.root = category.getRoot();
            for (MCRCategory child : category.getChildren()) {
                catImpl.getChildren().add(child);
            }
            return catImpl;
        }

    }

    private int left, right;

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
        this.children = new ChildList(this.root, this);
        this.children.addAll(children);
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
