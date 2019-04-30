package org.mycore.datamodel.metadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;

public class MCRMetaDerivateLinkID extends MCRMetaLinkID {

    private List<Content> contentList;

    MCRMetaDerivateLinkID() {
    }

    public static MCRMetaDerivateLinkID fromDom(Element element){
        final MCRMetaDerivateLinkID mcrMetaDerivateLinkID = new MCRMetaDerivateLinkID();
        mcrMetaDerivateLinkID.setFromDOM(element);
        return mcrMetaDerivateLinkID;
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

        MCRMetaDerivateLinkID that = (MCRMetaDerivateLinkID) o;

        return Objects.equals(getContentList(), that.getContentList());
    }

    @Override public int hashCode() {
        return Objects.hash(super.hashCode(), getContentList());
    }
}
