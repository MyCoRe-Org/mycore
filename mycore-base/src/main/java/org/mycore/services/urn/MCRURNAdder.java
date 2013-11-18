package org.mycore.services.urn;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

/**
 * This class provides methods for adding URN to mycore objects and derivates.
 * 
 * @author shermann
 */
public class MCRURNAdder {

    private static final Logger LOGGER = Logger.getLogger(MCRURNAdder.class);

    /**
     * This methods adds an URN to the metadata of a mycore object.
     * 
     * @param objectId
     * @return
     * @throws Exception
     */
    public boolean addURN(String objectId) throws Exception {
        // checking access right
        if (!MCRAccessManager.checkPermission(objectId, PERMISSION_WRITE)) {
            LOGGER.warn("Permission denied");
            return false;
        }

        MCRObjectID id = MCRObjectID.getInstance(objectId);
        if (isAllowedObject(id.getTypeId())) {
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(id);
            MCRMetaElement srcElement = obj.getMetadata().getMetadataElement("def.identifier");

            String urnToAssign = this.generateURN();

            /* objects with ppn have already the def.identifier element defined */
            /* no ppn -> no def.identifier element, thus it has to be created */
            if (srcElement == null) {
                ArrayList<MCRMetaInterface> list = new ArrayList<MCRMetaInterface>();
                srcElement = new MCRMetaElement(MCRMetaLangText.class, "def.identifier", false, true, list);
                obj.getMetadata().setMetadataElement(srcElement);
            }
            // adding the urn
            MCRMetaLangText urn = new MCRMetaLangText("identifier", "de", "urn", 0, "", urnToAssign);
            srcElement.addMetaObject(urn);

            String objId = obj.getId().toString();
            try {
                LOGGER.info("Updating metadata of object " + objId + " with URN " + urnToAssign + ".");
                MCRMetadataManager.update(obj);
            } catch (Exception ex) {
                LOGGER.error("Updating metadata of object " + objId + " with URN " + urnToAssign + " failed.", ex);
                return false;
            }
            try {
                LOGGER.info("Assigning urn " + urnToAssign + " to object " + objId + ".");
                MCRURNManager.assignURN(urnToAssign, objId);
            } catch (Exception ex) {
                LOGGER.error("Saving URN in database failed.", ex);
                return false;
            }
        }
        return true;
    }

    /**
     * This methods adds an URN to the metadata of a mycore object. The urn is
     * stored under the given xpath in the mycore object given by its id.
     * 
     * @param objectId
     *            the id of the mycore object (not to be a derivate)
     * @param xpath
     *            only xpath without wildcards etc. will work, attributes are
     *            allowed and so are namespaces. If there is more than one
     *            attribute to set the attributes must be separated by an
     *            " and ". E.g. invoking with <code>
     *              <pre>
     *                  .mycoreobject/metadata/def.identifier/identifier[@type='aType']
     *              </pre>
     *            </code>as xpath parameter will lead to the following Element<br/>
     *            <code>
     *              <pre>
     *                  &lt;def.identifier&gt;
     *                      &lt;identifier type="aType"&gt;urn:foo:bar&lt;/identifier&gt;
     *                  &lt;/def.identifier&gt; 
     *              </pre>    
     *            </code> stored directly under ./mycoreobject/metadata.<br/>
     *            Please note, only xpath starting with ./mycoreobject/metadata
     *            will be accepted.
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @throws Exception
     * 
     * @deprecated will be deleted without replacement
     */
    public boolean addURN(String objectId, String xpath) throws Exception {
        MCRObjectID id = MCRObjectID.getInstance(objectId);
        // checking access right
        if (!(MCRAccessManager.checkPermission(objectId, PERMISSION_WRITE) || isAllowedObject(id.getTypeId()))) {
            LOGGER.warn("Permission denied");
            return false;
        }

        MCRObject mcrobj = MCRMetadataManager.retrieveMCRObject(id);
        XPathExpression<Element> xp = XPathFactory.instance().compile("./mycoreobject/metadata", Filters.element());
        Document xml = mcrobj.createXML();
        Element metaData = xp.evaluateFirst(xml);

        if (metaData == null) {
            LOGGER.error("Could not resolve metadata element");
            return false;
        }
        String urn = generateURN();
        Element urnHoldingElement = createElementByXPath(xpath, urn);
        metaData.addContent(urnHoldingElement);

        try {
            LOGGER.info("Updating metadata of object " + objectId + " with URN " + urn + " [" + xpath + "]");
            MCRXMLMetadataManager.instance().update(id, xml, new Date());
        } catch (Exception ex) {
            LOGGER.error("Updating metadata of object " + objectId + " with URN " + urn + " failed. [" + xpath + "]", ex);
            return false;
        }
        try {
            LOGGER.info("Assigning urn " + urn + " to object " + objectId + ".");
            MCRURNManager.assignURN(urn, objectId);
        } catch (Exception ex) {
            LOGGER.error("Saving URN in database failed.", ex);
            return false;
        }

        return true;
    }

