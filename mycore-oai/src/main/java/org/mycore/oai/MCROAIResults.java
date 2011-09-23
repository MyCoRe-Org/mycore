package org.mycore.oai;

import java.util.Date;
import java.util.List;

import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

public class MCROAIResults extends MCRResults {

    protected Date expirationDate;

    protected MetadataFormat metadataFormat;

    public MCROAIResults(Date expirationDate, MetadataFormat format) {
        this.expirationDate = expirationDate;
        this.metadataFormat = format;
    }

    boolean isExpired() {
        return (new Date().compareTo(expirationDate) > 0);
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public void add(MCRResults results) {
        for(MCRHit hit : results) {
            this.addHit(hit);
        }
    }

    public void add(List<String> idList) {
        for(String id : idList) {
            this.addHit(new MCRHit(id));
        }
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

}
