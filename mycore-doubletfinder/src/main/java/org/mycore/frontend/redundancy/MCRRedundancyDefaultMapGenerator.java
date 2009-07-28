package org.mycore.frontend.redundancy;

import java.util.ArrayList;
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
 * The default redundancy map generator compares all objects of a type
 * with each other. This is not the fastest way to compare the objects
 * but it gives you the possibility to use your own comparator.
 * 
 * @author Matthias Eichner
 */
public class MCRRedundancyDefaultMapGenerator extends MCRRedundancyAbstractMapGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRRedundancyDefaultMapGenerator.class);

    private List<MCRRedundancyObject> objectList;
    private List<HelpGroup> groupList;

    public MCRRedundancyDefaultMapGenerator() {
        super();
        objectList = new ArrayList<MCRRedundancyObject>();
        groupList = new ArrayList<HelpGroup>();
    }

    @Override
    public void createRedundancyMap() {
        LOGGER.info("create the redundancy map");

        // get the search condition
        MCRCondition cond = createCondition();
        // sort the list
        List<MCRSortBy> sortByList = typeData.getFieldsToSort();
        // do search
        MCRQuery query = new MCRQuery(cond, sortByList, 0);
        MCRResults result = MCRQueryManager.search(query);

        int groupCount = 0;

        // go through all results
        for (MCRHit mcrHit : result) {
            // get the compare criteria of the mycore hit object
            Map<String,String> compareCriterias = getCompareCriteria(mcrHit);
            // set the current redundancy object
            MCRRedundancyObject currentRedundancyObject = new MCRRedundancyObject(mcrHit.getID(), compareCriterias);

            // check if a group with this compare criteria exists
            Element groupElement = getGroup(compareCriterias);
            if(groupElement != null) {
                // yes -> add this redundancy object to the group
                Element objElement = createObjectElement(currentRedundancyObject.getObjId());
                groupElement.addContent(objElement);
            } else {
                // no -> check if the current object compare criteria exists in the object list
                MCRRedundancyObject equalObject = null;
                // do a reverse search through the list, because the last element is in most cases
                // probably the more equal one
                for(int i = objectList.size() -1; i >= 0; i--) {
                    MCRRedundancyObject obj = objectList.get(i);
                    if(areRedundancyObjectsEqual(currentRedundancyObject, obj)) {
                        equalObject = obj;
                        break;
                    }
                }
                // objects are equal, so create a new group and add both redundancy objects to it
                if(equalObject != null) {
                    groupElement = createGroupElement(groupCount++, currentRedundancyObject.getCompareCriteria());
                    Element objElement1 = createObjectElement(currentRedundancyObject.getObjId());
                    Element objElement2 = createObjectElement(equalObject.getObjId());
                    groupElement.addContent(objElement1);
                    groupElement.addContent(objElement2);
                    // add the group to the group list and to the redundancy map
                    groupList.add(new HelpGroup(groupElement, compareCriterias));
                    redundancyMap.addContent(groupElement);
                    // remove the the redundancy object from the object list
                    objectList.remove(equalObject);
                } else {
                    // add the object to the object list
                    objectList.add(currentRedundancyObject);
                }
            }
        }
        LOGGER.info("redundancy map created");
    }

    protected Element getGroup(Map<String, String> compareCriterias) {
        for(HelpGroup group : groupList) {
            if(areConditionsEquals(group.getConditionMap(), compareCriterias)) {
                return group.getGroupElement();
            }
        }
        return null;
    }

    protected class HelpGroup {
        protected Element groupElement;
        protected Map<String, String> conditionMap;
        public HelpGroup(Element groupElement, Map<String,String> conditionMap) {
            this.groupElement = groupElement;
            this.conditionMap = conditionMap;
        }
        public Map<String, String> getConditionMap() {
            return conditionMap;
        }
        public Element getGroupElement() {
            return groupElement;
        }
    }
}