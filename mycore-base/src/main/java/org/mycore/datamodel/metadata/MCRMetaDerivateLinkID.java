package org.mycore.datamodel.metadata;

import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;

public class MCRMetaDerivateLinkID extends MCRMetaLinkID {

    public MCRMetaDerivateLinkID() {
    }

    public MCRMetaDerivateLinkID(String set_subtag, int set_inherted) {
        super(set_subtag, set_inherted);
    }

    public MCRMetaDerivateLinkID(String set_subtag, MCRObjectID id, String label, String title) {
        super(set_subtag, id, label, title);
    }

    public MCRMetaDerivateLinkID(String set_subtag, MCRObjectID id, String label, String title, String role) {
        super(set_subtag, id, label, title, role);
    }

    public MCRMetaDerivateLinkID(MCRMetaLinkID old, String mainDoc, List<Content> contentList) {
        setFromDOM(old.createXML());
        this.mainDoc = mainDoc;
        this.contentList = contentList;
    }

    private String mainDoc;

    private List<Content> contentList;

    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);

        contentList = element.getContent().stream().map(Content::clone).collect(Collectors.toList());
        mainDoc = element.getAttributeValue("mainDoc");
    }

    @Override public Element createXML() throws MCRException {
        final Element xml = super.createXML();

        if(contentList!=null){
            contentList.stream().map(Content::clone).forEach(xml::addContent);
        }

        if(this.mainDoc != null){
            xml.setAttribute("mainDoc", this.mainDoc);
        }

        return xml;
    }

    public void setMainDoc(String mainDoc) {
        this.mainDoc = mainDoc;
    }

    public String getMainDoc() {
        return mainDoc;
    }

    public void setContentList(List<Content> contentList) {
        this.contentList = contentList;
    }

    public List<Content> getContentList() {
        return contentList;
    }
}