    /**
     * Creates an {@link Element} given by an xpath.
     * 
     * @param xpath
     *            the xpath
     * @return an Element as specified by the given xpath
     * @throws Exception
     */
    private Element createElementByXPath(String xpath, String urn) {
        String prefix = ".mycoreobject/metadata/";
        if (!xpath.startsWith(prefix)) {
            throw new IllegalArgumentException("XPath does not start with '" + prefix + "'");
        }

        String[] parts = xpath.substring(prefix.length()).split("/");
        /* build the root element */
        Element toReturn = getElement(parts[0]);
        for (Attribute a : getAttributes(parts[0])) {
            toReturn.setAttribute(a);
        }

        Element predecessor = toReturn;

        /* add the children */
        for (int i = 1; i < parts.length; i++) {
            List<Attribute> attributes = getAttributes(parts[i]);
            Element element = getElement(parts[i]);
            for (Attribute a : attributes) {
                element.setAttribute(a);
            }

            predecessor.addContent(element);
            predecessor = element;

            if (i == parts.length - 1) {
                element.setText(urn);
            }
        }

        return toReturn;
    }

    /**
     * Creates the element name from the given string which is part of an xpath.
     * 
     * @param s
     *            source string, part of an xpath
     * @return the element name
     */
    private Element getElement(String s) {
        String elementNamespace = null;
        Element toReturn = null;
        int nsEndIndex = s.indexOf(":");
        int attBeginIndex = s.indexOf("[");

        // if true -> namespace
        if (nsEndIndex != -1 && ((attBeginIndex > nsEndIndex) || (nsEndIndex != -1 && attBeginIndex == -1))) {
            elementNamespace = s.substring(0, nsEndIndex);
        }

        if (elementNamespace != null && attBeginIndex == -1) {
            toReturn = new Element(s.substring(nsEndIndex + 1), MCRConstants.getStandardNamespace(elementNamespace));
        } else if (elementNamespace != null && attBeginIndex != -1) {
            toReturn = new Element(s.substring(nsEndIndex + 1, attBeginIndex), MCRConstants.getStandardNamespace(elementNamespace));
        } else if (elementNamespace == null && attBeginIndex != -1) {
            toReturn = new Element(s.substring(0, attBeginIndex));
        } else if (elementNamespace == null && attBeginIndex == -1) {
            toReturn = new Element(s);
        }

        return toReturn;
    }

    /**
     * Creates a list of {@link Attribute} from the given string which is part
     * of an xpath.
     * 
     * @param s
     *            source string, part of an xpath
     * @return a list of {@link Attribute}, or an empty list, if there are no
     *         attributes at all
     */
    private List<Attribute> getAttributes(String s) {
        List<Attribute> list = new Vector<Attribute>();
        int beginIndex = s.indexOf("[");
        if (beginIndex == -1) {
            return new Vector<Attribute>();
        }

        String[] parts = s.substring(beginIndex + 1, s.indexOf("]")).split(" and ");

        for (String anAttribute : parts) {
            String attributeName = null;
            String namespace = null;

            /* examine attribute name */
            if (anAttribute.contains(":")) {
                // we have a namespace here -> namespace:attName=value
                attributeName = anAttribute.substring(1, anAttribute.indexOf("=")).substring(anAttribute.indexOf(":"));
                namespace = anAttribute.substring(1, anAttribute.indexOf(":"));
            } else {
                // no namespace here -> attName=value
                attributeName = anAttribute.substring(1, anAttribute.indexOf("="));
            }
            /* examine attribute value */
            String attributeValue = anAttribute.substring(anAttribute.indexOf("=") + 2, anAttribute.length() - 1);

            /* create an Attribute and add it to the result list */
            if (namespace == null) {
                list.add(new Attribute(attributeName, attributeValue));
            } else {
                list.add(new Attribute(attributeName, attributeValue, namespace.equals("xml") ? Namespace.XML_NAMESPACE : MCRConstants
                        .getStandardNamespace(namespace)));
            }
        }
        return list;
    }

    /**
     * Method generates a single URN with attached checksum.
     * 
     * @return an URN, as specified by the urn provider (class to be set in
     *         mycore.properties)
     * @throws Exception
     */
    private String generateURN() throws Exception {
        MCRIURNProvider urnProvider = this.getURNProvider();
        MCRURN myURN = urnProvider.generateURN();
        String myURNString = myURN.toString() + myURN.checksum();
        return myURNString;
    }

