/**
 * 
 */
package org.mycore.services.fieldquery;

import java.util.List;

import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author shermann
 * 
 * @deprecated Define proper search fields in solr to get a good performance solution.
 */
public class MCRDerivateWithURNSearcher extends MCRSearcher {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#search(org.mycore.parsers.
     * bool.MCRCondition, int, java.util.List, boolean)
     */
    @Override
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        if (!(condition instanceof MCRQueryCondition)) {
            LOGGER.warn("Retrieved of type " + condition.getClass() + " but only type" + MCRQueryCondition.class.getName()
                    + " is currently supported.");
            return new MCRResults();
        }

        String fieldName = ((MCRQueryCondition) condition).getFieldName();

        if (fieldName.equals("objectURN")) {
            return getObjectsForURN(condition);
        }

        if (fieldName.equals("objectsWithURN")) {
            return getObjectsWithURN(condition);
        }
        return new MCRResults();
    }

    /** Retrieves parent objects matching a given urn */
    private MCRResults getObjectsForURN(MCRCondition condition) {
        String value = null;
        if (condition instanceof MCRQueryCondition) {
            value = ((MCRQueryCondition) condition).getValue();
        }
        if (value == null) {
            return new MCRResults();
        }

        MCRCondition tFCond = new MCRQueryParser().parse("derivateURN like \"" + value + "\"");
        MCRQuery q = new MCRQuery(tFCond);
        MCRResults results = MCRQueryManager.search(q);

        MCRResults toReturn = new MCRResults();
        for (MCRHit aHit : results) {
            if (!aHit.getID().contains("_derivate_")) {
                toReturn.addHit(aHit);
            } else {
                MCRFieldValue derivateOwner = aHit.getMetaData("derivateOwner");
                if (derivateOwner != null) {
                    MCRHit newHit = new MCRHit(derivateOwner.getValue());
                    toReturn.addHit(newHit);
                }
            }
        }

        return toReturn;
    }

    /**
     * Depending on the condition provided this methods returns either all
     * objects with urn or all objects without urn
     * 
     * @param condition
     */
    private MCRResults getObjectsWithURN(MCRCondition condition) {
        String value = null;
        if (condition instanceof MCRQueryCondition) {
            value = ((MCRQueryCondition) condition).getValue();
        }

        MCRCondition tFCond = null;
        if (value != null && value.equalsIgnoreCase("false")) {
            List<String> idList = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
            MCRResults result = new MCRResults();
            for (String derivateId : idList) {
                if (!MCRXMLFunctions.hasURNDefined(derivateId)) {
                    MCRFieldValue derivateOwner = new MCRFieldValue("derivateOwner", getDerivateOwner(derivateId));
                    MCRHit newHit = new MCRHit(derivateId);
                    newHit.addMetaData(derivateOwner);
                    result.addHit(newHit);
                }
            }
            return result;
        } else {
            /* all objects with urn */
            tFCond = new MCRQueryParser().parse("derivateURN like \"urn\"");
        }
        MCRQuery q = new MCRQuery(tFCond);
        return MCRQueryManager.search(q);
    }

    private String getDerivateOwner(String id) {
        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(id));
        return der.getDerivate().getMetaLink().getXLinkHref();
    }

    @Override
    public boolean isIndexer() {
        return false;
    }
}