package org.mycore.datamodel.metadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Link to a {@link MCRDerivate}. In addition to {@link MCRMetaLink} this class contains information about the
 * linked {@link MCRBase} like mainDoc, titles and classifications in {@link MCRDerivate}.
 */
@JsonClassDescription("Links to derivates")
public class MCRMetaEnrichedLinkID extends MCRMetaLinkID {

    private List<Content> contentList;

    public MCRMetaEnrichedLinkID() {
    }

    public static MCRMetaEnrichedLinkID fromDom(Element element){
        final MCRMetaEnrichedLinkID mcrMetaEnrichedLinkID = new MCRMetaEnrichedLinkID();
        mcrMetaEnrichedLinkID.setFromDOM(element);
        return mcrMetaEnrichedLinkID;
    }

    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);

        contentList = element.getContent().stream().map(Content::clone).collect(Collectors.toList());
    }

    @Override public Element createXML() throws MCRException {
        final Element xml = super.createXML();

        if (contentList != null) {
            contentList.stream().map(Content::clone).forEach(xml::addContent);
        }

        return xml;
    }

    @JsonIgnore
    public List<Content> getContentList() {
        return contentList;
    }

    public void setContentList(List<Content> contentList) {
        this.contentList = contentList;
    }


    public int getOrder(){
        return getContentList().stream()
            .filter(el-> el instanceof Element)
            .map(Element.class::cast)
            .filter(el-> "order".equals(el.getName()))
            .findFirst()
            .map(Element::getTextNormalize)
            .map(Integer::valueOf)
            .orElse(1);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o))
            return false;

        MCRMetaEnrichedLinkID that = (MCRMetaEnrichedLinkID) o;

        return Objects.equals(getContentList(), that.getContentList());
    }

    @Override public int hashCode() {
        return Objects.hash(super.hashCode(), getContentList());
    }
}
