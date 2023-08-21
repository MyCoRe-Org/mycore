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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.fileupload.MCRPostUploadFileProcessor;
import org.mycore.frontend.fileupload.MCRUploadHelper;
import org.mycore.services.i18n.MCRTranslation;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("files/upload/")
public class MCRUploadResource {

    private static final String FILE_PROCESSOR_PROPERTY = "MCR.MCRUploadHandlerIFS.FileProcessors";

    private static final List<MCRPostUploadFileProcessor> FILE_PROCESSORS = initProcessorList();

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    ContainerRequestContext request;

    private static List<MCRPostUploadFileProcessor> initProcessorList() {
        List<String> fileProcessorList = MCRConfiguration2.getString(FILE_PROCESSOR_PROPERTY)
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
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

    @PUT
    @Path("commit")
    public Response commit(@QueryParam("uploadID") String uploadID,
        @QueryParam("uploadHandler") String uploadHandlerID,
        @QueryParam("classifications") String classificationValues) {
        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getBucket(uploadID);
        if (bucket == null) {
            throw new BadRequestException("uploadID " + uploadID + " is invalid!");
        }

        final List<MCRMetaClassification> classifications = getClassifications(classificationValues);
        MCRObjectID objOrDerivateID = MCRObjectID.getInstance(bucket.getObjectID());

        MCRUploadHandler uploadHandler = getUploadHandler(uploadHandlerID);
        URI location;

        try {
            location = uploadHandler.commit(objOrDerivateID, bucket, classifications);
        } finally {
            MCRFileUploadBucket.releaseBucket(bucket.getBucketID());
        }
        if (location == null) {
            return Response.ok().build();
        }
        return Response.created(location).build();
    }

    public static MCRUploadHandler getUploadHandler(String uploadHandlerID) {
        Optional<MCRUploadHandler> uploadHandler = MCRConfiguration2
            .getInstanceOf("MCR.Upload.Handler." + Optional.ofNullable(uploadHandlerID).orElse("Default"));

        if (uploadHandler.isEmpty()) {
            throw new BadRequestException("uploadHandler " + uploadHandlerID + " is invalid!");
        }
        return uploadHandler.get();
    }

    private List<MCRMetaClassification> getClassifications(String classifications) {
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        final List<MCRCategoryID> categoryIDS = Stream.of(classifications)
            .filter(Objects::nonNull)
            .filter(Predicate.not(String::isBlank))
            .flatMap(c -> Stream.of(c.split(",")))
            .filter(Predicate.not(String::isBlank))
            .filter(cv -> cv.contains(":"))
            .map(classValString -> {
                final String[] split = classValString.split(":");
                return new MCRCategoryID(split[0], split[1]);
            }).collect(Collectors.toList());

        if (!categoryIDS.stream().allMatch(dao::exist)) {
            final String parsedIDS = categoryIDS.stream().map(Object::toString).collect(Collectors.joining(","));
            throw new MCRException(String.format(Locale.ROOT, "One of the Categories \"%s\" was not found", parsedIDS));
        }

        return categoryIDS.stream()
            .map(category -> new MCRMetaClassification("classification", 0, null, category.getRootID(),
                category.getID()))
            .collect(Collectors.toList());
    }

    @GET
    @Path("{objectID}/{path:.+}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response validateFile(@PathParam("path") String path,
        @PathParam("objectID") String objectID,
        @QueryParam("size") String size) {
        String fileName = Paths.get(path).getFileName().toString();
        String unicodeNormalizedFileName =  Normalizer.normalize(fileName, Normalizer.Form.NFC);
        String translation = MCRTranslation.translate("IFS.invalid.fileName", unicodeNormalizedFileName);
        try {
            MCRUploadHelper.checkPathName(unicodeNormalizedFileName);
        } catch (MCRException e) {
            LOGGER.warn("Invalid file name: {} -> {}", unicodeNormalizedFileName, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), translation)
                    .entity(translation).build();
        }

        long sizeL = Long.parseLong(size);
        long maxSize = MCRConfiguration2.getOrThrow("MCR.FileUpload.MaxSize", Long::parseLong);

        if (sizeL > maxSize) {
            translation = MCRTranslation.translate("component.webtools.upload.invalid.fileSize",
                unicodeNormalizedFileName, MCRUtils.getSizeFormatted(sizeL), MCRUtils.getSizeFormatted(maxSize));
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                translation).entity(translation).build();
        }

        return Response.ok().build();
    }

    @PUT
    @Path("{objectID}/{path:.+}")
    public void uploadFile(@PathParam("path") String path,
        @PathParam("objectID") String objectID,
        @QueryParam("uploadID") String uploadID,
        @QueryParam("uploadHandler") String uploadHandlerID,
        InputStream contents)
        throws IOException {

        MCRObjectID oid = MCRObjectID.getInstance(objectID);
        MCRUploadHandler uploadHandler = getUploadHandler(uploadHandlerID);
        uploadHandler.validateObject(oid);

        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getOrCreateBucket(uploadID, objectID);

        String unicodeNormalizedPath = Normalizer.normalize(path, Normalizer.Form.NFC);
        final java.nio.file.Path filePath = MCRUtils.safeResolve(bucket.getRoot(), unicodeNormalizedPath);
        if (filePath.getNameCount() > 1) {
            java.nio.file.Path parentDirectory = filePath.getParent();
            if (!Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
            }
        }

        String actualStringFileName = bucket.getRoot().relativize(filePath).getFileName().toString();
        try {
            MCRUploadHelper.checkPathName(actualStringFileName);
        } catch (MCRException e) {
            throw new ClientErrorException(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
        }

        long maxSize = MCRConfiguration2.getOrThrow("MCR.FileUpload.MaxSize", Long::parseLong);
        String contentLength = request.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        if (contentLength == null || Long.parseLong(contentLength) == 0) {
            Files.createDirectory(filePath);
        } else if (Long.parseLong(contentLength) > maxSize) {
            throw new BadRequestException("File is to big. " + unicodeNormalizedPath);
        } else {
            final List<MCRPostUploadFileProcessor> processors = FILE_PROCESSORS.stream()
                .filter(processor -> processor.isProcessable(unicodeNormalizedPath))
                .collect(Collectors.toList());

            if (processors.size() == 0) {
                Files.copy(contents, filePath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                java.nio.file.Path input = Files.createTempFile("processing", ".temp");
                Files.copy(contents, input, StandardCopyOption.REPLACE_EXISTING);

                for (MCRPostUploadFileProcessor processor : processors) {
                    final java.nio.file.Path tempFile2 = Files.createTempFile("processing", ".temp");
                    final java.nio.file.Path result = processor.processFile(unicodeNormalizedPath, input,
                            () -> tempFile2);
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
