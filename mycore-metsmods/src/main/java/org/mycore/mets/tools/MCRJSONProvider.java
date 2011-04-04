/*
 * $Revision$ $Date$
 * $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- und
 * Landesbibliothek Jena
 * 
 * Mets-Editor is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mets-Editor is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Mets-Editor. If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.mets.model.IMetsElement;
import org.mycore.mets.tools.model.MCRDirectory;
import org.mycore.mets.tools.model.MCREntry;
import org.mycore.mets.tools.model.MCRMetsTree;

/**
 * @author Silvio Hermann (shermann)
 */
public class MCRJSONProvider implements Comparator<MCRFilesystemNode> {
    public static final String DEFAULT_METS_FILENAME = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");

    final private static Logger LOGGER = Logger.getLogger(MCRJSONProvider.class);

    private String derivate;

    private Element structLink;

    private Document mets;

    /**
     * @param mets
     *            the Mets document, must be non null
     * @param derivate
     *            the derivate id
     */
    @SuppressWarnings("unchecked")
    public MCRJSONProvider(Document mets, String derivate) throws DocumentException {
        this.derivate = derivate;
        this.mets = mets;

        /* set the struct link */
        Iterator<Element> it = mets.getDescendants(new ElementFilter("structLink", IMetsElement.METS));
        if (!it.hasNext()) {
            throw new DocumentException("Mets document is invalid, no structLink element found");
        }
        /* assuming only one struct map is existing */
        structLink = it.next();
    }

    /**
     * Use this constructor if no mets document for the given derivate is
     * available. The {@link MCRJSONProvider#toJSON()} method will return a
     * default JSON string.
     * 
     * @param derivate
     */
    public MCRJSONProvider(String derivate) throws DocumentException {
        this.derivate = derivate;
    }

    /**
     * @return
     */
    public String getDerivate() {
        return derivate;

    }

    /**
     * @param mets
     * @return String a String in JSON format suitable for a client side dijit
     *         tree
     */
    @SuppressWarnings("unchecked")
    public String toJSON() {
        if (this.mets == null) {
            LOGGER.info("No mets document set. Return default JSON for derivate \"" + derivate + "\"");
            return this.toJSON(this.derivate);
        }

        Element logStructMap = getLogicalStructMapElement(this.mets);
        Element parentDiv = logStructMap.getChild("div", IMetsElement.METS);
        if (parentDiv == null) {
            LOGGER.error("Invalid mets document, as there is no div container in the logical structure map <mets:structMap TYPE=\"LOGICAL\"> ");
            return null;
        }

        XPath xpath = null;
        List<Element> nodes = null;
        try {
            xpath = XPath.newInstance("mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div/mets:div");
            xpath.addNamespace(IMetsElement.METS);
            nodes = xpath.selectNodes(this.mets);
        } catch (JDOMException e) {
            LOGGER.error(e);
        }

        if (nodes == null) {
            LOGGER.error("Cannot create JSONObject as there is no logical structMap");
            return null;
        }

        MCRMetsTree tree = new MCRMetsTree(derivate);
        /* setting document type and title */
        String docType = parentDiv.getAttributeValue("TYPE");
        String docTitle = parentDiv.getAttributeValue("LABEL");

        if (docType != null && docType.length() > 0) {
            tree.setDocType(docType);
        }
        if (docTitle != null && docTitle.length() > 0) {
            tree.setDocTitle(docTitle);
        }

        Iterator<Element> divIterator = nodes.iterator();

        while (divIterator.hasNext()) {
            Element currentLogicalDiv = divIterator.next();
            String logicalId = currentLogicalDiv.getAttributeValue("ID");
            String label = currentLogicalDiv.getAttributeValue("LABEL");
            String structureType = currentLogicalDiv.getAttributeValue("TYPE");

            /* current element is about to be an item in the digit tree */
            if (structureType.equals("page")) {
                String physId = getPhysicalIdsForLogical(logicalId)[0];
                String itemId = physId.substring(physId.indexOf("_") + 1);
                String path = getHref(itemId);
                String labelPage = getLabelByPhysicalId(physId);

                if (labelPage == null) {
                    LOGGER.debug("Could not determine label attribute. Using file name as label");
                    int index = path.lastIndexOf("/");
                    label = path.substring(index == -1 ? 0 : index + 1);
                }
                MCREntry page = new MCREntry(itemId, labelPage, path, physId, "page");

                int order = Integer.valueOf(getOrderAttribute(physId));
                page.setOrder(order);
                String orderLabel = getOrderLabelAttribute(physId);
                page.setOrderLabel(orderLabel);
                tree.addEntry(page);
            }
            /*
             * current element is about to be a category/parent in the digit
             * tree
             */
            else {
                try {
                    XPath kiddies = XPath.newInstance("mets:div");
                    kiddies.addNamespace(IMetsElement.METS);
                    /* list of children */
                    List<Element> list = kiddies.selectNodes(currentLogicalDiv);

                    MCRDirectory dir = new MCRDirectory(logicalId, label, structureType);
                    tree.addDirectory(dir);

                    boolean flag = firstIsFile(logicalId);
                    String physId = null;
                    if (flag) {
                        physId = getPhysicalIdsForLogical(logicalId)[0];
                    } else {
                        String firstDivWithFiles = getFirstDivWithFiles(logicalId, list);
                        physId = getPhysicalIdsForLogical(firstDivWithFiles)[0];
                    }
                    int order = Integer.valueOf(getOrderAttribute(physId));
                    dir.setOrder(order);
                    addFiles(dir);
                    buildTree(dir, list);

                } catch (JDOMException ex) {
                    LOGGER.error(ex);
                }
            }
        }

        /* handle the pages direct assigned to the root */
        String[] physIDsUnderRoot = getPhysicalIdsForLogical(parentDiv.getAttributeValue("ID"));
        for (String physicalId : physIDsUnderRoot) {
            String itemID = getFileID(physicalId);
            String path = getHref(itemID);
            String label = getLabelByPhysicalId(physicalId);
            if (label == null) {
                int index = path.lastIndexOf("/");
                label = path.substring(index == -1 ? 0 : index + 1);
            }
            MCREntry e = new MCREntry(itemID, label, path, physicalId, "page");
            e.setOrderLabel(getOrderLabelAttribute(physicalId));
            e.setOrder(getOrderAttribute(physicalId));
            tree.addEntry(e);
        }

        LOGGER.debug(tree.asJson());
        return tree.asJson();
    }

