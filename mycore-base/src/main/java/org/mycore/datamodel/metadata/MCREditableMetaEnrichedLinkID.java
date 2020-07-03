package org.mycore.datamodel.metadata;

import java.util.List;
import java.util.Optional;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
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
            final Element titleElement = new Element(TITLE_ELEMENT_NAME);
            Optional.ofNullable(title.getLang())
                .ifPresent(lang -> titleElement.setAttribute(LANG_ATTRIBUTE_NAME, lang, MCRConstants.XML_NAMESPACE));
            titleElement.setText(title.getText());
            return titleElement;
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
