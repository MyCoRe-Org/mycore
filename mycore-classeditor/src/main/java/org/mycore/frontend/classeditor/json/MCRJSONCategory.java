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

package org.mycore.frontend.classeditor.json;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

/**
 * GSON Category abstraction.
 *
 * @author Chi
 */
public class MCRJSONCategory implements MCRCategory {

    private final MCRCategoryImpl category;

    private Boolean hasChildren;

    private int positionInParent;

    private MCRCategoryID parentID;

    public MCRJSONCategory() {
        category = new MCRCategoryImpl();
    }

    public MCRJSONCategory(MCRCategory category) {
        this.category = (MCRCategoryImpl) category;
    }

    public void setParent(MCRCategory parent) {
        category.setParent(parent);
    }

    public void setChildren(List<MCRCategory> children) {
        category.setChildren(children);
    }

    public int getLeft() {
        return category.getLeft();
    }

    @Override
    public int getLevel() {
        return category.getLevel();
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    @Override
    public boolean hasChildren() {
        if (hasChildren == null) {
            hasChildren = category.hasChildren();
        }
        return hasChildren;
    }

    @Override
    public List<MCRCategory> getChildren() {
        return category.getChildren();
    }

    public int getPositionInParent() {
        return positionInParent;
    }

    public void setPositionInParent(int positionInParent) {
        this.positionInParent = positionInParent;
    }

    @Override
    public MCRCategoryID getId() {
        return category.getId();
    }

    @Override
    public SortedSet<MCRLabel> getLabels() {
        return category.getLabels();
    }

    @Override
    public MCRCategory getRoot() {
        return category.getRoot();
    }

    @Override
    public java.net.URI getURI() {
        return category.getURI();
    }

    @Override
    public void setId(MCRCategoryID id) {
        category.setId(id);
    }

    @Override
    public void setURI(java.net.URI uri) {
        category.setURI(uri);
    }

    @Override
    public MCRCategory getParent() {
        return category.getParent();
    }

    @Override
    public Optional<MCRLabel> getCurrentLabel() {
        return category.getCurrentLabel();
    }

    public void setLabels(SortedSet<MCRLabel> labels) {
        category.setLabels(labels);
    }

    @Override
    public Optional<MCRLabel> getLabel(String lang) {
        return category.getLabel(lang);
    }

    public void setParentID(MCRCategoryID parentID) {
        this.parentID = parentID;
    }

    public MCRCategoryID getParentID() {
        return parentID;
    }

    @Override
    public boolean isClassification() {
        return category.isClassification();
    }

    @Override
    public boolean isCategory() {
        return category.isCategory();
    }

    public MCRCategoryImpl asMCRImpl() {
        return category;
    }

}
