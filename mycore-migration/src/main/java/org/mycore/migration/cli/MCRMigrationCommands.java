/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.migration.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.links.MCRLINKHREF;
import org.mycore.backend.jpa.links.MCRLINKHREFPK_;
import org.mycore.backend.jpa.links.MCRLINKHREF_;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.iview2.services.MCRTileJob;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(name = "MyCoRe migration")
public class MCRMigrationCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "migrate author servflags",
        help = "Create missing servflags for createdby and modifiedby. (MCR-786)",
        order = 20)
    public static List<String> addServFlags() {
        TreeSet<String> ids = new TreeSet<>(MCRXMLMetadataManager.instance().listIDs());
        ArrayList<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("migrate author servflags for " + id);
        }
        return cmds;
    }

    @MCRCommand(syntax = "migrate author servflags for {0}",
        help = "Create missing servflags for createdby and modifiedby for object {0}. (MCR-786)",
        order = 10)
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
            LOGGER.info("{}, created by: {}", objectID, createUser);
            if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_MODIFIEDBY)) { //the chicken
                //have to restore also modifiedby from version history.
                LOGGER.info("{}, modified by: {}", objectID, modifyUser);
                service.addFlag(MCRObjectService.FLAG_TYPE_MODIFIEDBY, modifyUser);
            }
            obj.setImportMode(true);
            MCRMetadataManager.update(obj);
        }
    }

    @MCRCommand(syntax = "fix MCR-1717", help = "Fixes wrong entries in tile job table (see MCR-1717 comments)")
    public static void fixMCR1717() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRTileJob> allTileJobQuery = em.createNamedQuery("MCRTileJob.all", MCRTileJob.class);
        List<MCRTileJob> tiles = allTileJobQuery.getResultList();
        tiles.stream()
            .filter(tj -> !tj.getPath().startsWith("/"))
            .peek(tj -> LOGGER.info("Fixing TileJob {}:{}", tj.getDerivate(), tj.getPath()))
            .forEach(tj -> {
                String newPath = "/" + tj.getPath();
                tj.setPath(newPath);
            });
    }

    @MCRCommand(syntax = "fix invalid derivate links {0} for {1}",
        help = "Fixes the paths of all derivate links "
            + "({0} -> xpath -> e.g. /mycoreobject/metadata/derivateLinks/derivateLink) for object {1}. (MCR-1267)",
        order = 15)
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
                        LOGGER.warn("{} of {}cannot be found on file system. This is most likly a dead link.", href,
                            objectID);
                    }
                }
            } catch (URISyntaxException uriExc) {
                // path could not be decoded, so maybe its already decoded
                // check if the file with href exists, if so, the path is
                // not encoded properly
                if (tryRawPath(objectID, derivateLinkElement, href, link, owner)) {
                    changedObject = true;
                } else {
                    LOGGER.warn(
                        "{} of {} isn't URI encoded and cannot be found on file system."
                            + " This is most likly a dead link.",
                        href, objectID);
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
                LOGGER.error("Unable to encode {} for object {}", rawPath, objectID, uriEncodeException);
                return false;
            }
        }
        return false;
    }

    @MCRCommand(syntax = "add missing children to {0}",
        help = "Adds missing children to structure of parent {0}. (MCR-1480)",
        order = 15)
    public static void fixMissingChildren(String id) throws IOException, JDOMException, SAXException {
        MCRObjectID parentId = MCRObjectID.getInstance(id);
        Collection<String> children = MCRLinkTableManager.instance().getSourceOf(parentId,
            MCRLinkTableManager.ENTRY_TYPE_PARENT);
        if (children.isEmpty()) {
            return;
        }
        MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentId);
        MCRObjectStructure parentStructure = parent.getStructure();
        int sizeBefore = parentStructure.getChildren().size();
        children.stream().map(MCRObjectID::getInstance)
            .filter(cid -> !parentStructure.getChildren().stream()
                .anyMatch(candidate -> candidate.getXLinkHrefID().equals(cid)))
            .sorted().map(MCRMigrationCommands::toLinkId).sequential()
            .peek(lid -> LOGGER.info("Adding {} to {}", lid, parentId)).forEach(parentStructure::addChild);
        if (parentStructure.getChildren().size() != sizeBefore) {
            MCRMetadataManager.fireUpdateEvent(parent);
        }
    }

    private static MCRMetaLinkID toLinkId(MCRObjectID mcrObjectID) {
        return new MCRMetaLinkID("child", mcrObjectID, null, null);
    }

    @MCRCommand(syntax = "add missing children",
        help = "Adds missing children to structure of parent objects using MCRLinkTableManager. (MCR-1480)",
        order = 20)
    public static List<String> fixMissingChildren() throws IOException, JDOMException, SAXException {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRLINKHREF> ac = query.from(MCRLINKHREF.class);
        return em
            .createQuery(query
                .select(cb.concat(cb.literal("add missing children to "),
                    ac.get(MCRLINKHREF_.key).get(MCRLINKHREFPK_.mcrto)))
                .where(cb.equal(ac.get(MCRLINKHREF_.key).get(MCRLINKHREFPK_.mcrtype),
                    MCRLinkTableManager.ENTRY_TYPE_PARENT))
                .distinct(true).orderBy(cb.asc(cb.literal(1))))
            .getResultList();
    }

    // 2017-> 2018
    @MCRCommand(syntax = "migrate tei entries in mets file of derivate {0}")
    public static void migrateTEIEntrysOfMetsFileOfDerivate(String derivateIdStr)
        throws IOException, JDOMException, SAXException {
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIdStr);
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.info("Derivate " + derivateIdStr + " does not exist!");
            return;
        }

        final MCRPath metsPath = MCRPath.getPath(derivateIdStr, "mets.xml");
        if (!Files.exists(metsPath)) {
            LOGGER.info("Derivate " + derivateIdStr + " has not mets.xml!");
            return;
        }

        final MCRXSLTransformer transformer = MCRXSLTransformer.getInstance("xsl/mets-translation-migration.xsl");

        final Document xml;

        try (InputStream is = Files.newInputStream(metsPath)) {
            final MCRContent content = transformer.transform(new MCRStreamContent(is));
            xml = content.asXML();
        }

        try (OutputStream os = Files.newOutputStream(metsPath, StandardOpenOption.TRUNCATE_EXISTING)) {
            final XMLOutputter writer = new XMLOutputter(Format.getPrettyFormat());
            writer.output(xml, os);

        }

        LOGGER.info("Migrated mets of " + derivateIdStr);
    }

    // 2018 -> 2019
    @MCRCommand(syntax = "migrate all derivates",
        help = "Migrates the order and label of all derivates (MCR-2003, MCR-2099)")
    public static List<String> migrateAllDerivates() {
        List<String> objectTypes = MCRObjectID.listTypes();
        objectTypes.remove("derivate");
        objectTypes.remove("class");

        ArrayList<String> commands = new ArrayList<>();
        for (String t : objectTypes) {
            for (String objID : MCRXMLMetadataManager.instance().listIDsOfType(t)) {
                commands.add("migrate derivatelinks for object " + objID);
            }
        }
        return commands;
    }

    @MCRCommand(syntax = "migrate derivatelinks for object {0}",
        help = "Migrates the Order of derivates from object {0} to derivate "
            + "(MCR-2003, MCR-2099)")
    public static List<String> migrateDerivateLink(String objectIDStr) {
        final MCRObjectID objectID = MCRObjectID.getInstance(objectIDStr);

        if (!MCRMetadataManager.exists(objectID)) {
            throw new MCRException("The object " + objectIDStr + "does not exist!");
        }

        final MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objectID);
        final List<MCRMetaEnrichedLinkID> derivates = mcrObject.getStructure().getDerivates();

        return derivates.stream().map(
            (der) -> "migrate derivate " + der.getXLinkHrefID() + " using order " + (derivates.indexOf(der) + 1))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "migrate derivate {0} using order {1}",
        help = "Sets the order of derivate {0} to the number {1}")
    public static void setOrderOfDerivate(String derivateIDStr, String orderStr) throws MCRAccessException {
        final int order = Integer.parseInt(orderStr);

        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDStr);

        if (!MCRMetadataManager.exists(derivateID)) {
            throw new MCRException("The object " + derivateIDStr + "does not exist!");
        }

        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        derivate.setOrder(order);

        //migrate title:
        //in professorenkatalog we used a service flag to store the title -> should be moved to titles/tile
        if (derivate.getService().getFlags("title").size() > 0) {
            String title = derivate.getService().getFlags("title").get(0);
            derivate.getDerivate().getTitles().add(new MCRMetaLangText("title", "de", null, 0, "main", title));
            derivate.getService().removeFlags("title");
        }

        //update derivate
        MCRMetadataManager.update(derivate);
    }
}
