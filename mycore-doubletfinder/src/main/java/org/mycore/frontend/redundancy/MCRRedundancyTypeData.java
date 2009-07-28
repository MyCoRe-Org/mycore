package org.mycore.frontend.redundancy;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Data holder class for a redundancy object type.
 * 
 * @author Matthias Eichner
 */
public class MCRRedundancyTypeData {

    private static final Logger LOGGER = Logger.getLogger(MCRRedundancyTypeData.class);

    protected String type;
    protected String tableHead;
    protected List<MCRSortBy> fieldsToSort;
    protected List<MCRFieldDef> fieldsToCompare;
    protected List<XPath> xpathToCompareList;

    public MCRRedundancyTypeData(String alias) throws MCRConfigurationException {
        fieldsToSort = new ArrayList<MCRSortBy>();
        fieldsToCompare = new ArrayList<MCRFieldDef>();
        xpathToCompareList = new ArrayList<XPath>();
        init(alias);
    }

    protected void init(String alias) throws MCRConfigurationException {
        MCRConfiguration config = MCRConfiguration.instance();
        String baseString = "MCR.doubletFinder." + alias + ".";
        type = config.getString(baseString + "type", alias);
        tableHead = config.getString(baseString + "tableHead");
        String fieldsToSortString = config.getString(baseString + "fieldsToSort", null);
        String fieldsToCompareString = config.getString(baseString + "fieldsToCompare", null);
        String xpathCompareString = config.getString(baseString + "xpathToCompare", null);
        if(fieldsToSortString != null && !fieldsToSortString.equals("")) {
            createFieldsToSort(fieldsToSortString);            
        }
        if(fieldsToCompareString != null && !fieldsToCompareString.equals("")) {
            createFieldsToCompare(fieldsToCompareString);            
        }
        if(xpathCompareString != null && !xpathCompareString.equals("")) {
            try {
                createXPathCompareList(xpathCompareString);
            } catch (JDOMException jdomExc) {
                LOGGER.error("Couldnt parse xpath expression: " + xpathCompareString, jdomExc);
            }
        }
    }

    protected void createFieldsToSort(String fieldsToSortString) {
        String[] sList = fieldsToSortString.split(",");
        for(String sortString : sList) {
            MCRFieldDef fieldDef = MCRFieldDef.getDef(sortString);
            MCRSortBy mcrSortBy = new MCRSortBy(fieldDef, MCRSortBy.ASCENDING);
            fieldsToSort.add(mcrSortBy);
        }
    }

    protected void createFieldsToCompare(String fieldsToCompareString) {
        String[] sList = fieldsToCompareString.split(",");
        for(String compareString : sList) {
            MCRFieldDef fieldDef = MCRFieldDef.getDef(compareString);
            fieldsToCompare.add(fieldDef);
        }
    }

    protected void createXPathCompareList(String xpathCompareString) throws JDOMException {
        String[] sList = xpathCompareString.split(",");
        for(String xpath : sList) {
            xpathToCompareList.add(XPath.newInstance(xpath));
        }
    }

    public String getType() {
        return type;
    }
    public String getTableHead() {
        return tableHead;
    }
    public List<MCRSortBy> getFieldsToSort() {
        return fieldsToSort;
    }
    public List<MCRFieldDef> getFieldsToCompare() {
        return fieldsToCompare;
    }
    public List<XPath> getXPathCompareList() {
        return xpathToCompareList;
    }
}
