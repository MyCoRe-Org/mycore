package org.mycore.mets.model.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MCRMetsSection {

    public MCRMetsSection() {
        this.metsSectionList = new ArrayList<>();
        this.altoLinks = new ArrayList<>();
    }

    public MCRMetsSection(String type, String label, MCRMetsSection parent) {
        this();
        this.type = type;
        this.label = label;
        this.parent = parent;
    }

    private List<MCRMetsSection> metsSectionList;

    private String id = UUID.randomUUID().toString();

    private String type;

    private String label;

    private List<MCRMetsAltoLink> altoLinks;

    private transient MCRMetsSection parent;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<MCRMetsSection> getMetsSectionList() {
        return Collections.unmodifiableList(metsSectionList);
    }

    public void addSection(MCRMetsSection section) {
        section.parent = this;
        this.metsSectionList.add(section);
    }

    public void removeSection(MCRMetsSection section) {
        section.parent = null;
        this.metsSectionList.remove(section);
    }

    public MCRMetsSection getParent() {
        return parent;
    }

    public void setParent(MCRMetsSection parent) {
        this.parent = parent;
    }

    public List<MCRMetsAltoLink> getAltoLinks() {
        return Collections.unmodifiableList(altoLinks);
    }

    public void setAltoLinks(List<MCRMetsAltoLink> altoLinks) {
        this.altoLinks = altoLinks;
    }

    public void addAltoLink(MCRMetsAltoLink link) {
        this.altoLinks.add(link);
    }

    public void removeAltoLink(MCRMetsAltoLink link) {
        this.altoLinks.remove(link);
    }

}
