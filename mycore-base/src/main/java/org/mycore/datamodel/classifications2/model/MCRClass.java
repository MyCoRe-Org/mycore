
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

package org.mycore.datamodel.classifications2.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@XmlRootElement(name = "mycoreclass")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MCRClass",
    propOrder = {
        "label",
        "url",
        "categories"
    })
@XmlSeeAlso({ MCRClassCategory.class, MCRClassURL.class, MCRLabel.class })
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "ID", "url", "labels", "categories" })
public class MCRClass {

    @XmlElement(required = true)
    protected List<MCRLabel> label;

    protected MCRClassURL url;

    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    protected List<MCRClassCategory> categories;

    @XmlAttribute(name = "ID", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String id;

    @JsonGetter("labels")
    public List<MCRLabel> getLabel() {
        if (label == null) {
            label = new ArrayList<>();
        }
        return this.label;
    }

    public MCRClassURL getUrl() {
        return url;
    }

    public void setUrl(MCRClassURL value) {
        this.url = value;
    }

    @JsonGetter("categories")
    public List<MCRClassCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<MCRClassCategory> value) {
        this.categories = value;
    }

    @JsonGetter
    public String getID() {
        return id;
    }

    public void setID(String value) {
        this.id = value;
    }

    public static MCRClass ofCategory(MCRCategory rootCategory) {
        if (!rootCategory.getId().isRootID()) {
            throw new IllegalArgumentException("Not a root category");
        }
        MCRClass mcrClass = new MCRClass();
        mcrClass.setID(rootCategory.getId().getRootID());
        mcrClass.setUrl(MCRClassURL.ofUri(rootCategory.getURI()));
        mcrClass.getLabel().addAll(
            rootCategory.getLabels()
                .stream()
                .map(MCRLabel::clone)
                .toList());
        mcrClass.setCategories(rootCategory.getChildren()
            .stream()
            .map(MCRClassCategory::ofCategory)
            .toList());
        return mcrClass;
    }

    public MCRCategory toCategory() {
        MCRCategoryImpl category = new MCRCategoryImpl();
        category.setId(new MCRCategoryID(getID()));
        category.setLevel(0);
        URI uri = Optional.ofNullable(getUrl())
            .map(MCRClassURL::getHref)
            .map(URI::create)
            .orElse(null);
        category.setURI(uri);
        category.getLabels()
            .addAll(getLabel().stream()
                .map(MCRLabel::clone)
                .toList());
        category.setChildren(new ArrayList<>());
        this.getCategories()
                .forEach((c) -> buildCategory(getID(), c, category));

        return category;
    }

    public static MCRCategory buildCategory(String classID, MCRClassCategory e, MCRCategory parent) {
        MCRCategoryImpl category = new MCRCategoryImpl();
        //setId must be called before setParent (info important)
        category.setId(new MCRCategoryID(classID, e.getID()));
        category.setRoot(parent.getRoot());
        category.setChildren(new ArrayList<>());
        category.setParent(parent);
        category.getLabels()
            .addAll(e.getLabel().stream()
                .map(MCRLabel::clone)
                .toList());
        category.setLevel(parent.getLevel() + 1);
        URI uri = Optional.ofNullable(e.getUrl())
            .map(MCRClassURL::getHref)
            .map(URI::create)
            .orElse(null);
        category.setURI(uri);
        for (MCRClassCategory child : e.getCategory()) {
            buildCategory(classID, child, category);
        }
        return category;
    }

}
