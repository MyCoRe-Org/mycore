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
package org.mycore.datamodel.classifications2.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;

public class MCRXMLTransformer {

    public static MCRCategory getCategory(Document xml) {
        MCRCategoryImpl category = new MCRCategoryImpl();
        category.setRoot(category);
        final String classID = xml.getRootElement().getAttributeValue("ID");
        category.setLevel(0);
        category.setId(MCRCategoryID.rootID(classID));
        category.setChildren(getChildCategories(classID, xml.getRootElement().getChild("categories").getChildren("category"), 1));
        category.setLabels(getLabel(xml.getRootElement().getChildren("label")));
        return category;
    }

    private static MCRCategory getCategory(String classID, Element e, int level) {
        MCRCategoryImpl category = new MCRCategoryImpl();
        category.setId(new MCRCategoryID(classID, e.getAttributeValue("ID")));
        category.setLabels(getLabel(e.getChildren("label")));
        category.setLevel(level);
        category.setChildren(getChildCategories(classID, e.getChildren("category"), level + 1));
        return category;
    }

    @SuppressWarnings("unchecked")
    private static List<MCRCategory> getChildCategories(String classID, List elements, int level) {
        List<MCRCategory> children = new ArrayList<MCRCategory>(elements.size());
        for (Object o : elements) {
            children.add(getCategory(classID, (Element) o, level));
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MCRLabel> getLabel(List elements) {
        Map<String, MCRLabel> labels = new HashMap<String, MCRLabel>(elements.size(), 1l);
        for (Object o : elements) {
            Element e = (Element) o;
            String lang = e.getAttributeValue("lang", Namespace.XML_NAMESPACE);
            labels.put(lang,new MCRLabel(lang, e.getAttributeValue("text"), e.getAttributeValue("description")));
        }
        return labels;
    }

}
