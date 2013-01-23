package org.mycore.importer.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jdom2.Element;

/**
 * This class is an abstraction of a xml classification mapping file.
 * It describe the connection between classification values from the
 * import source and the mycore classification values. Every instance
 * of this class specify one classification.
 * 
 * @author Matthias Eichner
 */
public class MCRImportClassificationMap extends HashMap<String, String> {

    /**
     * The id of the classification. For example something like
     * DocPortal_class_00000001
     */
    protected String id;

    /**
     * Creates a new classification mapping instance.
     * 
     * @param id the classification id
     */
    public MCRImportClassificationMap(String id) {
        this.id = id;
    }

    /**
     * key = import value</br>
     * value = mycore value
     */
    @Override
    public String put(String key, String value) {
        if(value == null)
            value = "";
        return super.put(key, value);
    }

    /**
     * Returns the mycore value from the hash table by the given
     * import value. If no entry was found, null is returned.
     * 
     * @param importValue the value from the import classification source
     * @return the mycore value, or null if nothing is found
     */
    public String getMyCoReValue(String importValue) {
        return this.get(importValue);
    }

    /**
     * Returns the classification id.
     * 
     * @return id of this instance
     */
    public String getId() {
        return this.id;
    }

    /**
     * Checks if all import- and mycore values are set. If a value
     * is null or like "" false will be returned.
     * 
     * @return true if all values are set, otherwise false
     */
    public boolean isCompletelyFilled() {
        for(Map.Entry<String, String> entry : this.entrySet()) {
            if(entry.getKey() == null || entry.getKey().equals(""))
                return false;
            if(entry.getValue() == null || entry.getValue().equals(""))
                return false;
        }
        return true;
    }

    /**
     * Returns a string list of all import values where the connected
     * mycore value is null or like "".
     * 
     * @return a list of import values
     */
    public ArrayList<String> getEmptyImportValues() {
        ArrayList<String> emptyImportValueList = new ArrayList<String>();
        for(Map.Entry<String, String> entry : this.entrySet()) {
            if(entry.getValue() == null || entry.getValue().equals(""))
                emptyImportValueList.add(entry.getKey());
        }
        return emptyImportValueList;
    }

    /**
     * Creates a jdom xml object representation.
     * The format of the element is:
     * <p>
     * &lt;classificationMapping id="id of the classification"&gt;</br>
     * &nbsp;&lt;map importValue="value from the import source" mycoreValue="the mycore value"&gt;</br>
     * &nbsp;...</br>
     * &lt;/classificationMapping&gt;</br>
     * </p>
     * 
     * @return a new classification mapping element
     */
    public Element createXML() {
        Element rootElement = new Element("classificationMapping");
        rootElement.setAttribute("id", id);
        // go through the table and add all import and mycore values
        for(Map.Entry<String, String> entry : this.entrySet()) {
            Element mapElement = new Element("map");
            String importValue = entry.getKey();
            String mycoreValue = entry.getValue();
            if(importValue == null || importValue.equals(""))
                importValue = "";
            if(mycoreValue == null || mycoreValue.equals(""))
                mycoreValue = "";
            mapElement.setAttribute("importValue", importValue);
            mapElement.setAttribute("mycoreValue", mycoreValue);
            rootElement.addContent(mapElement);
        }
        return rootElement;
    }
}