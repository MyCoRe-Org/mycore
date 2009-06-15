package org.mycore.frontend.redundancy;

import java.util.Map;

/**
 * A redundancy object is defined by an id and a map of compare criterias.
 */
public class MCRRedundancyObject {
    private String objId;
    private Map<String, String> compareCriterias;
    public MCRRedundancyObject(String objId, Map<String, String> compareCriterias) {
        this.objId = objId;
        this.compareCriterias = compareCriterias;
    }
    public String getObjId() {
        return objId;
    }
    public Map<String, String> getCompareCriteria() {
        return compareCriterias;
    }
}