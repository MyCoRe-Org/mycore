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

package org.mycore.datamodel.classifications2.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MCRClassCategory",
    propOrder = {
        "label",
        "url",
        "category"
    })
@XmlSeeAlso({ MCRLabel.class, MCRClassURL.class })
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class MCRClassCategory {

    @XmlElement(required = true)
    @JsonProperty("labels")
    protected List<MCRLabel> label;

    protected MCRClassURL url;

    protected List<MCRClassCategory> category;

    @XmlAttribute(name = "ID", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String id;

    public List<MCRLabel> getLabel() {
        if (label == null) {
            label = new ArrayList<MCRLabel>();
        }
        return this.label;
    }

    public MCRClassURL getUrl() {
        return url;
    }

    public void setUrl(MCRClassURL value) {
        this.url = value;
    }

    @JsonProperty("categories")
    public List<MCRClassCategory> getCategory() {
        if (category == null) {
            category = new ArrayList<MCRClassCategory>();
        }
        return this.category;
    }

    public String getID() {
        return id;
    }

    public void setID(String value) {
        this.id = value;
    }

    public static MCRClassCategory getInstance(MCRCategory from) {
        MCRClassCategory categ = new MCRClassCategory();
        MCRCategoryID categoryID = from.getId();
        categ.setID(categoryID.isRootID() ? categoryID.getRootID() : categoryID.getID());
        categ.setUrl(MCRClassURL.getInstance(from.getURI()));
        categ.getCategory()
            .addAll(getInstance(from.getChildren()));
        categ.getLabel()
            .addAll(
                from.getLabels()
                    .stream()
                    .map(MCRLabel::clone)
                    .collect(Collectors.toList()));
        return categ;
    }

    public static List<MCRClassCategory> getInstance(List<MCRCategory> children) {
        return children
            .stream()
            .map(MCRClassCategory::getInstance)
            .collect(Collectors.toList());
    }

}