    /**
     * @return the orderlabel attribute of the div with the given physical id,
     *         returns an empty string if there is no such label
     */
    @SuppressWarnings("unchecked")
    private String getOrderLabelAttribute(String physId) {
        XPath xpath = null;
        List<Element> nodes = null;
        try {
            xpath = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div[@ID='" + physId + "']");
            xpath.addNamespace(IMetsElement.METS);
            nodes = xpath.selectNodes(this.mets);
            Element e = nodes.get(0);
            String orderLabel = e.getAttributeValue("ORDERLABEL");
            return orderLabel == null ? "" : orderLabel;
        } catch (Exception e) {
            LOGGER.error("Error getting orderlabel attribute for file with physical id=" + physId, e);
        }
        return "";
    }

    /**
     * @return true if the first child is a file/page or false otherwise
     */
    private boolean firstIsFile(String logicalDivId) {
        String[] array = this.getPhysicalIdsForLogical(logicalDivId);
        return array.length == 0 ? false : true;
    }

    /** @return the id of the first logical div where the 1st child is a file */
    @SuppressWarnings("unchecked")
    private String getFirstDivWithFiles(String parentLogicalDivId, List<Element> children) throws JDOMException {
        Iterator<Element> iterator = children.iterator();
        while (iterator.hasNext()) {
            Element div = iterator.next();
            String divId = div.getAttributeValue("ID");
            if (firstIsFile(divId)) {
                return divId;
            } else {
                XPath kiddies = XPath.newInstance("mets:div");
                kiddies.addNamespace(IMetsElement.METS);

                return getFirstDivWithFiles(divId, kiddies.selectNodes(div));
            }
        }
        return null;
    }

