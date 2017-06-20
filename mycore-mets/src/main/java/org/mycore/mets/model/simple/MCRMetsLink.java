package org.mycore.mets.model.simple;

public class MCRMetsLink {

    public MCRMetsLink(MCRMetsSection from, MCRMetsPage to) {
        this.from = from;
        this.to = to;
    }

    public MCRMetsLink() {
    }

    private MCRMetsSection from;

    private MCRMetsPage to;

    public MCRMetsSection getFrom() {
        return from;
    }

    public void setFrom(MCRMetsSection from) {
        this.from = from;
    }

    public MCRMetsPage getTo() {
        return to;
    }

    public void setTo(MCRMetsPage to) {
        this.to = to;
    }
}