    /**
     * This methods adds a URN to the derivate of a mycore object and to all
     * files within this derivate
     * 
     * @param derivateId
     *            the derivate id
     * @throws SAXException 
     * @throws JDOMException 
     * @throws IOException 
     */
    public boolean addURNToDerivates(String derivateId) throws IOException, JDOMException, SAXException {
        MCRObjectID id = MCRObjectID.getInstance(derivateId);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);

        if (!parentIsAllowedObject(derivate)) {
            LOGGER.warn("Parent permission denied");
            return false;
        }
        /* Generating base urn for the derivate */
        LOGGER.info("Generating base urn for derivate " + derivate.getId().toString());
        MCRIURNProvider urnProvider;
        try {
            urnProvider = this.getURNProvider();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new MCRException("Could not get URN Provider.", e);
        }
        MCRURN parentURN = urnProvider.generateURN();
        parentURN.attachChecksum();

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(derivateId);

        if (node instanceof MCRDirectory) {
            /*
             * save the base urn in the database this will be the urn for the
             * complete book
             */
            try {
                LOGGER.info("Assigning urn " + parentURN.toString() + " to " + derivate.getId().toString());
                MCRURNManager.assignURN(parentURN.toString(), derivate.getId().toString(), null, null);
            } catch (Exception ex) {
                LOGGER.error("Assigning base urn " + parentURN.toString() + parentURN.checksum() + " to derivate " + derivate.getId().toString() + " failed.",
                        ex);
                return false;
            }

            // get the files/directories in the filesystem
            MCRFilesystemNode[] nodes = ((MCRDirectory) node).getChildren();
            /* the list containing the path-filename pairs */
            List<MCRPair<String, MCRFile>> pairs = new Vector<MCRPair<String, MCRFile>>();
            /* fill the list with the path-filename pairs */
            getPathFilenamePairs(nodes, pairs);
            Collections.sort(pairs);
            /* generate the urn based on the parent urn */
            String setId = derivate.getId().getNumberAsString();
            MCRURN[] urnToSet = urnProvider.generateURN(pairs.size(), parentURN, setId);
            derivate.getDerivate().setURN(parentURN.toString());
            for (int i = 0; i < pairs.size(); i++) {
                MCRPair<String, MCRFile> current = pairs.get(i);

                /* mets files should not get an urn assigned */
                String metsFileName = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
                if (current.getRightComponent().getName().equals(metsFileName)) {
                    continue;
                }

                LOGGER.info("Assigning urn " + urnToSet[i] + urnToSet[i].checksum() + " to " + current.getLeftComponent()
                        + current.getRightComponent().getName());
                /* save the urn in the database here */
                try {
                    MCRURNManager.assignURN(urnToSet[i].toString() + urnToSet[i].checksum(), derivate.getId().toString(), current.getLeftComponent(), current
                            .getRightComponent().getName());
                    /*
                     * updating the fileset element, with the current file and
                     * urn
                     */
                    MCRFileMetadata fileMetadata = derivate.getDerivate().getOrCreateFileMetadata(current.getRightComponent());
                    fileMetadata.setUrn(urnToSet[i].toString() + String.valueOf(urnToSet[i].checksum()));
                } catch (Exception ex) {
                    LOGGER.error("Assigning urn " + urnToSet[i] + urnToSet[i].checksum() + " to " + current.getLeftComponent()
                            + current.getRightComponent().getName() + " failed.", ex);
                    handleError(derivate);
                    return false;
                }
            }

            updateDerivateInDB(derivate);
        }
        return true;
    }

    /**
     * @param derivateId
     *            the id of the derivate
     * @param path
     *            the path to the file including the filename
     */
    public boolean addURNToSingleFile(String derivateId, String path) throws Exception {
        if (derivateId == null || path == null) {
            LOGGER.error("null not allowed as parameter. derivate=" + derivateId + ", path=" + path);
            return false;
        }
        MCRObjectID id = MCRObjectID.getInstance(derivateId);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);

        MCRObjectDerivate objectDerivate = derivate.getDerivate();
        if (objectDerivate.getURN() == null) {
            LOGGER.error("No URN for derivate. URN assignment to single file canceled");
            return false;
        }

        MCRIURNProvider provider = getURNProvider();
        MCRURN base = MCRURN.valueOf(objectDerivate.getURN());
        int fileCount = getFilesWithURNCount(derivate) + 1;
        MCRURN[] u = provider.generateURN(1, base, derivate.getId().getNumberAsString() + "-" + fileCount);
        MCRURN urn = u[0];
        urn.attachChecksum();

        LOGGER.info("Assigning urn " + urn + " to " + path);
        MCRURNManager.assignURN(urn.toString(), derivateId, path);
        objectDerivate.getOrCreateFileMetadata(path).setUrn(urn.toString());
        MCRMetadataManager.updateMCRDerivateXML(derivate);
        return true;
    }

    /**
     * @return the number of files with urn referenced by the derivate
     */
    private int getFilesWithURNCount(MCRDerivate derivate) throws Exception {
        int size = 0;
        for (MCRFileMetadata fileMetadata : derivate.getDerivate().getFileMetadata()) {
            if (fileMetadata.getUrn() != null) {
                size++;
            }
        }
        return size;
    }

    /**
     * Checks whether it is allowed to add URN to derivates.
     * 
     * @return <code>true</code> if it allowed to add urn to the owner of the
     *         derivate,<code>false</code> otherwise
     */
    private boolean parentIsAllowedObject(MCRDerivate derivate) throws IOException, JDOMException, SAXException {
        MCRMetaLinkID linkToParent = derivate.getDerivate().getMetaLink();
        MCRObjectID parentID = linkToParent.getXLinkHrefID();
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(parentID);
        return isAllowedObject(obj.getId().getTypeId());
    }

    /**
     * Reads the property "URN.Enabled.Objects".
     * 
     * @param givenType
     *            the type of the mycore object to check
     * @return <code>true</code> if the given type is in the list of allowed
     *         objects, <code>false</code> otherwise
     */
    private boolean isAllowedObject(String givenType) {
        if (givenType == null)
            return false;

        String propertyName = "MCR.URN.Enabled.Objects";
        String propertyValue = MCRConfiguration.instance().getString(propertyName);
        if (propertyValue == null || propertyValue.length() == 0) {
            LOGGER.error("URN assignment failed as the property \"" + propertyName + "\" is not set");
            return false;
        }

        String[] allowedTypes = propertyValue.split(",");
        for (String current : allowedTypes) {
            if (current.trim().equals(givenType.trim())) {
                return true;
            }
        }
        LOGGER.warn("URN assignment failed as the object type " + givenType + " is not in the list of allowed objects. See property \"" + propertyName + "\"");
        return false;
    }

    /**
     * @return
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private MCRIURNProvider getURNProvider() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String className = MCRConfiguration.instance().getString("MCR.URN.Provider.Class");
        LOGGER.info("Loading class " + className + " as IURNProvider");
        Class<MCRIURNProvider> c = (Class<MCRIURNProvider>) Class.forName(className);
        MCRIURNProvider provider = c.newInstance();
        return provider;
    }

    /**
     * @param m
     *            the source from which the path-filename pairs should be
     *            generated from
     * @param pairs
     *            the variable where the pairs are stored in
     */
    private void getPathFilenamePairs(MCRFilesystemNode[] m, List<MCRPair<String, MCRFile>> pairs) {
        if (pairs == null)
            return;
        for (MCRFilesystemNode aM : m) {
            if (aM instanceof MCRDirectory) {
                getPathFilenamePairs(((MCRDirectory) aM).getChildren(), pairs);
            }
            if (aM instanceof MCRFile) {
                pairs.add(new MCRPair<String, MCRFile>(getPath((MCRFile) aM), (MCRFile) aM));
            }
        }
    }

    /**
     * @param file
     * @return the path of the given file, the path terminates with an
     */
    private String getPath(MCRFile file) {
        String p = file.getAbsolutePath();
        int index = p.lastIndexOf("/");
        return p.substring(0, index + 1);
    }

    /**
     * Deletes the entries in the database. to be called if the changing of the
     * derivate metadata xml failes
     */
    private void handleError(MCRDerivate derivate) {
        LOGGER.error("Removing already assigned urn from derivate with id " + derivate.getId() + ".");
        try {
            MCRURNManager.removeURNByObjectID(derivate.getId().toString());
        } catch (Exception rmEx) {
            LOGGER.error("Removing already assigned urn from derivate with id " + derivate.getId() + " failed.", rmEx);
        }
    }

    /**
     * Updates the derivate in the database and does the appropriate error
     * handling if needed
     */
    private void updateDerivateInDB(MCRDerivate derivate) {
        try {
            // just update modified XML here, no new import of files pleaze
            MCRMetadataManager.updateMCRDerivateXML(derivate);
        } catch (Exception ex) {
            LOGGER.error("An exception occured while updating the object " + derivate.getId() + " in database. The adding of the fileset element failed.", ex);
            handleError(derivate);
        }
    }

}
