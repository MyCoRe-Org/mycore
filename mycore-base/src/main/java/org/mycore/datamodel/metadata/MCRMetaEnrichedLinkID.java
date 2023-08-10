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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A Link to a {@link MCRDerivate}. In addition to {@link MCRMetaLink} this class contains information about the
 * linked {@link MCRBase} like mainDoc, titles and classifications in {@link MCRDerivate}.
 * See also {@link MCREditableMetaEnrichedLinkID}
 */
@JsonClassDescription("Links to derivates")
public class MCRMetaEnrichedLinkID extends MCRMetaLinkID {

    protected static final String ORDER_ELEMENT_NAME = "order";

    protected static final String MAIN_DOC_ELEMENT_NAME = "maindoc";

    protected static final String CLASSIFICATION_ELEMENT_NAME = "classification";

    protected static final String CLASSID_ATTRIBUTE_NAME = "classid";

    protected static final String CATEGID_ATTRIBUTE_NAME = "categid";

    protected static final String TITLE_ELEMENT_NAME = "title";

    protected static final String LANG_ATTRIBUTE_NAME = "lang";

    private static final List<String> ORDER = List.of(ORDER_ELEMENT_NAME, MAIN_DOC_ELEMENT_NAME, TITLE_ELEMENT_NAME,
        CLASSIFICATION_ELEMENT_NAME);

    private List<Content> contentList;

    public MCRMetaEnrichedLinkID() {
        setContentList(new ArrayList<>());
    }

    public static MCRMetaEnrichedLinkID fromDom(Element element) {
        final MCRMetaEnrichedLinkID mcrMetaEnrichedLinkID = new MCRMetaEnrichedLinkID();
        mcrMetaEnrichedLinkID.setFromDOM(element);
        return mcrMetaEnrichedLinkID;
    }

    private static int getElementPosition(Element e) {
        final int index = ORDER.indexOf(e.getName());
        return index < 0 ? ORDER.size() : index;
    }

    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);

        contentList = element.getContent().stream().map(Content::clone).collect(Collectors.toList());
    }

    @Override
    public Element createXML() throws MCRException {
        final Element xml = super.createXML();

        contentList.stream().map(Content::clone).forEach(xml::addContent);

        xml.sortChildren(
            Comparator.comparingInt(MCRMetaEnrichedLinkID::getElementPosition)
                .thenComparing(contentList::indexOf));

        return xml;
    }

    @JsonIgnore
    public List<Content> getContentList() {
        return contentList;
    }

    public void setContentList(List<Content> contentList) {
        this.contentList = Objects.requireNonNull(contentList);
    }

    public int getOrder() {
        return elementsWithNameFromContentList(ORDER_ELEMENT_NAME)
            .findFirst()
            .map(Element::getTextNormalize)
            .map(Integer::valueOf)
            .orElse(1);
    }

    public String getMainDoc() {
        return elementsWithNameFromContentList(MAIN_DOC_ELEMENT_NAME)
            .findFirst()
            .map(Element::getTextTrim)
            .orElse(null);
    }

    public List<MCRCategoryID> getClassifications() {
        return elementsWithNameFromContentList(CLASSIFICATION_ELEMENT_NAME)
            .map(el -> new MCRCategoryID(el.getAttributeValue(CLASSID_ATTRIBUTE_NAME),
                el.getAttributeValue(CATEGID_ATTRIBUTE_NAME)))
            .collect(Collectors.toList());
    }

    public List<MCRMetaLangText> getTitle() {
        return elementsWithNameFromContentList(TITLE_ELEMENT_NAME)
            .map(el -> {
                MCRMetaLangText mlt = new MCRMetaLangText();
                mlt.setFromDOM(el);
                return mlt;
            })
            .collect(Collectors.toList());
    }

    public Stream<Element> elementsWithNameFromContentList(String name) {
        return getContentList().stream()
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .filter(el -> el.getName().equals(name));
    }

    @Override
    public JsonObject createJSON() {
        final JsonObject json = super.createJSON();
        json.addProperty(ORDER_ELEMENT_NAME, getOrder());
        json.addProperty(MAIN_DOC_ELEMENT_NAME, getMainDoc());
        final List<MCRMetaLangText> title = getTitle();
        JsonArray titles = new JsonArray(title.size());
        title.stream().forEach(t -> titles.add(t.createJSON()));
        json.add("titles", titles);
        final List<MCRCategoryID> categories = getClassifications();
        JsonArray classifications = new JsonArray(categories.size());
        categories.stream().map(MCRCategoryID::toString).forEach(classifications::add);
        json.add("classifications", classifications);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        MCRMetaEnrichedLinkID that = (MCRMetaEnrichedLinkID) o;
        final List<Content> myContentList = getContentList();
        final List<Content> theirContentList = that.getContentList();
        final int listSize = myContentList.size();
        if (listSize != theirContentList.size()) {
            return false;
        }
        for (int i = 0; i < listSize; i++) {
            Content myContent = myContentList.get(i);
            Content theirContent = theirContentList.get(i);
            if (!myContent.equals(theirContent) || (myContent instanceof Element && theirContent instanceof Element
                && !MCRXMLHelper.deepEqual((Element) myContent, (Element) theirContent))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getContentList());
    }
}
