package org.mycore.webtools.upload;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRFileCollectingFileVisitor;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;

public class MCRDefaultUploadHandler implements MCRUploadHandler {

    private static final String IGNORE_MAINFILE_PROPERTY = "MCR.Upload.NotPreferredFiletypeForMainfile";

    private static final Logger LOGGER = LogManager.getLogger();

    public static void setDefaultMainFile(MCRDerivate derivate) {
        MCRPath path = MCRPath.getPath(derivate.getId().toString(), "/");
        List<String> ignoreMainfileList = MCRConfiguration2.getString(IGNORE_MAINFILE_PROPERTY)
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
        try {
            MCRFileCollectingFileVisitor<Path> visitor = new MCRFileCollectingFileVisitor<>();
            Files.walkFileTree(path, visitor);

            //sort files by name
            ArrayList<Path> paths = visitor.getPaths();
            paths.sort(Comparator.comparing(Path::getNameCount)
                .thenComparing(Path::getFileName));
            //extract first file, before filtering
            MCRPath firstPath = MCRPath.toMCRPath(paths.get(0));

            //filter files, remove files that should be ignored for mainfile
            paths.stream()
                .map(MCRPath.class::cast)
                .filter(p -> ignoreMainfileList.stream().noneMatch(p.getOwnerRelativePath()::endsWith))
                .findFirst()
                .or(() -> Optional.of(firstPath))
                .ifPresent(file -> {
                    derivate.getDerivate().getInternals().setMainDoc(file.getOwnerRelativePath());
                    try {
                        MCRMetadataManager.update(derivate);
                    } catch (MCRPersistenceException | MCRAccessException e) {
                        LOGGER.error("Could not set main file!", e);
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Could not set main file!", e);
        }
    }

    @Override
    public URI commit(MCRObjectID objOrDerivateID,
        MCRFileUploadBucket bucket,
        List<MCRMetaClassification> classifications) {

        final Path root = bucket.getRoot();
        final boolean isDerivate = "derivate".equals(objOrDerivateID.getTypeId());

        final MCRPath targetDerivateRoot;

        MCRObjectID derivateID = objOrDerivateID;
        if (isDerivate) {
            targetDerivateRoot = MCRPath.getPath(objOrDerivateID.toString(), "/");
        } else {
            try {
                derivateID = createDerivate(objOrDerivateID, classifications).getId();
                targetDerivateRoot = MCRPath.getPath(objOrDerivateID.toString(), "/");
            } catch (MCRAccessException e) {
                throw new MCRUploadException("mcr.upload.create.derivate.failed", e);
            }
        }

        final MCRTreeCopier copier;
        try {
            copier = new MCRTreeCopier(root, targetDerivateRoot, false, true);
        } catch (NoSuchFileException e) {
            throw new MCRException(e);
        }

        try {
            Files.walkFileTree(root, copier);
        } catch (IOException e) {
            throw new MCRUploadException("mcr.upload.import.failed", e);
        }

        MCRDerivate theDerivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);

        String mainDoc = theDerivate.getDerivate().getInternals().getMainDoc();
        if (mainDoc == null || mainDoc.isEmpty()) {
            setDefaultMainFile(theDerivate);
        }

        return null; // We don´t want to redirect to the derivate, so we return null
    }

    @Override
    public void validateObject(MCRObjectID oid) throws ForbiddenException {
        if (!MCRMetadataManager.exists(oid) || !MCRAccessManager
            .checkPermission(oid, MCRAccessManager.PERMISSION_WRITE)) {
            throw new ForbiddenException("No write access to " + oid);
        }
    }

    public static MCRObjectID getNewCreateDerivateID(MCRObjectID objId) {
        String projectID = objId.getProjectId();
        return MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(projectID + "_derivate");

    }

    public static MCRDerivate createDerivate(MCRObjectID objectID, List<MCRMetaClassification> classifications)
        throws MCRPersistenceException, MCRAccessException {

        MCRObjectID derivateID = getNewCreateDerivateID(objectID);
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.getDerivate().getClassifications().addAll(classifications);

        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(objectID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID {}", derivateID);
        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivateID);

        return derivate;
    }

    public static void setDefaultPermissions(MCRObjectID derivateID) {
        if (MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true)) {
            MCRRuleAccessInterface aclImpl = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = aclImpl.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }
    }
}
