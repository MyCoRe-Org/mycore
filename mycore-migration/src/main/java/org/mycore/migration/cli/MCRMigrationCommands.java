package org.mycore.migration.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.links.MCRLINKHREF;
import org.mycore.backend.jpa.links.MCRLINKHREFPK_;
import org.mycore.backend.jpa.links.MCRLINKHREF_;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLink;
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
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;
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
            .peek(lid -> LOGGER.info("Adding " + lid + " to " + parentId)).forEach(parentStructure::addChild);
        if (parentStructure.getChildren().size() != sizeBefore) {
            MCRMetadataManager.fireUpdateEvent(parent);
        }
    }

    private static MCRMetaLinkID toLinkId(MCRObjectID mcrObjectID) {
        String objectLabel = MCRMetadataManager.retrieve(mcrObjectID).getLabel();
        return new MCRMetaLinkID("child", mcrObjectID, null, objectLabel);
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

    @MCRCommand(help = "migrate urn with serveID {service ID}", syntax = "migrate urn with serveID {0}")
    @Deprecated
    public static void migrateURN(String serviceID) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();

        entityManager.createQuery("select u from MCRURN u", org.mycore.urn.hibernate.MCRURN.class)
            .getResultList()
            .stream()
            .flatMap(mcrurn -> toMCRPI(mcrurn, serviceID))
            .peek(MCRMigrationCommands::logInfo)
            .forEach(entityManager::persist);

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.commit();
    }

    @Deprecated
    private static Stream<MCRPI> toMCRPI(org.mycore.urn.hibernate.MCRURN mcrurn, String serviceID) {
        String derivID = mcrurn.getId();
        String additional = Optional
            .ofNullable(mcrurn.getPath())
            .flatMap(path -> Optional.ofNullable(mcrurn.getFilename())
                .map(filename -> Paths.get(path, filename)))
            .map(Path::toString)
            .orElse("");

        MCRPI mcrpi = new MCRPI(mcrurn.getURN(), MCRDNBURN.TYPE, derivID, additional, serviceID, null);
        String suffix = "-dfg";

        return Optional.of(mcrurn)
            .filter(u -> u.isDfg())
            .flatMap(MCRMigrationCommands::parse)
            .map(dnbURN -> dnbURN.withSuffix(suffix))
            .map(MCRDNBURN::asString)
            .map(dfgURN -> new MCRPI(dfgURN, MCRDNBURN.TYPE + suffix, derivID, additional,
                serviceID + suffix, null))
            .map(dfgMcrPi -> Stream.of(mcrpi, dfgMcrPi))
            .orElse(Stream.of(mcrpi));
    }

    @Deprecated
    private static Optional<MCRDNBURN> parse(org.mycore.urn.hibernate.MCRURN urn) {
        return new MCRDNBURNParser().parse(urn.getURN());
    }

    private static void logInfo(MCRPI urn) {
        String urnStr = urn.getIdentifier();
        String mycoreID = urn.getMycoreID();
        String path = urn.getAdditional();
        LOGGER.info("Migrating: {} - {}:{}", urnStr, mycoreID, path);
    }
}
