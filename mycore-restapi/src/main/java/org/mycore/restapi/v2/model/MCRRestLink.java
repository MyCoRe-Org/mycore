package org.mycore.restapi.v2.model;

public class MCRRestLink {
    String rel;
    String url;

    public MCRRestLink(String rel, String url) {
        this.rel = rel;
        this.url = url;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
