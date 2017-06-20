package org.mycore.common.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRTextResolver;

/**
 * <p>
 * Extend this class to include dynamic jdom content. The content is loaded
 * from a xml file, which have to be set by the <code>setXmlFile</code>
 * method.
 * </p><p>
 * The dynamic comes into play by the variables that can be defined for the
 * uri. In general they are set directly behind the uri prefix. For example:
 * </p>
 * <pre>
 * uriprefix:value1:value2:value3... or
 * uriprefix:varname1=varvalue1:varname2=varvalue2:varname3=varname3...
 * </pre>
 * In the xml file you can use these variables with curly brackets '{}'.
 * For more informations about the syntax see
 * <code>MCRTextResolver</code>. Heres only a short example
 * what is possible:
 * <p>myURIResolver:classId=class_000_1:levels=-1:axis=children</p>
 * <pre>
 * &lt;dynIncl&gt;
 *  &nbsp;&lt;panel&gt;
 *   &nbsp;&nbsp;&lt;hidden var="@classid" default="{classId}"/&gt;
 *   &nbsp;&nbsp;&lt;include uri="dynClassification:editor:{levels}:{axis}:{classId}[:{categId}]"/&gt;
 *  &nbsp;&lt;panel/&gt;
 * &lt;/dynIncl&gt;
 * </pre>
 * 
 * @see MCRTextResolver
 * @author Matthias Eichner
 */
public abstract class MCRDynamicURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRDynamicURIResolver.class);

    protected Element cachedElement;

    protected File xmlFile;

    protected long lastModified;

    public MCRDynamicURIResolver() {
        lastModified = 0;
        cachedElement = null;
        xmlFile = null;
    }

    /**
     * Sets the xml file. From this file the jdom content
     * will be created.
     * 
     * @param xmlFile xml file object
     */
    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
        if (!xmlFile.exists()) {
            LOGGER.error(new FileNotFoundException());
        }
    }

    public Element resolveElement(String uri) throws Exception {
        if (xmlFile == null) {
            throw new NullPointerException("No xml file set in '" + this.getClass().getName() + "'!");
        }

        Element rootElement = getRootElement();
        Hashtable<String, String> variablesMap = createVariablesMap(uri);
        resolveVariablesFromElement(rootElement, variablesMap);
        return rootElement;
    }

    protected Element getRootElement() {
        if (cachedElement == null || xmlFile.lastModified() > lastModified) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(xmlFile);
                cachedElement = doc.getRootElement();
                lastModified = System.currentTimeMillis();
            } catch (Exception exc) {
                LOGGER.error("Error while parsing " + xmlFile + "!", exc);
                return null;
            }
        }
        // clone it for further replacements
        // TODO: whats faster? cloning or building it new from file
        return (Element) cachedElement.clone();
    }

    /**
     * This method creates a hashtable that contains variables. There are two
     * possibilities for getting the name of a variable from the uri:
     * <ol>
     * <li>uriprefix:value1:value2:value3</li>
     * <li>uriprefix:varname1=varvalue1:varname2=varvalue2:varname3=varname3</li>
     * </ol>
     * Both options can be mixed, but this is not recommended.<br>
     * For the first option the name of the variables is 'x', where 'x' is a number
     * for the position in the uri. To get the first value use {1}, to get the second
     * one use {2} and so on.
     * 
     * @param uri the whole uri
     * @return a hashtable with all variables from the uri
     */
    protected Hashtable<String, String> createVariablesMap(String uri) {
        Hashtable<String, String> variablesMap = new Hashtable<String, String>();
        String uriValue = uri.substring(uri.indexOf(':') + 1);
        String[] variablesArr = uriValue.split(":");
        for (int i = 0; i < variablesArr.length; i++) {
            int equalsSignIndex = variablesArr[i].indexOf("=");
            if (equalsSignIndex == -1) {
                String varName = String.valueOf(i + 1);
                String varValue = variablesArr[i];
                variablesMap.put(varName, varValue);
            } else {
                String varName = variablesArr[i].substring(0, equalsSignIndex);
                String varValue = variablesArr[i].substring(equalsSignIndex + 1);
                variablesMap.put(varName, varValue);
            }
        }
        return variablesMap;
    }

    /**
     * This method runs through the whole content of the startElement and
     * tries to resolve all variables in texts and attributes.
     * 
     * @param startElement where to start to resolve the variables
     * @param variablesMap a map of all variables
     */
    protected void resolveVariablesFromElement(Element startElement, Hashtable<String, String> variablesMap) {
        Iterator<Element> it = startElement.getDescendants(Filters.element());
        MCRTextResolver varResolver = new MCRTextResolver(variablesMap);
        while (it.hasNext()) {
            Element element = it.next();
            // text
            String text = element.getText();
            if (text != null && !text.equals("") && text.contains("{")) {
                element.setText(varResolver.resolve(text));
            }

            // attributes
            for (Attribute attrib : (List<Attribute>) element.getAttributes()) {
                String attribValue = attrib.getValue();
                if (attribValue.contains("{")) {
                    attrib.setValue(varResolver.resolve(attribValue));
                }
            }
        }
    }

}
