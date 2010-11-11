package org.mycore.services.urn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRURNAdder {

    private static final Logger LOGGER = Logger.getLogger(MCRURNAdder.class);

    /** This methods adds a URN to the metadata of a cbu mycore object */
    public boolean addURN(String objectId) throws Exception {
        // checking access right
        if (!MCRAccessManager.checkPermission(objectId, "writedb")) {
            LOGGER.warn("Permission denied");
            return false;
        }

        MCRObjectID id = MCRObjectID.getInstance(objectId);
        if (isAllowedObject(id.getTypeId())) {
            MCRObject obj = MCRMetadataManager.retrieveMCRObject(id);
            MCRMetaElement srcElement = obj.getMetadata().getMetadataElement("def.identifier");
            IURNProvider urnProvider = this.getURNProvider();
            URN myURN = urnProvider.generateURN();
            String myURNString = myURN.toString() + myURN.checksum();

            /* objects with ppn have already the def.identifier element defined */
            /* no ppn -> no def.identifier element, thus it has to be created */
            if (srcElement == null) {
                ArrayList<MCRMetaInterface> list = new ArrayList<MCRMetaInterface>();
                srcElement = new MCRMetaElement(MCRMetaLangText.class, "def.identifier", false, true, list);
                obj.getMetadata().setMetadataElement(srcElement);
            }
            // adding the urn
            MCRMetaLangText urn = new MCRMetaLangText("identifier", "de", "urn", 0, "", myURNString);
            srcElement.addMetaObject(urn);

            String objId = obj.getId().toString();
            try {
                LOGGER.debug("Updating metadata of object " + objId + " with URN " + myURNString + ".");
                MCRMetadataManager.update(obj);
            } catch (Exception ex) {
                LOGGER.error("Updating metadata of object " + objId + " with URN " + myURNString + " failed.", ex);
                return false;
            }
            try {
                LOGGER.info("Assigning urn " + myURNString + " to object " + objId + ".");
                MCRURNManager.assignURN(myURNString, objId);
            } catch (Exception ex) {
                LOGGER.error("Saving URN in database failed.", ex);
                return false;
            }
        }
        return true;
    }

    /** This methods adds a URN to the derivates mycore object and to all files within this derivate */
    public boolean addURNToDerivates(String derivateId) throws Exception {
        //checking access right
        if (!MCRAccessManager.checkPermission(derivateId, "writedb")) {
            LOGGER.warn("Permission denied");
            return false;
        }
        MCRObjectID id = MCRObjectID.getInstance(derivateId);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);

        if (!parentIsAllowedObject(derivate)) {
            LOGGER.warn("Parent permission denied");
            return false;
        }
        /* Generating base urn for the derivate */
        LOGGER.info("Generating base urn for derivate " + derivate.getId().toString());
        IURNProvider urnProvider = this.getURNProvider();
        URN parentURN = urnProvider.generateURN();
        parentURN.attachChecksum();

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(derivateId);

        if (node instanceof MCRDirectory) {
            /*
             * save the base urn in the database this will be the urn for the
             * complete book
             */
            try {
                LOGGER.info("Assigning urn " + parentURN.toString() + " to " + derivate.getId().toString());
                MCRURNManager.assignURN(parentURN.toString(), derivate.getId().toString(), " ", " ");
            } catch (Exception ex) {
                LOGGER.error("Assigning base urn " + parentURN.toString() + parentURN.checksum() + " to derivate " + derivate.getId().toString() + " failed.",
                    ex);
                return false;
            }

            // get the files/directories in the filesystem
            MCRFilesystemNode[] nodes = ((MCRDirectory) node).getChildren();
            /* the list containing the path-filename pairs */
            List<Pair<String, MCRFile>> pairs = new Vector<Pair<String, MCRFile>>();
            /* fill the list with the path-filename pairs */
            getPathFilenamePairs(nodes, pairs);
            Collections.sort(pairs);
            /* generate the urn based on the parent urn */
            String setId = derivate.getId().getNumberAsString();
            URN[] urnToSet = urnProvider.generateURN(pairs.size(), parentURN, setId);
            Element fileset = new Element("fileset");
            fileset.setAttribute("urn", parentURN.toString());
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String, MCRFile> current = pairs.get(i);
                LOGGER.info("Assigning urn " + urnToSet[i] + urnToSet[i].checksum() + " to " + current.getLeftComponent()
                    + current.getRightComponent().getName());
                /* save the urn in the database here */
                try {
                    MCRURNManager.assignURN(urnToSet[i].toString() + urnToSet[i].checksum(), derivate.getId().toString(), current.getLeftComponent(), current
                        .getRightComponent()
                        .getName());
                    /* updating the fileset element, with the current file and urn */
                    addToFilesetElement(fileset, urnToSet[i], current);
                } catch (Exception ex) {
                    LOGGER.error("Assigning urn " + urnToSet[i] + urnToSet[i].checksum() + " to " + current.getLeftComponent()
                        + current.getRightComponent().getName() + " failed.", ex);
                    fileset = null;
                }
            }

            /* an error has occured */
            if (fileset == null) {
                handleError(derivate);
                return false;
            } else {
                updateDerivateInDB(derivate, fileset);
            }
        }
        return true;
    }

    /**
     * @param derivateId the id of the derivate
     * @param path the path to the file including the filename
     * */
    public boolean addURNToSingleFile(String derivateId, String path, String fileId) throws Exception {
        if (derivateId == null || path == null || fileId == null) {
            LOGGER.error("null not allowed as parameter. derivate=" + derivateId + ", path=" + path + ", fileId=" + fileId);
            return false;
        }
        MCRObjectID id = MCRObjectID.getInstance(derivateId);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);

        Document xml = derivate.createXML();
        XPath xp = XPath.newInstance("./mycorederivate/derivate/fileset");
        Object obj = xp.selectSingleNode(xml);
        if (obj == null) {
            LOGGER.error("No fileset element in derivate. URN assignment to single file canceled");
            return false;
        }
        IURNProvider provider = getURNProvider();
        URN base = provider.generateURN();
        URN[] u = provider.generateURN(1, base, derivate.getId().getNumberAsString());
        u[0].attachChecksum();

        LOGGER.info("Assigning urn " + u[0] + " to " + path);
        int i = path.lastIndexOf("/");
        String file = path.substring(i + 1);
        String pathDb = path.substring(0, i + 1);

        MCRURNManager.assignURN(u[0].toString(), derivateId, pathDb, file);

        //reference to fileset element
        Element fs = (Element) obj;
        //reference to the new file element
        Element f = new Element("file");
        f.setAttribute("name", path).setAttribute("ifsid", fileId);
        f.addContent(new Element("urn").setText(u[0].toString()));
        fs.addContent(f);

        MCRDerivate update = new MCRDerivate(xml);
        MCRMetadataManager.updateMCRDerivateXML(update);

        return true;
    }

    /**
     * Checks whether it is allowed to add URN to derivates.
     * 
     * @return <code>true</code> if it allowed to add urn to the owner of the
     *         derivate,<code>false</code> otherwise
     */
    private boolean parentIsAllowedObject(MCRDerivate derivate) {
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
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private IURNProvider getURNProvider() throws Exception {
        String className = MCRConfiguration.instance().getString("MCR.URN.Provider.Class");
        LOGGER.info("Loading class " + className + " as IURNProvider");
        Class<IURNProvider> c = (Class<IURNProvider>) Class.forName(className);
        IURNProvider provider = c.newInstance();
        return provider;
    }

    /**
     * @param m
     *            the source from which the path-filename pairs should be
     *            generated from
     * @param pairs
     *            the variable where the pairs are stored in
     */
    private void getPathFilenamePairs(MCRFilesystemNode[] m, List<Pair<String, MCRFile>> pairs) {
        if (pairs == null)
            return;
        for (int i = 0; i < m.length; i++) {
            if (m[i] instanceof MCRDirectory) {
                getPathFilenamePairs(((MCRDirectory) m[i]).getChildren(), pairs);
            }
            if (m[i] instanceof MCRFile) {
                pairs.add(new Pair<String, MCRFile>(getPath((MCRFile) m[i]), (MCRFile) m[i]));
            }
        }
    }

    /** Adds a file element to the fileset element */
    private void addToFilesetElement(Element fileset, URN urn, Pair<String, MCRFile> currentFile) throws Exception {
        Element fileElement = new Element("file");
        fileElement.setAttribute("name", currentFile.getRightComponent().getAbsolutePath());
        fileElement.setAttribute("ifsid", currentFile.getRightComponent().getID());
        Element urnElement = new Element("urn");
        urnElement.addContent(urn.toString() + String.valueOf(urn.checksum()));
        fileElement.addContent(urnElement);
        fileset.addContent(fileElement);
    }

    /**
     *@param file
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
    private void updateDerivateInDB(MCRDerivate derivate, Element fileset) {
        if (derivate == null || fileset == null)
            return;
        MCRObjectDerivate objDer = derivate.getDerivate();
        alterDerivateDOM(objDer, fileset);

        try {
            //just update modified XML here, no new import of files pleaze
            MCRMetadataManager.updateMCRDerivateXML(derivate);
        } catch (Exception ex) {
            LOGGER.error("An exception occured while updating the object " + derivate.getId() + " in database. The adding of the fileset element failed.", ex);
            handleError(derivate);
        }
    }

    /** Adds the fileset element to the derivate element */
    private void alterDerivateDOM(MCRObjectDerivate objDer, Element fileset) {
        Element dom = objDer.createXML();
        if (dom != null && dom.getName().equals("derivate")) {
            dom.addContent(fileset);
        }
        objDer.setFromDOM(dom);
    }

}
