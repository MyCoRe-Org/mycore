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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.xml.sax.SAXException;

/**
 * This class provides methods for adding URN to mycore objects and derivates.
 *
 * @author shermann
 * @author Kathleen Neumann (kkrebs)
 * @author Frank L\u00FCtzenkirchen (fluet)
 */
@Deprecated
public class MCRURNAdder {

    private static final Logger LOGGER = LogManager.getLogger(MCRURNAdder.class);

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

            MCRObject mcrobj = MCRMetadataManager.retrieveMCRObject(id);
            String type = mcrobj.getId().getTypeId();

            MCRConfiguration conf = MCRConfiguration.instance();
            String xPathString = conf.getString("MCR.Persistence.URN.XPath." + type,
                conf.getString("MCR.Persistence.URN.XPath",
                    "/mycoreobject/metadata/def.identifier[@class='MCRMetaLangText']/identifier[@type='urn']"));

            String urnToAssign = this.generateURN();

            try {
                LOGGER.info(
                    "Updating metadata of object " + objectId + " with URN " + urnToAssign + " [" + xPathString + "]");
                Document xml = mcrobj.createXML();
                MCRNodeBuilder nb = new MCRNodeBuilder();
                nb.buildElement(xPathString, urnToAssign, xml);
                mcrobj = new MCRObject(xml);
                MCRMetadataManager.update(mcrobj);
            } catch (Exception ex) {
                LOGGER.error("Updating metadata of object " + objectId + " with URN " + urnToAssign + " failed. ["
                    + xPathString + "]", ex);
                return false;
            }
        }

        return true;
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
        return myURN.toString();

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
        MCRURN parentURN = MCRURN.parse(derivate.getDerivate().getURN());

        Collections.sort(files);
        /* generate the urn based on the parent urn */
        String setId = derivate.getId().getNumberAsString();
        MCRURN[] urnToSet = urnProvider.generateURN(files.size(), parentURN, setId);
        for (int i = 0; i < urnToSet.length; i++) {
            MCRPath currentFile = files.get(i);
            MCRURN currentURN = urnToSet[i];
            String urnString = currentURN.toString();
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
    public boolean addURNToDerivate(String derivateId) throws IOException, JDOMException, SAXException, MCRException {
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

        /*
         * save the base urn in the database this will be the urn for the
         * complete book
         */
        try {
            LOGGER.info("Assigning urn " + parentURN.toString() + " to " + derivate.getId().toString());
            MCRURNManager.assignURN(parentURN.toString(), derivate.getId().toString(), null, null);
        } catch (Exception ex) {
            LOGGER.error("Assigning base urn " + parentURN.toString() + " to derivate " + derivate.getId().toString()
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
        MCRURN base = MCRURN.parse(objectDerivate.getURN());
        int fileCount = getFilesWithURNCount(derivate) + 1;
        MCRURN[] u = provider.generateURN(1, base, derivate.getId().getNumberAsString() + "-" + fileCount);
        MCRURN urn = u[0];

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
        return (int) derivate.getDerivate()
            .getFileMetadata()
            .stream()
            .filter(fileMetadata -> fileMetadata.getUrn() != null)
            .count();
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
        LOGGER.warn("URN assignment failed as the object type " + givenType
            + " is not in the list of allowed objects. See property \""
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
    private MCRIURNProvider getURNProvider()
        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
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
