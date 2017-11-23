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

import java.util.ArrayList;
import java.util.Collection;

import org.mycore.datamodel.classifications2.MCRCategory;

class MCRCategoryChildList extends ArrayList<MCRCategory> {
    private static final long serialVersionUID = 5844882597476033744L;

    private MCRCategory root;

    private MCRCategory thisCategory;

    public MCRCategoryChildList(MCRCategory root, MCRCategory thisCategory) {
        super();
        this.root = root;
        this.thisCategory = thisCategory;
    }

    @Override
    public void add(int index, MCRCategory element) {
        super.add(index, MCRCategoryImpl.wrapCategory(element, thisCategory, root));
    }

    @Override
    public boolean add(MCRCategory e) {
        return super.add(MCRCategoryImpl.wrapCategory(e, thisCategory, root));
    }

    @Override
    public boolean addAll(Collection<? extends MCRCategory> c) {
        return super.addAll(MCRCategoryImpl.wrapCategories(c, thisCategory, root));
    }

    @Override
    public boolean addAll(int index, Collection<? extends MCRCategory> c) {
        return super.addAll(index, MCRCategoryImpl.wrapCategories(c, thisCategory, root));
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
