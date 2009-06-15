package org.mycore.frontend.redundancy.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.redundancy.MCRRedundancyUtil;

/**
 * Processes a redundancy xml-file to relink and delete duplicate mcr objects in the database.<br>
 * Commandprocess:<br>
 * (cleanUp [(process redundancy object[(replace links)* -> (delete mcrobject) -> (delete redundancy object xml entry)])*] -> [update xml document]) 
 * @author Matthias Eichner
 */
public class MCRRedundancyCleanUpCommand {

    private static final Logger LOGGER = Logger.getLogger(MCRRedundancyCleanUpCommand.class);

    /**
     * The xml document is stored for the whole process.
     */
    private static Document document;

    /**
     * The start method.
     * @param type The type.
     * @return A commandlist of redundancy objects which have to processed.
     * @throws Exception
     */
    public static List<String> cleanUp(String type) throws Exception {
        // create the commands list
        List<String> commands = new ArrayList<String>();

        // try to get the right file
        File file = new File(MCRRedundancyUtil.DIR + "redundancy-" + type + ".xml");
        if (!file.exists()) {
            LOGGER.error("Couldnt find the file of type: " + type);
            LOGGER.error("A file like 'redundancy-person.xml' in 'build/webapps/doubletFinder' is necessary.");
            return commands;
        }
        LOGGER.info("Redundancy file found: " + file);

        // get the xml document
        SAXBuilder builder = new SAXBuilder();
        document = builder.build(file);
        Element rootElement = document.getRootElement();
        Filter redunObjectFilter = new ElementFilter("redundancyObjects");

        LOGGER.info("Start to replaces links and delete duplicate objects.");

        // pass through the redundancyObjects elements
        List list = rootElement.getContent(redunObjectFilter);
        for (Object o : list) {
            Element redunElement = (Element) o;
            String status = redunElement.getAttributeValue("status");
            if (status != null && status.equals("closed")) {
                // add the redundancy object to commands list
                commands.add("internal process redundancy object " + redunElement.getAttributeValue("id"));
            }
        }
        if (commands.size() > 0)
            commands.add("internal update xml document " + file.getAbsolutePath());
        else if (list.size() == 0)
            LOGGER.info("Redundancy xml file is empty.");
        else
            LOGGER.info("Redundancy Xml file has no 'closed' entries.");
        return commands;
    }

    /**
     * Creates the commands of an redundancy Object.
     * @param id The id of the redundancyObjects element.
     * @return A list of commands.
     * @throws Exception
     */
    public static List<String> processRedundancyObject(String id) throws Exception {
        Element redunElement = getRedunElementOfId(id);
        Element originalElement = null;
        ArrayList<Element> duplicateElements = new ArrayList<Element>();

        Filter objectFilter = new ElementFilter("object");
        for (Object o : redunElement.getContent(objectFilter)) {
            Element objectElement = (Element) o;
            String status = objectElement.getAttributeValue("status");
            if (status == null)
                continue;
            if (status.equals("nonDoublet")) {
                originalElement = objectElement;
            } else if (status.equals("doublet")) {
                duplicateElements.add(objectElement);
            }
        }
        List<String> commands = createLinkAndDeleteCommands(originalElement, duplicateElements);
        commands.add("internal delete redundancy object xml entry " + id);
        return commands;
    }

    /**
     * Deletes an processed redundancy element entry in the xml document.
     * @param id The id of the element.
     */
    public static void deleteRedundancyElementEntry(String id) {
        Element e = getRedunElementOfId(id);
        if (e.getParent() != null)
            e.getParent().removeContent(e);
    }

