package org.mycore.datamodel.metadata;

import java.util.ArrayList;

import org.jdom2.Content;

public class MCRDefaultEnrichedDerivateLinkIDFactory extends MCRMetaEnrichedLinkIDFactory {

    @Override
    public MCREditableMetaEnrichedLinkID getDerivateLink(MCRDerivate der) {
        final MCREditableMetaEnrichedLinkID derivateLinkID = getEmptyLinkID();
        final String mainDoc = der.getDerivate().getInternals().getMainDoc();
        final String label = der.getLabel();

        derivateLinkID.setReference(der.getId().toString(), null, label);
        derivateLinkID.setSubTag("derobject");
        final ArrayList<Content> contentList = new ArrayList<>();

        final int order = der.getOrder();
        derivateLinkID.setOrder(order);

        if (mainDoc != null) {
            derivateLinkID.setMainDoc(mainDoc);
        }

        derivateLinkID.setTitles(der.getDerivate().getTitles());
        derivateLinkID.setClassifications(der.getDerivate().getClassifications());
        derivateLinkID.setContentList(contentList);

        return derivateLinkID;
    }
}
