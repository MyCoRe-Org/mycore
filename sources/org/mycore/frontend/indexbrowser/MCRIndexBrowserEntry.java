package org.mycore.frontend.indexbrowser;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder class for an index browser entry.
 * 
 * @author Matthias Eichner
 */
public class MCRIndexBrowserEntry {
    private List<String> sortValues;
    private List<String> outputValues;
    private String objectId;

    public MCRIndexBrowserEntry() {
        sortValues = new ArrayList<String>();
        outputValues = new ArrayList<String>();
    }
    public void addSortValue(String sortValue) {
        sortValues.add(sortValue);
    }
    public void addOutputValue(String value) {
        outputValues.add(value);
    }
    public String getSortValue(int index) {
        return sortValues.get(index);
    }
    public String getOutputValue(int index) {
        return outputValues.get(index);
    }
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
    public List<String> getSortValues() {
        return sortValues;
    }
    public List<String> getOutputValues() {
        return outputValues;
    }
    public String getObjectId() {
        return objectId;
    }
}