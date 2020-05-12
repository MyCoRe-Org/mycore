package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

    private static final List<String> ORDER;

    static {
        ORDER =Stream.of(ORDER_ELEMENT_NAME,
            MAIN_DOC_ELEMENT_NAME, TITLE_ELEMENT_NAME, CLASSIFICATION_ELEMENT_NAME)
            .collect(Collectors.toList());
        Collections.reverse(ORDER);
    }

    private List<Content> contentList;

    public MCRMetaEnrichedLinkID() {
        setContentList(new ArrayList<>());
    }

    public static MCRMetaEnrichedLinkID fromDom(Element element) {
        final MCRMetaEnrichedLinkID mcrMetaEnrichedLinkID = new MCRMetaEnrichedLinkID();
        mcrMetaEnrichedLinkID.setFromDOM(element);
        return mcrMetaEnrichedLinkID;
    }

    private static int compareChildren(Element el1, Element el2) {
        return ORDER.indexOf(el2.getName()) - ORDER.indexOf(el1.getName());
    }

    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);

        contentList = element.getContent().stream().map(Content::clone).collect(Collectors.toList());
    }

    @Override
    public Element createXML() throws MCRException {
        final Element xml = super.createXML();

        if (contentList != null) {
            contentList.stream().map(Content::clone).forEach(xml::addContent);
        }

        xml.sortChildren(MCRMetaEnrichedLinkID::compareChildren);

        return xml;
    }

    @JsonIgnore
    public List<Content> getContentList() {
        return contentList;
    }

    public void setContentList(List<Content> contentList) {
        this.contentList = contentList;
    }

    public int getOrder() {
        return elementsWithNameFromContentList(ORDER_ELEMENT_NAME)
            .findFirst()
            .map(Element::getTextNormalize)
            .map(Integer::valueOf)
            .orElse(1);
    }

    public List<MCRMetaClassification> getClassifications() {
        return elementsWithNameFromContentList(ORDER_ELEMENT_NAME)
            .map(el -> new MCRMetaClassification("classification",
                0,
                null,
                el.getAttributeValue(CLASSID_ATTRIBUTE_NAME),
                el.getAttributeValue(CATEGID_ATTRIBUTE_NAME)))
            .collect(Collectors.toList());
    }

    public List<MCRMetaLangText> getTitle() {
        return elementsWithNameFromContentList(TITLE_ELEMENT_NAME)
            .map(el -> new MCRMetaLangText(TITLE_ELEMENT_NAME, el.getAttributeValue(LANG_ATTRIBUTE_NAME), null, 0, null,
                title))
            .collect(Collectors.toList());
    }

    protected Stream<Element> elementsWithNameFromContentList(String name) {
        return getContentList().stream()
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .filter(el -> el.getName().equals(name));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        MCRMetaEnrichedLinkID that = (MCRMetaEnrichedLinkID) o;

        return Objects.equals(getContentList(), that.getContentList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getContentList());
    }
}
