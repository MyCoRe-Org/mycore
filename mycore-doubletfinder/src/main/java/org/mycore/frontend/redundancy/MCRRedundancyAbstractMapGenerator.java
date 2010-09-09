package org.mycore.frontend.redundancy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQueryCondition;

/**
 * Abstract class for creating a redundancy map.
 * @author Matthias Eichner
 */
public abstract class MCRRedundancyAbstractMapGenerator {

    private static final Logger LOGGER = Logger.getLogger(MCRRedundancyAbstractMapGenerator.class);

    protected MCRRedundancyTypeData typeData;
    protected Element redundancyMap;
    protected Comparator<String> comparator;
    protected MCRRedundancyFormattable<String> compareStringFormatter;

    public MCRRedundancyAbstractMapGenerator() {
        this.redundancyMap = new Element("redundancyMap");
        this.comparator = new DefaultStringComparator();
        this.compareStringFormatter = new DefaultCompareStringFormatter();
    }

    public void setTypeData(MCRRedundancyTypeData typeData) {
        this.typeData = typeData;
        this.redundancyMap.setAttribute("tableHead", typeData.getTableHead());
    }

    protected String getFileName() {
        File directory = new File(MCRRedundancyUtil.DIR);
        if (!directory.exists())
            directory.mkdir();        
        return MCRRedundancyUtil.DIR + "redundancy-" + typeData.getType() + ".xml";
    }

    /**
     * Creates the search condition.
     * @return a new search condition
     */
    protected MCRCondition createCondition() {
        MCRFieldDef def = MCRFieldDef.getDef("objectType");
        MCRCondition cond = new MCRQueryCondition(def, "=", typeData.getType());
        return cond;
    }

    /**
     * Returns the compare criterias of a search hit. The return map is
     * a combination of fieldsToCompare and xpathToCompare.
     * @param mcrHit the search hit
     * @return a new compare criteria for an MyCoRe Object
     */
    protected Map<String, String> getCompareCriteria(MCRHit mcrHit) {
        Map<String, String> compareCriteriaValues = new Hashtable<String, String>();
        for(MCRFieldDef def : typeData.getFieldsToCompare()) {
            for(MCRFieldValue fieldValue : mcrHit.getMetaData()) {
                if(fieldValue.getField().equals(def)) {
                    String key = def.getName();
                    String value = compareStringFormatter.format(fieldValue.getValue());
                    compareCriteriaValues.put(key, value);
                    break;
                }
            }
        }
        // use the xpath values as compare string
        List<XPath> xpathList = typeData.getXPathCompareList();
        if(xpathList != null && xpathList.size() > 0) {
            Document doc = MCRXMLMetadataManager.instance().retrieveXML(new MCRObjectID(mcrHit.getID()));
            for(int i = 0; i < xpathList.size(); i++) {
                try {
                    Object o = xpathList.get(i).selectSingleNode(doc);
                    String value = null;
                    if (o instanceof Attribute)
                        value = ((Attribute) o).getValue();
                    else if (o instanceof Text)
                        value = ((Text) o).getText();
                    else if(o != null)
                        value = o.toString();
                    compareCriteriaValues.put(xpathList.get(i).getXPath(), compareStringFormatter.format(value));
                } catch (JDOMException jdomExc) {
                    LOGGER.error("Couldnt parse xpath expression " + xpathList.get(i)
                            + " while creating compare criterias for mcr object " + mcrHit.getID(), jdomExc);
                }
            }
        }
        return compareCriteriaValues;
    }

    /**
     * Checks if the redundancy objects are equal.
     * @param obj1 the first redundancy object
     * @param obj2 the second redundancy object
     * @return if they are equal
     */
    protected boolean areRedundancyObjectsEqual(MCRRedundancyObject obj1, MCRRedundancyObject obj2) {
        if(obj1 == null || obj2 == null)
            return false;
        if(obj1.getCompareCriteria() == null || obj2.getCompareCriteria() == null)
            return false; 
        return areConditionsEquals(obj1.getCompareCriteria(), obj2.getCompareCriteria());
    }

    /**
     * Compares a condition map with another condition map.
     * Each key/value pair of the first condition map has to be equal to
     * the second one.
     * @return If the condition maps are equal.
     */
    protected boolean areConditionsEquals(Map<String, String> conditionMap1, Map<String, String> conditionMap2) {
        if(conditionMap1.size() != conditionMap2.size())
            return false;
        Iterator<Map.Entry<String, String>> i = conditionMap1.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, String> entry = i.next();
            String key = entry.getKey();
            String compareValue1 = conditionMap1.get(key);
            String compareValue2 = conditionMap2.get(key);
            if(compareValue1 == null || compareValue2 == null || comparator.compare(compareValue1, compareValue2) != 0)
                return false;
        }
        return true;
    }

    public void setComparator(Comparator<String> comparator) {
        this.comparator = comparator; 
    }
    public Comparator<String> getComparator() {
        return comparator;
    }
    public void setStringFormatter(MCRRedundancyFormattable<String> stringFormatter) {
        this.compareStringFormatter = stringFormatter;
    }
    public MCRRedundancyFormattable<String> getStringFormatter() {
        return compareStringFormatter;
    }

    /**
     * Main method to create the redundancy map.
     */
    public abstract void createRedundancyMap();

    /**
     * Saves the redundancy map to the file system.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveToFile() throws FileNotFoundException, IOException {
        if(redundancyMap == null) {
            LOGGER.error("Redundancy map element is null. Execute createRedundancyMap before saving.");
            return;
        }
        // save xml file
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = new FileOutputStream(getFileName());
        outputter.output(new Document(redundancyMap), output);
        LOGGER.info("command successfully finished");
    }

    /**
     * Creates a group as element. A group contains redundancy
     * objects elements as childs.
     * @param id the id of the group. its a increasing integer number.
     * @param name the name of the group 
     * @return a new group element
     */
    protected Element createGroupElement(int id, Map<String, String> compareCriterias) {
        StringBuffer name = new StringBuffer();
        int count = 0;
        for(String value : compareCriterias.values()) {
            name.append(value);
            if(count++ < compareCriterias.size() - 1)
                name.append(",");
        }
        Element groupElement = new Element("redundancyObjects");
        groupElement.setAttribute("id", String.valueOf(id));
        groupElement.setAttribute("name", name.toString());
        return groupElement;
    }
    /**
     * Creates a single redundancy object element.
     * @param id the id of the mcr object
     * @return a new object element.
     */
    protected Element createObjectElement(String id) {
        Element object = new Element("object");
        object.setAttribute("objId", id);
        return object;
    }

    protected class DefaultStringComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            // do a faster equal compare!
            if(o1.equals(o2))
                return 0;
            return -1;
        }
    }
    protected class DefaultCompareStringFormatter implements MCRRedundancyFormattable<String> {
        public String format(String stringToFormat) {
            // do no formatting
            return stringToFormat;
        }
    }
}