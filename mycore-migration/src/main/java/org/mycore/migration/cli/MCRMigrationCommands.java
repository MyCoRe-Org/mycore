/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.Optional;

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
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRExpandedObjectStructure;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.iview2.services.MCRTileJob;
import org.mycore.migration.strategy.ChildrenOrderMigrationStrategy;
import org.mycore.migration.strategy.NeverAddChildrenOrderStrategy;
import org.xml.sax.SAXException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(name = "MyCoRe migration")
public class MCRMigrationCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CHILDREN_ORDER_STRATEGY_PROPERTY = "MCR.Migration.ChildrenOrder.Strategy.Class";

    public static final String MIGRATE_NORMALIZED_OBJECT = "migrate to normalized object {0}";

    @MCRCommand(syntax = MIGRATE_NORMALIZED_OBJECT,
        help = "Migrates an object to a normalized one (MCR-3375). "
            + "Uses strategy defined in " + CHILDREN_ORDER_STRATEGY_PROPERTY
            + " (default: NeverAddChildrenOrderStrategy) to decide if <children> should become <childrenOrder>.",
        order = 30)
    public static void migrateNormalizedObject(String mcrObjectIDStr) throws IOException, JDOMException {
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.getInstance();

        // TODO: check if child order needs to be migrated

        MCRObjectID objectID = MCRObjectID.getInstance(mcrObjectIDStr);
        if (!mm.exists(objectID)) {
            LOGGER.error("Object {} does not exist!", mcrObjectIDStr);
            return;
        }

        Document document = mm.retrieveXML(objectID);

        if (document == null) {
            LOGGER.error("Object {} has no XML!", mcrObjectIDStr);
            return;
        }

        // Load the strategy for childrenOrder migration
        ChildrenOrderMigrationStrategy strategy = MCRConfiguration2
            .getSingleInstanceOf(ChildrenOrderMigrationStrategy.class, CHILDREN_ORDER_STRATEGY_PROPERTY)
            .orElseGet(() -> {
                LOGGER.info("No strategy configured for '{}', using default: NeverAddChildrenOrderStrategy",
                    CHILDREN_ORDER_STRATEGY_PROPERTY);
                return new NeverAddChildrenOrderStrategy();
            });
        LOGGER.debug("Using ChildrenOrderMigrationStrategy: {}", strategy.getClass().getName());

        Document oldDocument = document.clone();
        Element rootElement = document.getRootElement();

        Element structureElement = rootElement.getChild(MCRObjectStructure.XML_NAME);
        if (structureElement != null) {

            Element derivatesElement = structureElement.getChild(MCRObjectStructure.ELEMENT_DERIVATE_OBJECTS);

            if (derivatesElement != null) {
                // trigger repair on all derivates to recreate the derivate links in the right direction
                List<Element> derObjects = derivatesElement.getChildren("derobject");
                derObjects.stream()
                        .map(derObject -> derObject.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE))
                        .map(MCRObjectID::getInstance)
                        .map(MCRMetadataManager::retrieveMCRDerivate)
                        .forEach(MCRMetadataManager::fireRepairEvent);
            }

            Element childrenElement = structureElement.getChild(MCRExpandedObjectStructure.CHILDREN_ELEMENT_NAME);
            if (childrenElement != null) {
                if (strategy.shouldAddChildrenOrder(objectID, document)) {
                    LOGGER.info("Migrating <children> to <childrenOrder> for object {} based on strategy {}",
                        mcrObjectIDStr, strategy.getClass().getSimpleName());
                    childrenElement.setName(MCRObjectStructure.CHILDREN_ORDER_ELEMENT_NAME);
                    List<Element> children = childrenElement.getChildren(MCRObjectStructure.CHILD_ELEMENT_NAME);

                    for (Element child : children) {
                        child.removeAttribute("title", MCRConstants.XLINK_NAMESPACE);
                        child.removeAttribute("inherited");
                    }
                } else {
                    LOGGER.info("Skipping <children> migration for object {} based on strategy {}",
                        mcrObjectIDStr, strategy.getClass().getSimpleName());
                    // Remove the old <children> element as it's not needed in the normalized structure
                    // and the strategy decided against migrating it to <childrenOrder>.
                    // MCRMetadataManager.normalizeObject will handle the structure correctly later.
                    // However, explicitly removing it here makes the intent clearer for this migration step.
                    structureElement.removeChild(MCRExpandedObjectStructure.CHILDREN_ELEMENT_NAME);
                }
            }
        }


        MCRObject object = new MCRObject(document);

        MCRMetadataManager.validateObject(object);
        MCRMetadataManager.normalizeObject(object);

        Document afterMigration = object.createXML();

        if (!MCRXMLHelper.deepEqual(oldDocument, afterMigration)) {
            LOGGER.info("Object {} has changed after migration... Save it!", mcrObjectIDStr);
            mm.update(objectID, afterMigration, new Date());
        }
    }

    @MCRCommand(syntax = "migrate author servflags",
        help = "Create missing servflags for createdby and modifiedby. (MCR-786)",
        order = 20)
    public static List<String> addServFlags() {
        SortedSet<String> ids = new TreeSet<>(MCRXMLMetadataManager.getInstance().listIDs());
        List<String> cmds = new ArrayList<>(ids.size());
        for (String id : ids) {
            cmds.add("migrate author servflags for " + id);
        }
        return cmds;
    }

    @MCRCommand(syntax = "migrate author servflags for {0}",
        help = "Create missing servflags for createdby and modifiedby for object {0}. (MCR-786)",
        order = 10)
    public static void addServFlags(String id)
        throws IOException, MCRPersistenceException, MCRAccessException {
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        MCRBase obj = MCRMetadataManager.retrieve(objectID);
        MCRObjectService service = obj.getService();
        if (!service.isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) { //the egg
            List<? extends MCRAbstractMetadataVersion<?>> versions = MCRXMLMetadataManager.getInstance()
                .listRevisions(objectID);
            String createUser;
            String modifyUser;
            if (versions == null) {
                LOGGER.warn(
                    "Cannot restore author servflags as there are no versions available. Setting to current user.");
                createUser = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
                modifyUser = createUser;
            } else {
                MCRAbstractMetadataVersion<?> firstVersion = versions.getFirst();
                for (MCRAbstractMetadataVersion<?> version : versions) {
                    if (version.getType() == 'A') {
                        firstVersion = version; // get last 'added'
                    }
                }
                MCRAbstractMetadataVersion<?> lastVersion = versions.getLast();
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
        Document xml = MCRXMLMetadataManager.getInstance().retrieveXML(objectID);
        Element mcrObjectXML = xml.getRootElement();
        XPathExpression<Element> expression = XPathFactory.instance().compile(xpath, Filters.element());
        List<Element> derivateLinkElements = expression.evaluate(mcrObjectXML);

        // check them
        boolean changedObject = false;
        for (Element derivateLinkElement : derivateLinkElements) {
            String href = derivateLinkElement.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
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
                    if (tryRawPath(objectID, derivateLinkElement, link, owner)) {
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
                if (tryRawPath(objectID, derivateLinkElement, link, owner)) {
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
            MCRXMLMetadataManager.getInstance().update(objectID, xml, new Date());
            // manually fire update event
            MCRObject newObject = MCRMetadataManager.retrieveMCRObject(objectID);
            newObject.setImportMode(true);
            MCRMetadataManager.fireUpdateEvent(newObject);
        }
    }

    private static boolean tryRawPath(MCRObjectID objectID, Element derivateLinkElement,
        MCRMetaDerivateLink link, String owner) {
        String rawPath = link.getRawPath();
        MCRPath mcrPath = MCRPath.getPath(owner, rawPath);
        if (Files.exists(mcrPath)) {
            // path exists -> do URI encoding for href
            try {
                String encodedHref = MCRXMLFunctions.encodeURIPath(rawPath);
                derivateLinkElement.setAttribute(MCRXlink.HREF, owner + encodedHref, MCRConstants.XLINK_NAMESPACE);
                return true;
            } catch (URISyntaxException uriEncodeException) {
                LOGGER.error("Unable to encode {} for object {}", rawPath, objectID, uriEncodeException);
                return false;
            }
        }
        return false;
    }


    // 2017-> 2018
    @MCRCommand(syntax = "migrate tei entries in mets file of derivate {0}")
    public static void migrateTEIEntrysOfMetsFileOfDerivate(String derivateIdStr)
        throws IOException, JDOMException {
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIdStr);
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.info(() -> "Derivate " + derivateIdStr + " does not exist!");
            return;
        }

        final MCRPath metsPath = MCRPath.getPath(derivateIdStr, "mets.xml");
        if (!Files.exists(metsPath)) {
            LOGGER.info(() -> "Derivate " + derivateIdStr + " has no mets.xml!");
            return;
        }

        final MCRXSLTransformer transformer = MCRXSLTransformer.obtainInstance("xsl/mets-translation-migration.xsl");

        final Document xml;

        try (InputStream is = Files.newInputStream(metsPath)) {
            final MCRContent content = transformer.transform(new MCRStreamContent(is));
            xml = content.asXML();
        }

        try (OutputStream os = Files.newOutputStream(metsPath, StandardOpenOption.TRUNCATE_EXISTING)) {
            final XMLOutputter writer = new XMLOutputter(Format.getPrettyFormat());
            writer.output(xml, os);

        }

        LOGGER.info(() -> "Migrated mets of " + derivateIdStr);
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
        String titleFlag = "title";
        if (!derivate.getService().getFlags(titleFlag).isEmpty()) {
            String title = derivate.getService().getFlags(titleFlag).getFirst();
            derivate.getDerivate().getTitles().add(new MCRMetaLangText("title", "de", null, 0, "main", title));
            derivate.getService().removeFlags(titleFlag);
        }

        //update derivate
        MCRMetadataManager.update(derivate);
    }
}
