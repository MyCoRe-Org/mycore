package org.mycore.solr.classification;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Simple helper class to handle a classification link in solr.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrCategoryLink {

    private MCRCategoryID categoryId;

    private MCRCategLinkReference linkReference;

    /**
     * Creates a new link object.
     * 
     * @param categoryId category of the link 
     * @param linkReference the link reference
     */
    public MCRSolrCategoryLink(MCRCategoryID categoryId, MCRCategLinkReference linkReference) {
        this.categoryId = categoryId;
        this.linkReference = linkReference;
    }

    /**
     * Transform this link object to a solr document.
     * <ul>
     * <li>id - combination of the object id and the category separated by a dollar sign </li>
     * <li>object - object id</li>
     * <li>category - category id</li>
     * <li>type - fix string "link"</li>
     * <li>linkType - type of the link</li>
     * </ul>
     * 
     * @return a new solr document
     */
    public SolrInputDocument toSolrDocument() {
        SolrInputDocument doc = new SolrInputDocument();
        String objectId = linkReference.getObjectID();
        String catId = categoryId.toString();
        doc.setField("id", objectId + "$" + catId);
        doc.setField("object", objectId);
        doc.setField("category", catId);
        doc.setField("type", "link");
        doc.setField("linkType", linkReference.getType());
        return doc;
    }

}