    /** This methods builds the tree with the mets document as a base */
    @SuppressWarnings("unchecked")
    private void buildTree(MCRDirectory parent, List<Element> children) {
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            Element logicalDiv = it.next();
            String divType = logicalDiv.getAttributeValue("TYPE");
            String logicalDivId = logicalDiv.getAttributeValue("ID");
            String logicalDivLabel = logicalDiv.getAttributeValue("LABEL");

            MCRDirectory dir = new MCRDirectory(logicalDivId, logicalDivLabel, divType);
            int order = Integer.valueOf(getOrderAttribute(getPhysicalIdsForLogical(logicalDivId)[0]));
            dir.setOrder(order);
            addFiles(dir);
            parent.addDirectory(dir);

            try {
                XPath kiddies = XPath.newInstance("mets:div");
                kiddies.addNamespace(IMetsElement.METS);
                buildTree(dir, kiddies.selectNodes(logicalDiv));
            } catch (Exception x) {
                LOGGER.error(x);
            }
        }
    }

    /**
     * Gets the files (physical ids) belonging to the directory, creates a entry
     * for each file and adds it to the directory
     * 
     * @param dir
     */
    private void addFiles(MCRDirectory dir) {
        String[] physIds = getPhysicalIdsForLogical(dir.getLogicalId());
        for (int i = 0; i < physIds.length; i++) {
            /* determine base properties */
            String itemId = getFileID(physIds[i]);
            String path = getHref(itemId);
            String label = getLabelByPhysicalId(physIds[i]);
            if (label == null) {
                LOGGER.debug("Could not determine label attribute. Using file name as label");
                int index = path.lastIndexOf("/");
                label = path.substring(index == -1 ? 0 : index + 1);
            }

            MCREntry page = new MCREntry(itemId, label, path, physIds[i], "page");
            page.setOrder(getOrderAttribute(physIds[i]));
            String orderLabel = getOrderLabelAttribute(physIds[i]);
            page.setOrderLabel(orderLabel);
            dir.addEntry(page);
        }
    }

    /**
     * Method returns the file id for the given physical id
     * 
     * @return the file id for the given physical id
     */
    private String getFileID(String physicalID) {
        try {
            String path = "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div[@ID='" + physicalID + "']/mets:fptr/@FILEID";
            XPath xp = XPath.newInstance(path);
            xp.addNamespace(MCRConstants.METS_NAMESPACE);
            xp.addNamespace(MCRConstants.XLINK_NAMESPACE);

            Object node = xp.selectSingleNode(this.mets);
            if (node instanceof Attribute) {
                return ((Attribute) node).getValue();
            }

        } catch (Exception ex) {
            LOGGER.error("Error determining file id in structMap for physical file id " + physicalID, ex);
        }
        return null;
    }

    /**
     * @param physId
     */
    private String getLabelByPhysicalId(String physId) {
        try {
            String path = "mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div[@ID='" + physId + "']/@LABEL";
            XPath xp = XPath.newInstance(path);
            xp.addNamespace(MCRConstants.METS_NAMESPACE);
            xp.addNamespace(MCRConstants.XLINK_NAMESPACE);

            Object node = xp.selectSingleNode(this.mets);
            if (node instanceof Attribute) {
                return ((Attribute) node).getValue();
            }

        } catch (Exception ex) {
            LOGGER.error("Error determining LABEL in structMap (physical) for physical id " + physId, ex);
        }
        return null;
    }

    /**
     * Returns the href attribute of the given file within the fileGrp section
     * 
     * @param fileID
     *            the fileid to lookup (must be in the mets:fileGrp USE="MASTER"
     *            element)
     * @return the href attribute
     */
    private String getHref(String fileID) {
        try {
            String path = "mets:mets/mets:fileSec/mets:fileGrp[@USE='MASTER']/mets:file[@ID='" + fileID + "']/mets:FLocat/@xlink:href";
            XPath xp = XPath.newInstance(path);
            xp.addNamespace(MCRConstants.METS_NAMESPACE);
            xp.addNamespace(MCRConstants.XLINK_NAMESPACE);

            Object node = xp.selectSingleNode(this.mets);
            if (node instanceof Attribute) {
                return ((Attribute) node).getValue();
            }

        } catch (Exception ex) {
            LOGGER.error("Error determining path in file group with use=\"master\" for master file id " + fileID, ex);
        }
        return null;
    }

    /**
     * Returns the physical order of the file associated with the given physical
     * id
     */
    @SuppressWarnings("unchecked")
    private int getOrderAttribute(String physId) {
        XPath xpath = null;
        List<Element> nodes = null;
        try {
            xpath = XPath.newInstance("mets:mets/mets:structMap[@TYPE='PHYSICAL']/mets:div/mets:div[@ID='" + physId + "']");
            xpath.addNamespace(IMetsElement.METS);
            nodes = xpath.selectNodes(this.mets);
            Element e = nodes.get(0);
            int order = Integer.valueOf(e.getAttributeValue("ORDER"));
            return order;
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return -1;
    }

    /**
     * @param mets
     *            the source mets document
     * @return the logical structure map of the mets document
     */
    @SuppressWarnings("unchecked")
    private Element getLogicalStructMapElement(Document mets) {
        Iterator<Element> iterator = mets.getDescendants(new ElementFilter("structMap", IMetsElement.METS));
        while (iterator.hasNext()) {
            Element structMap = iterator.next();
            String type = structMap.getAttributeValue("TYPE");
            if (type != null && type.equals("LOGICAL")) {
                return structMap;
            }
        }

        return null;
    }

    /**
     * Creates a JSON Object for the dojo tree at the client side
     * 
     * @param derivate
     *            the id of the derivate for which to create the initial json
     * @return the json string for the given derivate
     */
    private String toJSON(String derivate) {
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(derivate);
        MCRFilesystemNode[] nodes = ((org.mycore.datamodel.ifs.MCRDirectory) node).getChildren();
        Arrays.sort(nodes, this);

        StringBuilder builder = new StringBuilder();
        builder.append("{identifier: 'id',label: 'name',items: [\n");
        builder.append("{id: '" + derivate + "', name:'" + derivate + "', structureType:'monograph', type:'category', children:[\n");

        processNodes(nodes, builder);

        builder.append("]}\n]}");
        return builder.toString();
    }

    /**
     * @param nodes
     * @param builder
     */
    private void processNodes(MCRFilesystemNode[] nodes, StringBuilder builder) {
        for (int i = 0; i < nodes.length; i++) {
            String name = nodes[i].getName();
            /* ignore the mets file that may be available */
            if (!name.endsWith(DEFAULT_METS_FILENAME)) {
                if (nodes[i] instanceof org.mycore.datamodel.ifs.MCRDirectory) {
                    builder.append("\t{ id: '");
                    builder.append(UUID.randomUUID());
                    builder.append("', name:'");
                    builder.append(name + "', ");
                    builder.append("structureType:'section', ");
                    builder.append("orderLabel:'', ");
                    builder.append("type:'category', ");
                    builder.append("children:[\n");
                    processNodes(((org.mycore.datamodel.ifs.MCRDirectory) nodes[i]).getChildren(), builder);
                    builder.append("]}\n");
                } else {
                    builder.append("\t{ id: '");
                    builder.append("master_" + UUID.randomUUID() + "', ");
                    builder.append("name:'" + name + "', ");
                    builder.append("path:'" + nodes[i].getAbsolutePath().substring(1) + "', ");
                    builder.append("structureType:'page', ");
                    builder.append("orderLabel:'', ");
                    builder.append("type:'item'}");
                }
            }
            if (i != nodes.length - 1) {
                builder.append(",\n");
            }
        }
    }

    /**
     * @param logicalId
     * @return the list of physical ids belonging to the given logical id
     */
    @SuppressWarnings("unchecked")
    private String[] getPhysicalIdsForLogical(String logicalId) {
        try {
            XPath xp = XPath.newInstance("mets:mets/mets:structLink/mets:smLink[@xlink:from='" + logicalId + "']/@xlink:to");
            xp.addNamespace(MCRConstants.METS_NAMESPACE);
            xp.addNamespace(MCRConstants.XLINK_NAMESPACE);
            List a = xp.selectNodes(this.mets);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }

        Vector<String> col = new Vector<String>();
        Iterator<Element> it = this.structLink.getDescendants(new ElementFilter("smLink", IMetsElement.METS));
        while (it.hasNext()) {
            Element link = it.next();
            if (link.getAttributeValue("from", IMetsElement.XLINK).equals(logicalId)) {
                String linkTo = link.getAttributeValue("to", IMetsElement.XLINK);
                col.add(linkTo);
            }
        }
        return col.toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(MCRFilesystemNode o1, MCRFilesystemNode o2) {
        if (o1.getName().compareTo(o2.getName()) < 0) {
            return -1;
        }
        if (o1.getName().compareTo(o2.getName()) > 0) {
            return 1;
        }
        if (o1.getName().compareTo(o2.getName()) == 0) {
            return 0;
        }
        return 0;
    }
}
