package org.mycore.migration2015.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(name = "MyCore 2015.0x migration")
public class MCRMigrationCommands {

    private static final Logger LOGGER = Logger.getLogger(MCRMigrationCommands.class);

    @MCRCommand(syntax = "migrate author servflags", help = "Create missing servflags for createdby and modifiedby. (MCR-786)", order = 20)
    public static List<String> addServFlags() {
        TreeSet<String> ids = new TreeSet<>(MCRXMLMetadataManager.instance().listIDs());
        ArrayList<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("migrate author servflags for " + id);
        }
        return cmds;
    }

    @MCRCommand(syntax = "migrate author servflags for {0}", help = "Create missing servflags for createdby and modifiedby for object {0}. (MCR-786)", order = 10)
    public static void addServFlags(String id)
        throws IOException, MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCRBase obj = MCRMetadataManager.retrieve(objectID);
        MCRObjectService service = obj.getService();
        if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) { //the egg
            MCRVersionedMetadata versionedMetadata = MCRXMLMetadataManager.instance().getVersionedMetaData(objectID);
            String createUser = null, modifyUser = null;
            if (versionedMetadata == null) {
                LOGGER.warn(
                    "Cannot restore author servflags as there are no versions available. Setting to current user.");
                createUser = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
                modifyUser = createUser;
            } else {
                List<MCRMetadataVersion> versions = versionedMetadata.listVersions();
                MCRMetadataVersion firstVersion = versions.get(0);
                for (MCRMetadataVersion version : versions) {
                    if (version.getType() == 'A') {
                        firstVersion = version; // get last 'added'
                    }
                }
                MCRMetadataVersion lastVersion = versions.get(versions.size() - 1);
                createUser = firstVersion.getUser();
                modifyUser = lastVersion.getUser();
            }
            service.addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, createUser);
            LOGGER.info(objectID + ", created by: " + createUser);
            if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_MODIFIEDBY)) { //the chicken
                //have to restore also modifiedby from version history.
                LOGGER.info(objectID + ", modified by: " + modifyUser);
                service.addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, modifyUser);
            }
            obj.setImportMode(true);
            if (obj instanceof MCRDerivate) {
                MCRMetadataManager.updateMCRDerivateXML((MCRDerivate) obj);
            } else {
                MCRMetadataManager.update((MCRObject) obj);
            }
        }
    }

    @MCRCommand(syntax = "fix invalid derivate links {0} for {1}", help = "Fixes the paths of all derivate links "
        + "({0} -> xpath -> e.g. /mycoreobject/metadata/derivateLinks/derivateLink) for object {1}. (MCR-1267)", order = 15)
    public static void fixDerivateLinks(String xpath, String id) throws IOException, JDOMException, SAXException {
        // get mcr object
        MCRObjectID objectID = MCRObjectID.getInstance(id);

        // find derivate links
        Document xml = MCRXMLMetadataManager.instance().retrieveXML(objectID);
        Element mcrObjectXML = xml.getRootElement();
        XPathExpression<Element> expression = XPathFactory.instance().compile(xpath, Filters.element());
        List<Element> derivateLinkElements = expression.evaluate(mcrObjectXML);

        // check them
        boolean changedObject = false;
        for (Element derivateLinkElement : derivateLinkElements) {
            String href = derivateLinkElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            MCRMetaDerivateLink link = new MCRMetaDerivateLink();
            link.setReference(href, null, null);
            String owner = link.getOwner();
            try {
                String path = link.getPath();
                MCRPath mcrPath = MCRPath.getPath(owner, path);
                if (!Files.exists(mcrPath)) {
                    // path is correct URI encoded but not found.
                    // this could have two reasons
                    // 1. the file does not exist on the file system
                    // 2. maybe the path isn't correct URI decoded
                    //    -> e.g. a?c.tif -> path (a), query (c.tif) which is obvious wrong
                    if (tryRawPath(objectID, derivateLinkElement, href, link, owner)) {
                        changedObject = true;
                    } else {
                        LOGGER.warn(href + " of " + objectID
                            + "cannot be found on file system. This is most likly a dead link.");
                    }
                }
            } catch (URISyntaxException uriExc) {
                // path could not be decoded, so maybe its already decoded
                // check if the file with href exists, if so, the path is
                // not encoded properly
                if (tryRawPath(objectID, derivateLinkElement, href, link, owner)) {
                    changedObject = true;
                } else {
                    LOGGER.warn(href + " of " + objectID
                        + " isn't URI encoded and cannot be found on file system. This is most likly a dead link.");
                }
            }
        }

        // store the mcr object if its changed
        if (changedObject) {
            // we use MCRXMLMetadataMananger because we don't want to validate the old mcr object
            MCRXMLMetadataManager.instance().update(objectID, xml, new Date());
            // manually fire update event
            MCRObject newObject = MCRMetadataManager.retrieveMCRObject(objectID);
            newObject.setImportMode(true);
            MCRMetadataManager.fireUpdateEvent(newObject);
        }
    }

    private static boolean tryRawPath(MCRObjectID objectID, Element derivateLinkElement, String href,
        MCRMetaDerivateLink link, String owner) {
        String rawPath = link.getRawPath();
        MCRPath mcrPath = MCRPath.getPath(owner, rawPath);
        if (Files.exists(mcrPath)) {
            // path exists -> do URI encoding for href
            try {
                String encodedHref = MCRXMLFunctions.encodeURIPath(rawPath);
                derivateLinkElement.setAttribute("href", owner + encodedHref, MCRConstants.XLINK_NAMESPACE);
                return true;
            } catch (URISyntaxException uriEncodeException) {
                LOGGER.error("Unable to encode " + rawPath + " for object " + objectID, uriEncodeException);
                return false;
            }
        }
        return false;
    }

    // fix invalid derivate links /mycoreobject/metadata/derivateLinks/derivateLink for jportal_jparticle_00000008

}
