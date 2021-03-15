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

package org.mycore.datamodel.metadata;

import java.util.List;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategoryID;

public class MCREditableMetaEnrichedLinkID extends MCRMetaEnrichedLinkID {

    public void setOrder(int order) {
        setOrCreateElement(ORDER_ELEMENT_NAME, String.valueOf(order));
    }

    public void setMainDoc(String mainDoc) {
        setOrCreateElement(MAIN_DOC_ELEMENT_NAME, mainDoc);
    }

    public void setClassifications(List<MCRCategoryID> list) {
        elementsWithNameFromContentList(CLASSIFICATION_ELEMENT_NAME).forEach(getContentList()::remove);
        list.stream().map(clazz -> {
            final Element classElement = new Element(CLASSIFICATION_ELEMENT_NAME);
            classElement.setAttribute(CLASSID_ATTRIBUTE_NAME, clazz.getRootID());
            classElement.setAttribute(CATEGID_ATTRIBUTE_NAME, clazz.getID());
            return classElement;
        }).forEach(getContentList()::add);
    }

    public void setTitles(List<MCRMetaLangText> titles) {
        elementsWithNameFromContentList(TITLE_ELEMENT_NAME).forEach(getContentList()::remove);
        titles.stream().map(title -> {
            return title.createXML();
        }).forEach(getContentList()::add);
    }

    protected void setOrCreateElement(String elementName, String textContent) {
        elementsWithNameFromContentList(elementName)
            .findFirst()
            .orElseGet(() -> createNewElement(elementName))
            .setText(textContent);
    }

    protected Element createNewElement(String name) {
        final List<Content> contentList = getContentList();
        Element orderElement = new Element(name);
        contentList.add(orderElement);
        return orderElement;
    }

}
