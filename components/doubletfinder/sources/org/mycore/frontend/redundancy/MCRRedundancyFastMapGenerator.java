package org.mycore.frontend.redundancy;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * This implementation does a fast compare to create the
 * redundancy map. It only compares the result object n with
 * n - 1. That works because the result list is sorted.
 * 
 * @author Matthias Eichner
 */
public class MCRRedundancyFastMapGenerator extends MCRRedundancyAbstractMapGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRRedundancyFastMapGenerator.class);

    public MCRRedundancyFastMapGenerator() {
        super();
    }

    public void createRedundancyMap() {
        LOGGER.info("create the redundancy map");

        // get the search condition
        MCRCondition cond = createCondition();
        // sort the list
        List<MCRSortBy> sortByList = typeData.getFieldsToSort();
        // do search
        MCRQuery query = new MCRQuery(cond, sortByList, 0);
        MCRResults result = MCRQueryManager.search(query);

        MCRRedundancyObject previousRedundancyObject = null;
        Element currentGroupElement = null;
        int groupCount = 0;

        // go through all results
        for (MCRHit mcrHit : result) {
            // get the compare criteria of the mycore hit object
            Map<String,String> compareCriteria = getCompareCriteria(mcrHit);
            // set the current redundancy object
            MCRRedundancyObject currentRedundancyObject = new MCRRedundancyObject(mcrHit.getID(), compareCriteria);
            // test if the objects are equal, if true returned they are duplicates
            if(areRedundancyObjectsEqual(currentRedundancyObject, previousRedundancyObject)) {
                // there is no existing group element for the duplicates -> create a new group element
                if(currentGroupElement == null) {
                    currentGroupElement = createGroupElement(groupCount++, compareCriteria);
                    currentGroupElement.addContent(createObjectElement(previousRedundancyObject.getObjId()));
                    redundancyMap.addContent(currentGroupElement);
                }
                currentGroupElement.addContent(createObjectElement(currentRedundancyObject.getObjId()));
            } else {
                // the names are different, so there is no group for those
                currentGroupElement = null;
            }
            // set the previous element
            previousRedundancyObject = currentRedundancyObject;
        }
        LOGGER.info("redundancy map created");
    }
}