    /**
     * Creates the link- and delete commands for an redundancyElement
     */
    private static List<String> createLinkAndDeleteCommands(Element originalElement, ArrayList<Element> duplicateElements) throws Exception {
        List<String> commands = new ArrayList<String>();
        if (originalElement == null || duplicateElements.size() == 0)
            return commands;

        String originalObjectId = originalElement.getAttributeValue("objId");
        for (Element duplicateElement : duplicateElements) {
            String dupObjectId = duplicateElement.getAttributeValue("objId");
            Collection<String> list = MCRLinkTableManager.instance().getSourceOf(dupObjectId, "reference");
            for (String source : list) {
                // add replace command
                commands.add("internal replace links " + source + " " + dupObjectId + " " + originalObjectId);
            }
            // add delete command
            commands.add("delete object " + dupObjectId);
        }
        return commands;
    }

    /**
     * Returns an redundancy object by the specified id.
     * @param id The id.
     * @return The element or null. 
     */
    protected static Element getRedunElementOfId(String id) {
        Filter filter = new ElementFilter("redundancyObjects");
        List list = document.getRootElement().getContent(filter);
        for (Object o : list) {
            Element e = (Element) o;
            if (e.getAttributeValue("id") != null && e.getAttributeValue("id").equals(id)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Replaces all links which are found in the source mcrobject xml-tree.
     * @param source The source Id as String.
     * @param oldLink The link which to replaced.
     * @param newLink The new link.
     */
    public static void replaceLinks(String sourceId, String oldLink, String newLink) throws Exception {
        if (!MCRAccessManager.checkPermission(sourceId, "writedb")) {
            LOGGER.error("The current user has not the permission to modify " + sourceId);
            return;
        }

        MCRObject sourceMCRObject = new MCRObject();
        sourceMCRObject.receiveFromDatastore(sourceId);

        // ArrayList for equal elements
        ArrayList<Element> equalElements = new ArrayList<Element>();

        Namespace ns = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
        Filter oldLinkFilter = new AttributeValueFilter("href", ns, oldLink);
        Document doc = sourceMCRObject.createXML();
        Iterator i = doc.getDescendants(oldLinkFilter);
        while (i.hasNext()) {
            Element e = (Element) i.next();
            e.setAttribute("href", newLink, ns);
            /*	It is possible, that an updated element is equal with an existing element.
            	In that case it is necessary to delete the new element. */
            if (isElementAlreadyExists(e)) {
                equalElements.add(e);
            }
        }
        // delete equal elements
        for (Element e : equalElements) {
            Element parent = e.getParentElement();
            parent.removeContent(e);
        }
        sourceMCRObject.setFromJDOM(doc);
        sourceMCRObject.updateInDatastore();
        LOGGER.info("Links replaced of source " + sourceId + ": " + oldLink + " -> " + newLink);
    }

    /**
     * Checks if the element is equal to an element from the same parent.
     * @param element The element to check.
     * @return If the element in the parent already exists.
     */
    protected static boolean isElementAlreadyExists(Element element) {
        Element parent = element.getParentElement();
        Filter filter = new ElementFilter(element.getName());
        for (Object o : parent.getContent(filter)) {
            Element child = (Element) o;
            // only different instances
            if (element == child)
                continue;

            // bad compare, but jdom doesnt support a better solution
            if (element.getName().equals(child.getName()) && element.getAttributes().toString().equals(child.getAttributes().toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the specified file with the static document xml-structure.
     * @param fileName
     * @throws Exception
     */
    public static void updateXMLDocument(String fileName) throws Exception {
        // write the updated xml document to the file system
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = new FileOutputStream(fileName);
        outputter.output(document, output);
    }

    /**
     * A jdom-filter which compares attribute values.
     */
    protected static class AttributeValueFilter implements Filter {
        protected String attrKey;

        protected String attrValue;

        protected Namespace ns;

        public AttributeValueFilter(String attrKey, Namespace ns, String attrValue) {
            this.attrKey = attrKey;
            this.attrValue = attrValue;
            this.ns = ns;

        }

        public boolean matches(Object arg0) {
            if (!(arg0 instanceof Element))
                return false;
            Element e = (Element) arg0;
            String value = e.getAttributeValue(attrKey, ns);
            if (value != null && value.equals(attrValue))
                return true;
            return false;
        }
    }
}