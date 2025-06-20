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

import java.util.ArrayList;
import java.util.List;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MCRClassCategory",
    propOrder = {
        "label",
        "url",
        "category"
    })
@XmlSeeAlso({ MCRLabel.class, MCRClassURL.class })
@XmlRootElement(name = "category")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "ID", "url", "labels", "categories" })
public class MCRClassCategory {

    @XmlElement(required = true)
    protected List<MCRLabel> label;

    protected MCRClassURL url;

    protected List<MCRClassCategory> category;

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
    public List<MCRClassCategory> getCategory() {
        if (category == null) {
            category = new ArrayList<>();
        }
        return this.category;
    }

    @JsonGetter
    public String getID() {
        return id;
    }

    public void setID(String value) {
        this.id = value;
    }

    public static MCRClassCategory ofCategory(MCRCategory from) {
        MCRClassCategory categ = new MCRClassCategory();
        MCRCategoryID categoryID = from.getId();
        categ.setID(categoryID.isRootID() ? categoryID.getRootID() : categoryID.getId());
        categ.setUrl(MCRClassURL.ofUri(from.getURI()));
        categ.getCategory().addAll(
            from.getChildren()
                .stream()
                .map(MCRClassCategory::ofCategory)
                .toList());
        categ.getLabel().addAll(
            from.getLabels()
                .stream()
                .map(MCRLabel::clone)
                .toList());
        return categ;
    }

}
