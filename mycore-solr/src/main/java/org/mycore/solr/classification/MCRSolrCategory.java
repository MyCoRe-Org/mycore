package org.mycore.solr.classification;

import java.util.LinkedList;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

public class MCRSolrCategory {

    private MCRCategory category;

    public MCRSolrCategory(MCRCategory category) {
        this.category = category;
    }

    public SolrInputDocument toSolrDocument() {
        SolrInputDocument doc = new SolrInputDocument();
        LinkedList<MCRCategory> ancestors = MCRSolrClassificationUtil.getAncestors(category);
        MCRCategory parent = !ancestors.isEmpty() ? ancestors.getLast() : null;
        // ids
        MCRCategoryID id = category.getId();
        doc.setField("id", id.toString());
        doc.setField("classification", id.getRootID());
        doc.setField("type", "node");
        if (category.isCategory()) {
            doc.setField("category", id.getID());
        }
        // labels
        Set<MCRLabel> labels = category.getLabels();
        for (MCRLabel label : labels) {
            doc.addField("label." + label.getLang(), label.getText());
        }
        // children
        if (category.hasChildren()) {
            for (MCRCategory child : category.getChildren()) {
                doc.addField("children", child.getId().toString());
            }
        }
        // parent
        if (parent != null) {
            doc.setField("parent", parent.getId().toString());
            doc.setField("index", parent.getChildren().indexOf(category));
        }
        // ancestors
        for (MCRCategory ancestor : ancestors) {
            doc.addField("ancestors", ancestor.getId().toString());
        }
        return doc;
    }

}
