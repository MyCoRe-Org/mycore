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

package org.mycore.webtools.upload;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRFileCollectingFileVisitor;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.mycore.frontend.fileupload.MCRPostUploadFileProcessor;

@Path("files/upload/")
public class MCRUploadResource {

    private static final String FILE_PROCESSOR_PROPERTY = "MCR.MCRUploadHandlerIFS.FileProcessors";

    private static final List<MCRPostUploadFileProcessor> FILE_PROCESSORS = initProcessorList();

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    ContainerRequestContext request;

    private static List<MCRPostUploadFileProcessor> initProcessorList() {
        List<String> fileProcessorList = MCRConfiguration.instance().getStrings(FILE_PROCESSOR_PROPERTY,
            Collections.emptyList());
        return fileProcessorList.stream().map(fpClassName -> {
            try {
                @SuppressWarnings("unchecked")
                Class<MCRPostUploadFileProcessor> aClass = (Class<MCRPostUploadFileProcessor>) Class
                    .forName(fpClassName);
                Constructor<MCRPostUploadFileProcessor> constructor = aClass.getConstructor();

                return constructor.newInstance();
            } catch (ClassNotFoundException e) {
                throw new MCRConfigurationException(
                    "The class " + fpClassName + " defined in " + FILE_PROCESSOR_PROPERTY + " was not found!", e);
            } catch (NoSuchMethodException e) {
                throw new MCRConfigurationException(
                    "The class " + fpClassName + " defined in " + FILE_PROCESSOR_PROPERTY
                        + " has no default constructor!",
                    e);
            } catch (IllegalAccessException e) {
                throw new MCRConfigurationException(
                    "The class " + fpClassName + " defined in " + FILE_PROCESSOR_PROPERTY
                        + " has a private/protected constructor!",
                    e);
            } catch (InstantiationException e) {
                throw new MCRConfigurationException(
                    "The class " + fpClassName + " defined in " + FILE_PROCESSOR_PROPERTY + " is abstract!", e);
            } catch (InvocationTargetException e) {
                throw new MCRConfigurationException(
                    "The constrcutor of class " + fpClassName + " defined in " + FILE_PROCESSOR_PROPERTY
                        + " threw a exception on invoke!",
                    e);
            }
        }).collect(Collectors.toList());
    }

    private static void setDefaultMainFile(MCRDerivate derivate) {
        MCRPath path = MCRPath.getPath(derivate.getId().toString(), "/");
        try {
            MCRFileCollectingFileVisitor<java.nio.file.Path> visitor = new MCRFileCollectingFileVisitor<>();
            Files.walkFileTree(path, visitor);

            visitor.getPaths().stream()
                .map(MCRPath.class::cast)
                .filter(p -> !p.getOwnerRelativePath().endsWith(".xml"))
                .findFirst()
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

    @PUT
    @Path("commit")
    public void commit(@QueryParam("uploadID") String uploadID) {
        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getBucket(uploadID);
        if (bucket == null) {
            throw new BadRequestException("uploadID " + uploadID + " is invalid!");
        }

        final java.nio.file.Path root = bucket.getRoot();

        MCRObjectID objOrDerivateID = MCRObjectID.getInstance(bucket.getObjectID());
        final boolean isDerivate = objOrDerivateID.getTypeId().equals("derivate");

        final MCRPath targetDerivateRoot;

        if (isDerivate) {
            targetDerivateRoot = MCRPath.getPath(objOrDerivateID.toString(), "/");
        } else {
            try {
                objOrDerivateID = createDerivate(objOrDerivateID).getId();
                targetDerivateRoot = MCRPath.getPath(objOrDerivateID.toString(), "/");
            } catch (MCRAccessException e) {
                throw new MCRUploadException("mcr.upload.create.derivate.failed", e);
            }
        }

        final MCRTreeCopier copier;
        try {
            copier = new MCRTreeCopier(root, targetDerivateRoot);
        } catch (NoSuchFileException e) {
            throw new MCRException(e);
        }

        try {
            Files.walkFileTree(root, copier);
        } catch (IOException e) {
            throw new MCRUploadException("mcr.upload.import.failed", e);
        }

        MCRDerivate theDerivate = MCRMetadataManager.retrieveMCRDerivate(objOrDerivateID);

        String mainDoc = theDerivate.getDerivate().getInternals().getMainDoc();
        if (mainDoc == null || mainDoc.isEmpty()) {
            setDefaultMainFile(theDerivate);
        }
    }

    private MCRObjectID getNewCreateDerivateID(MCRObjectID objId) {
        String projectID = objId.getProjectId();
        return MCRObjectID.getNextFreeId(projectID + "_derivate");

    }

    private MCRDerivate createDerivate(MCRObjectID objectID) throws MCRPersistenceException, MCRAccessException {

        MCRObjectID derivateID = getNewCreateDerivateID(objectID);
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.setLabel("data object from " + objectID.toString());

        String schema = MCRConfiguration.instance().getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml")
            .replaceAll(".xml",
                ".xsd");
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

    private void setDefaultPermissions(MCRObjectID derivateID) {
        if (MCRConfiguration.instance().getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }
    }

    @PUT
    @Path("{objectID}/{path:.+}")
    public void uploadFile(@PathParam("path") String path,
        @PathParam("objectID") String objectID,
        @QueryParam("uploadID") String uploadID,
        InputStream contents)
        throws IOException {

        MCRObjectID oid = MCRObjectID.getInstance(objectID);
        if (!MCRMetadataManager.exists(oid) || !MCRAccessManager
            .checkPermission(oid, MCRAccessManager.PERMISSION_WRITE)) {
            throw new ForbiddenException("No write access to " + oid.toString());
        }

        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getOrCreateBucket(uploadID, objectID);

        final java.nio.file.Path filePath = bucket.getRoot().resolve(path);
        if (filePath.getNameCount() > 1) {
            java.nio.file.Path parentDirectory = filePath.getParent();
            if (!Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
            }
        }

        long maxSize = MCRConfiguration2.getOrThrow("MCR.FileUpload.MaxSize", Long::parseLong);
        String contentLength = request.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        if (contentLength == null || Long.parseLong(contentLength) == 0) {
            Files.createDirectory(filePath);
        } else if (Long.parseLong(contentLength) > maxSize) {
            throw new BadRequestException("File is to big. " + path);
        } else {
            final List<MCRPostUploadFileProcessor> processors = FILE_PROCESSORS.stream()
                .filter(procesor -> {
                    return procesor.isProcessable(path);
                }).collect(Collectors.toList());

            if (processors.size() == 0) {
                Files.copy(contents, filePath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                java.nio.file.Path input = Files.createTempFile("processing", ".temp");
                Files.copy(contents, input, StandardCopyOption.REPLACE_EXISTING);

                for (MCRPostUploadFileProcessor processor : processors) {
                    final java.nio.file.Path tempFile2 = Files.createTempFile("processing", ".temp");
                    final java.nio.file.Path result = processor.processFile(path, input, () -> tempFile2);
                    if (result != null) {
                        Files.deleteIfExists(input);
                        input = result;
                    }
                }
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(input);
            }
        }
    }

}
