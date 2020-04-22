package org.mycore.restapi.v2.model;

import java.util.ArrayList;
import java.util.List;

public class MCRRestResponseHeader {
    private int status;

    private List<MCRRestLink> links = new ArrayList<>();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<MCRRestLink> getLinks() {
        return links;
    }

    public void addLink(String rel, String url) {
        links.add(new MCRRestLink(rel, url));
    }
}
