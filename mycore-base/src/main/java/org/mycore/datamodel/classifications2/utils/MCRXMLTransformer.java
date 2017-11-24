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

package org.mycore.datamodel.classifications2.utils;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

public class MCRXMLTransformer {

    public static MCRCategory getCategory(Document xml) throws URISyntaxException {
        MCRCategoryImpl category = new MCRCategoryImpl();
        category.setRoot(category);
        final String classID = xml.getRootElement().getAttributeValue("ID");
        category.setLevel(0);
        category.setId(MCRCategoryID.rootID(classID));
        setURL(xml.getRootElement(), category);
        //setChildren has to be called before setParent (below) can be called without
        //database access see: org.mycore.datamodel.classifications2.impl.MCRAbstractCategoryImpl.getChildren()
        category.setChildren(new ArrayList<>());
        buildChildCategories(classID, xml.getRootElement().getChild("categories").getChildren("category"), category);
        try {
            category.setLabels(getLabels(xml.getRootElement().getChildren("label")));
        } catch (NullPointerException | IllegalArgumentException ex) {
            throw new MCRException("Error while adding labels to classification: " + classID, ex);
        }
        return category;
    }

    public static MCRCategory buildCategory(String classID, Element e, MCRCategory parent) throws URISyntaxException {
        MCRCategoryImpl category = new MCRCategoryImpl();
        //setId must be called before setParent (info important)
        category.setId(new MCRCategoryID(classID, e.getAttributeValue("ID")));
        category.setRoot(parent.getRoot());
        category.setChildren(new ArrayList<>());
        category.setParent(parent);
        try {
            category.setLabels(getLabels(e.getChildren("label")));
        } catch (NullPointerException | IllegalArgumentException ex) {
            throw new MCRException("Error while adding labels to category: " + category.getId(), ex);
        }
        category.setLevel(parent.getLevel() + 1);
        setURL(e, category);
        buildChildCategories(classID, e.getChildren("category"), category);
        return category;
    }

    private static List<MCRCategory> buildChildCategories(String classID, List<Element> elements, MCRCategory parent)
        throws URISyntaxException {
        List<MCRCategory> children = new ArrayList<>(elements.size());
        for (Object o : elements) {
            children.add(buildCategory(classID, (Element) o, parent));
        }
        return children;
    }

    public static Set<MCRLabel> getLabels(List<Element> elements)
        throws NullPointerException, IllegalArgumentException {
        Set<MCRLabel> labels = new HashSet<>(elements.size(), 1L);
        for (Element labelElement : elements) {
            MCRLabel label = getLabel(labelElement);
            labels.add(label);
        }
        return labels;
    }

    public static MCRLabel getLabel(Element labelElement) throws NullPointerException, IllegalArgumentException {
        String lang = labelElement.getAttributeValue("lang", Namespace.XML_NAMESPACE);
        return new MCRLabel(lang, labelElement.getAttributeValue("text"),
            labelElement.getAttributeValue("description"));
    }

    private static void setURL(Element e, MCRCategory category) throws URISyntaxException {
        if (e.getChild("url") != null) {
            final String uri = e.getChild("url").getAttributeValue("href", XLINK_NAMESPACE);
            if (uri != null) {
                category.setURI(new URI(uri));
            }
        }
    }

}
