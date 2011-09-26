package org.mycore.oai;

import java.util.Date;

import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.services.fieldquery.MCRResults;

/**
 * OAI Results container.
 * 
 * @author Matthias Eichner
 */
public class MCROAIResults {

    protected Date expirationDate;

    protected MetadataFormat metadataFormat;

    protected MCRResults results;

    public MCROAIResults(Date expirationDate, MetadataFormat format, MCRResults results) {
        this.expirationDate = expirationDate;
        this.metadataFormat = format;
        this.results = results;
    }

    boolean isExpired() {
        return (new Date().compareTo(expirationDate) > 0);
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public MCRResults getMCRResults() {
        return results;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

}
