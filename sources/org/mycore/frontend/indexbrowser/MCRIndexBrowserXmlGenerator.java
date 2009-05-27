package org.mycore.frontend.indexbrowser;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Xml generator class for the index browser.
 * <p>
 * This class is excluded from MCRIndexBrowserData.
 * </p>
 * @author Matthias Eichner
 */
public class MCRIndexBrowserXmlGenerator {

    protected static Logger LOGGER = Logger.getLogger(MCRIndexBrowserXmlGenerator.class);
    
    protected static final String defaultlang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "de");

    protected Element page;

    protected MCRIndexBrowserIncomingData browseData;
    
    protected MCRIndexBrowserConfig indexConfig;
    
    protected List<MCRIndexBrowserEntry> resultList;
    
    public MCRIndexBrowserXmlGenerator(List<MCRIndexBrowserEntry> resultList, MCRIndexBrowserIncomingData browseData, MCRIndexBrowserConfig indexConfig) {
        this.browseData = browseData;
        this.indexConfig = indexConfig;
        this.resultList = resultList;

        page = buildPageElement(browseData);
        Element resultsElement = buildResultsElement(page, browseData);
        int numRows = resultList.size();

        int from = Math.max(0, browseData.getFrom());
        int to = Math.min(numRows, browseData.getTo() + 1);
        int numSelectedRows = to - from;

        resultsElement.setAttribute("numHits", String.valueOf(numRows));
        if (browseData.getSearch() != null) {
            resultsElement.setAttribute("search", browseData.getSearch());
        }

        if (numSelectedRows <= indexConfig.getMaxPerPage()) {
            // set the metadata for the details only for the shown objects
            fillHitListWithMetadata(from, to);

            // build the xml-result tree with detailed Informations
            for (int i = from; i < to; i++) {
                MCRIndexBrowserEntry entry = resultList.get(i);
                Element v = new Element("value");
                v.setAttribute("pos", String.valueOf(i));

                List<String> sortValues = entry.getSortValues();
                
                v.addContent(new Element("sort").addContent(sortValues.get(0)));
                String idx = sortValues.get(0);
                if(sortValues.size() > 1)
                    idx = sortValues.get(1);
                v.addContent(new Element("idx").addContent(idx));
                v.addContent(new Element("id").addContent(entry.getObjectId()));

                for(int index = 0; index < indexConfig.getOutputList().size(); index++) {
                    Element col = new Element("col");
                    col.setAttribute("name", indexConfig.getOutputList().get(index));
                    col.addContent(entry.getOutputValue(index));
                    v.addContent(col);
                }
                resultsElement.addContent(v);
            }
        } else {
            // to much results to show all details
            // build the xml-result tree with the range of the searchresults
            int stepSize = calculateStepSize(numSelectedRows, indexConfig.getMaxPerPage());
            List<MyRangeDelim> delims = new Vector<MyRangeDelim>();

            int index = from;
            do {  
                MCRIndexBrowserEntry firstEntry = resultList.get(index);
                delims.add(new MyRangeDelim(index, firstEntry.getSortValue(0)));
                index += stepSize;
                String secondValue = "";
                if(index >= to) {
                    index = to - 1;
                }
                secondValue = resultList.get(index).getSortValue(0);
                delims.add(new MyRangeDelim(index, secondValue));
            } while((++index) < to);
            buildPrefixDifference(delims);
            buildXML(resultsElement, delims);
        }
    }

    /**
     * Calculates the step size of the selection. 
     * 
     * @param numSelectedRows the number of selected elements
     * @param maxPerPage maximum displayable elements
     * @return step size
     */
    private int calculateStepSize(int numSelectedRows, int maxPerPage) {
        for (int i = 1;; i++) {
            double dNum = numSelectedRows;
            double dI = 1.0 / ((double) i);
            double root = Math.pow(dNum, dI);
            if (root <= maxPerPage)
                return (int) (Math.floor(dNum / root));
        }
    }

    /**
     * Returns the final xml document.
     * @return xml document
     */
    public Document getXMLContent() {
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            LOGGER.debug("Results: \n" + out.outputString(page));
        }
        return new Document(page);
    }

    /**
     * Goes through the result list to expand each entry with
     * output values.
     * @param from start from that element
     * @param to end at this element
     */
    private void fillHitListWithMetadata(int from, int to) {
        // now we take only the objects from metadata for the dataobjects that
        // we will shown in detail
        // this occurres if we have only a small count of results, or we show
        // only a part
        if (to - (from + 1) <= indexConfig.getMaxPerPage() && indexConfig.getOutputList() != null) {
            for (int i = from; i < to; i++) {
                MCRIndexBrowserEntry entry = resultList.get(i);
                String id = entry.getObjectId();
                Document jdomDoc = MCRXMLTableManager.instance().readDocument(new MCRObjectID(id));
                // outputfields came only from metadataobject
                setListeElm(jdomDoc, indexConfig.getOutputList(), entry);
            }
        }
    }

    /**
     * Adds the output values for an index browser entry.
     * @param od the xml document of the current mcr object
     * @param outputList the output list from the index configuration
     * @param entry the index browser entry
     */
    private void setListeElm(Document od, List<String> outputList, MCRIndexBrowserEntry entry) {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentlang = mcrSession.getCurrentLanguage();
        for (String sField : outputList) {
            String value = "";
            String attribute = "";

            // Eintrag der ID
            if (sField.equalsIgnoreCase("id")) {
                value = od.getRootElement().getAttributeValue("ID");
            } else {
                if (sField.contains("/")) {
                    // evaluate an XPath-Expression
                    try {
                        XPath xpath = XPath.newInstance(sField);
                        List<?> xpathResults = xpath.selectNodes(od);
                        value = "";
                        for (int i = 0; i < xpathResults.size(); i++) {
                            if (i > 0) {
                                value += " - ";
                            }
                            if (xpathResults.get(i) instanceof Attribute) {
                                value += ((Attribute) xpathResults.get(i)).getValue();
                            } else {
                                value += ((Content) xpathResults.get(i)).getValue();
                            }
                        }

                    } catch (JDOMException jde) {
                        value = "";
                    }
                } else {
                    // get for current lang
                    Iterator<?> it = od.getDescendants(new ElementFilter(sField));
                    // only the atribute different texts are taken!!
                    int counter = 0;
                    boolean hasdefault = false;
                    while (it.hasNext()) {
                        Element el = (Element) it.next();
                        String lang = el.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                        if ((lang != null) && (lang.equals(currentlang))) {
                            if (attribute != el.getAttributeValue("type")) {
                                if (value.length() > 0) {
                                    value += " - ";
                                }
                                value += el.getText();
                                attribute = el.getAttributeValue("type");
                            }
                            counter++;
                        }
                        if ((lang != null) && (lang.equals(currentlang))) {
                            hasdefault = true;
                        }
                    }
                    if (counter == 0) {
                        // get for current lang
                        it = od.getDescendants(new ElementFilter(sField));
                        // only the atribute different texts are taken!!
                        if (hasdefault) {
                            while (it.hasNext()) {
                                Element el = (Element) it.next();
                                String lang = el.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                                if ((lang != null) && (lang.equals(defaultlang))) {
                                    if (attribute != el.getAttributeValue("type")) {
                                        if (value.length() > 0) {
                                            value += " - ";
                                        }
                                        value += el.getText();
                                        attribute = el.getAttributeValue("type");
                                    }
                                    break;
                                }
                            }
                        } else {
                            while (it.hasNext()) {
                                Element el = (Element) it.next();
                                if (attribute != el.getAttributeValue("type")) {
                                    if (value.length() > 0) {
                                        value += " - ";
                                    }
                                    value += el.getText();
                                    attribute = el.getAttributeValue("type");
                                }
                                break;
                            }
                        }
                    }

                }
            }
            entry.addOutputValue(value);
        }
    }

    /**
     * Creates the root element of the browser index.
     * @param browseData the incoming data from the browser
     * @return the new root element
     */
    public static Element buildPageElement(MCRIndexBrowserIncomingData browseData) {
        // Build output index page
        Element page = new Element("indexpage");
        page.setAttribute("path", browseData.getPath());

        Element eIndex = new Element("index");
        page.addContent(eIndex);

        eIndex.setAttribute("id", browseData.getIndex());
        return page;
    }

    /**
     * Adds a result element to the given page element.
     * @param pageElement the parent element
     * @param browseData the incoming data from the browser
     * @return the new results element.
     */
    public static Element buildResultsElement(Element pageElement, MCRIndexBrowserIncomingData browseData) {
        Element results = new Element("results");
        pageElement.addContent(results);
        results.setAttribute("mode", browseData.getMode());
        return results;
    }

    /**
     * Builds the prefix difference value for each delimiter.
     * @param delims a list of delimiter.
     */
    protected void buildPrefixDifference(List<MyRangeDelim> delims) {
        for (int i = 0; i < delims.size(); i++) {
            MyRangeDelim curr = (delims.get(i));
            MyRangeDelim prev = (delims.get(Math.max(0, i - 1)));
            MyRangeDelim next = (delims.get(Math.min(i + 1, delims.size() - 1)));

            String vCurr = curr.value;
            String vPrev = (i > 0 ? prev.value : "");
            String vNext = (i < delims.size() - 1 ? next.value : "");

            String a = buildPrefixDifference(vCurr, vPrev);
            String b = buildPrefixDifference(vCurr, vNext);
            curr.diff = (a.length() > b.length() ? a : b);
        }
    }

    /**
     * Compares two strings and returns the prefix difference.
     * @param a the first string
     * @param b the second string
     * @return the prefix which is equal in both string
     */
    protected String buildPrefixDifference(String a, String b) {
        if (a.equals(b))
            return a;

        StringBuffer pdiff = new StringBuffer();

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            pdiff.append(a.charAt(i));
            if (a.charAt(i) != b.charAt(i))
                break;
        }

        if ((a.length() > b.length()) && (b.equals(pdiff.toString())))
            pdiff.append(a.charAt(pdiff.length()));

        return pdiff.toString();
    }

    /**
     * Adds each delimiter entry to the results element.
     * @param results the parent results element
     * @param delims a list of delimiters
     */
    protected void buildXML(Element results, List<MyRangeDelim> delims) {
        for (int i = 0; i < delims.size(); i += 2) {
            MyRangeDelim start = (delims.get(i));
            MyRangeDelim end = (delims.get(i + 1));

            Element range = new Element("range");
            results.addContent(range);

            Element eFrom = new Element("from");
            eFrom.setAttribute("pos", String.valueOf(start.pos));
            eFrom.setAttribute("short", start.diff);
            eFrom.addContent(start.value);
            range.addContent(eFrom);

            Element eTo = new Element("to");
            eTo.setAttribute("pos", String.valueOf(end.pos));
            eTo.setAttribute("short", end.diff);
            eTo.addContent(end.value);
            range.addContent(eTo);
        }
    }

    /**
     * Holder class for an index browser delimiter entry. A delimiter entry
     * is used, when a range of elements are displayed (folder style).
     */
    private static class MyRangeDelim {
        int pos;

        String value;

        String diff;

        MyRangeDelim(int pos, String value) {
            this.pos = pos;
            this.value = value;
        }
    }

}

