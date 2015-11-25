package org.mycore.urn.services;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
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
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.niofs.MCRPath;
import org.xml.sax.SAXException;

/**
 * This class provides methods for adding URN to mycore objects and derivates.
 *
 * @author shermann
 * @author Kathleen Neumann (kkrebs)
 */
public class MCRURNAdder {

    private static final Logger LOGGER = Logger.getLogger(MCRURNAdder.class);

    /**
     * This methods adds an URN to the metadata of a mycore object.
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
     *            " and ". E.g. invoking with
     *              <code>
     *                  /mycoreobject/metadata/def.identifier/identifier[@type='aType']
     *              </code>
     *            as xpath parameter will lead to the following Element<br>
     *              <pre>
     * &lt;def.identifier&gt;
     *  &lt;identifier type="aType"&gt;urn:foo:bar&lt;/identifier&gt;
     * &lt;/def.identifier&gt;</pre>
     *            stored directly under /mycoreobject/metadata.<br>
     *            Please note, only xpath starting with /mycoreobject/metadata
     *            will be accepted.
     * @return <code>true</code> if successful, <code>false</code> otherwise
     *
     * @deprecated doesn't work that generic, need another solution
     *
     */
    public boolean addURN(String objectId, String xpath) {
        MCRObjectID id = MCRObjectID.getInstance(objectId);
        // checking access right
        if (!(MCRAccessManager.checkPermission(objectId, PERMISSION_WRITE) || isAllowedObject(id.getTypeId()))) {
            LOGGER.warn("Permission denied");
            return false;
        }

        MCRObject mcrobj = MCRMetadataManager.retrieveMCRObject(id);
        MCRObjectMetadata mcrmetadata = mcrobj.getMetadata();

        String urn;
        try {
            urn = generateURN();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Could not generate URN", e);
            return false;
        }
        Element urnHoldingElement = createElementByXPath(xpath, urn);
        MCRObjectMetadata urnmetadata = new MCRObjectMetadata();
        urnmetadata.setFromDOM(urnHoldingElement);

        try {
            LOGGER.info("Updating metadata of object " + objectId + " with URN " + urn + " [" + xpath + "]");
            mcrmetadata.mergeMetadata(urnmetadata);
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
        String prefix = "/mycoreobject/metadata/";
        if (!xpath.startsWith(prefix)) {
            throw new IllegalArgumentException("XPath does not start with '" + prefix + "'");
        }

        String[] parts = xpath.split("/");
        /* build the element starting with metadata */
        Element toReturn = getElement(parts[2]);
        for (Attribute a : getAttributes(parts[2])) {
            toReturn.setAttribute(a);
        }

        Element predecessor = toReturn;

        /* add the children */
        for (int i = 3; i < parts.length; i++) {
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
                list.add(new Attribute(attributeName, attributeValue,
                        namespace.equals("xml") ? Namespace.XML_NAMESPACE : MCRConstants.getStandardNamespace(namespace)));
            }
        }
        return list;
    }

    /**
     * Method generates a single URN with attached checksum.
     *
     * @return an URN, as specified by the urn provider (class to be set in
     *         mycore.properties)
     */
    private String generateURN() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        MCRIURNProvider urnProvider = this.getURNProvider();
        MCRURN myURN = urnProvider.generateURN();
        String myURNString = myURN.toString() + myURN.checksum();
        return myURNString;
    }

    /**
     * This methods adds a URN to the derivate of a mycore object and to all
     * files within this derivate.
     *
     * @param derivateId
     *            the derivate id
     */
    public boolean addURNToDerivates(String derivateId) throws IOException, JDOMException, SAXException {
        // create parent URN and add it to derivate
        boolean successful = addURNToDerivate(derivateId);

        if (!successful) {
            LOGGER.warn("Could set urn for derivate " + derivateId);
            return false;
        }

        MCRObjectID id = MCRObjectID.getInstance(derivateId);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);
        MCRPath rootPath = MCRPath.getPath(derivateId, "/");

        final List<MCRPath> files = new ArrayList<>();

        final String metsFileName = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().equalsIgnoreCase(metsFileName)) {
                    files.add(MCRPath.toMCRPath(file));
                }
                return super.visitFile(file, attrs);
            }

        });

        MCRIURNProvider urnProvider;
        try {
            urnProvider = this.getURNProvider();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new MCRException("Could not get URN Provider.", e);
        }
        MCRURN parentURN = MCRURN.valueOf(derivate.getDerivate().getURN());

        Collections.sort(files);
        /* generate the urn based on the parent urn */
        String setId = derivate.getId().getNumberAsString();
        MCRURN[] urnToSet = urnProvider.generateURN(files.size(), parentURN, setId);
        for (int i = 0; i < urnToSet.length; i++) {
            MCRPath currentFile = files.get(i);
            MCRURN currentURN = urnToSet[i];
            String urnString = currentURN.toString() + currentURN.checksum();
            LOGGER.info("Assigning urn " + urnString + " to " + currentFile);
            /* save the urn in the database here */
            try {
                MCRURNManager.assignURN(urnString, currentFile);
                /*
                 * updating the fileset element, with the current file and
                 * urn
                 */
                MCRFileMetadata fileMetadata = derivate.getDerivate().getOrCreateFileMetadata(currentFile);
                fileMetadata.setUrn(urnString);
            } catch (Exception ex) {
                LOGGER.error("Assigning urn " + urnString + " to " + currentFile + " failed.", ex);
                handleError(derivate);
                return false;
            }
        }

        updateDerivateInDB(derivate);
        return true;
    }

    /**
     * This method adds a URN to the derivate of a mycore object.
     *
     * @param derivateId
     *          the id of the derivate
     */
    public boolean addURNToDerivate(String derivateId) throws IOException, JDOMException, SAXException {
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

        /*
         * save the base urn in the database this will be the urn for the
         * complete book
         */
        try {
            LOGGER.info("Assigning urn " + parentURN.toString() + " to " + derivate.getId().toString());
            MCRURNManager.assignURN(parentURN.toString(), derivate.getId().toString(), null, null);
        } catch (Exception ex) {
            LOGGER.error("Assigning base urn " + parentURN.toString() + parentURN.checksum() + " to derivate " + derivate.getId().toString()
                    + " failed.", ex);
            return false;
        }

        derivate.getDerivate().setURN(parentURN.toString());
        updateDerivateInDB(derivate);
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
        LOGGER.warn("URN assignment failed as the object type " + givenType + " is not in the list of allowed objects. See property \""
                + propertyName + "\"");
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
     * Deletes the entries in the database. to be called if the changing of the
     * derivate metadata xml fails
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
            // just update modified XML here, no new import of files please
            MCRMetadataManager.updateMCRDerivateXML(derivate);
        } catch (Exception ex) {
            LOGGER.error("An exception occured while updating the object " + derivate.getId()
                    + " in database. The adding of the fileset element failed.", ex);
            handleError(derivate);
        }
    }

}
