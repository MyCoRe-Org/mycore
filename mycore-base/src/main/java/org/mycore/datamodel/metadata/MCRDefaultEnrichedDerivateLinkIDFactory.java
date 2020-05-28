package org.mycore.datamodel.metadata;

import java.util.stream.Collectors;

public class MCRDefaultEnrichedDerivateLinkIDFactory extends MCRMetaEnrichedLinkIDFactory {

    @Override
    public MCREditableMetaEnrichedLinkID getDerivateLink(MCRDerivate der) {
        final MCREditableMetaEnrichedLinkID derivateLinkID = getEmptyLinkID();
        final String mainDoc = der.getDerivate().getInternals().getMainDoc();
        final String label = der.getLabel();

        derivateLinkID.setReference(der.getId().toString(), null, label);
        derivateLinkID.setSubTag("derobject");

        final int order = der.getOrder();
        derivateLinkID.setOrder(order);

        if (mainDoc != null) {
            derivateLinkID.setMainDoc(mainDoc);
        }

        derivateLinkID.setTitles(der.getDerivate().getTitles());
        derivateLinkID.setClassifications(
            der.getDerivate().getClassifications().stream()
                .map(metaClass -> metaClass.category)
                .collect(Collectors.toList()));

        return derivateLinkID;
    }
}
