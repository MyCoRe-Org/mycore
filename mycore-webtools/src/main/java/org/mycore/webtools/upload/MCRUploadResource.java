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
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.fileupload.MCRPostUploadFileProcessor;
import org.mycore.webtools.upload.exception.MCRInvalidFileException;
import org.mycore.webtools.upload.exception.MCRInvalidUploadParameterException;
import org.mycore.webtools.upload.exception.MCRMissingParameterException;
import org.mycore.webtools.upload.exception.MCRUploadException;
import org.mycore.webtools.upload.exception.MCRUploadForbiddenException;
import org.mycore.webtools.upload.exception.MCRUploadServerException;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("files/upload/")
public class MCRUploadResource {

    private static final String FILE_PROCESSOR_PROPERTY = "MCR.MCRUploadHandlerIFS.FileProcessors";

    private static final List<MCRPostUploadFileProcessor> FILE_PROCESSORS = initProcessorList();

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    ContainerRequestContext request;

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    @Path("begin")
    public Response begin(@QueryParam("uploadHandler") String uploadHandlerID) throws MCRUploadServerException {
        MultivaluedMap<String, String> parameters = request.getUriInfo().getQueryParameters();

        MCRUploadHandler uploadHandler = getUploadHandler(uploadHandlerID);
        String bucketID;
        try {
            bucketID = uploadHandler.begin(parameters);
            MCRFileUploadBucket.createBucket(bucketID, parameters, uploadHandler);
        } catch (MCRUploadForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (MCRInvalidUploadParameterException | MCRMissingParameterException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRUploadServerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok().entity(bucketID).build();
    }

    @PUT
    @Path("{buckedID}/commit")
    public Response commit(@PathParam("buckedID") String buckedID) {
        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getBucket(buckedID);
        if (bucket == null) {
            throw new BadRequestException("buckedID " + buckedID + " is invalid!");
        }

        URI location;

        try {
            location = bucket.getUploadHandler().commit(bucket);
        } catch (MCRInvalidFileException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRUploadException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            try {
                MCRFileUploadBucket.releaseBucket(bucket.getBucketID());
            } catch (MCRUploadServerException e) {
                LOGGER.error(() -> "Error while releasing bucket " + bucket.getBucketID(), e);
            }
        }
        if (location == null) {
            return Response.ok().build();
        }
        return Response.created(location).build();
    }

    public static MCRUploadHandler getUploadHandler(String uploadHandlerID) {
        Optional<MCRUploadHandler> uploadHandler = MCRConfiguration2.getSingleInstanceOf(MCRUploadHandler.class,
            "MCR.Upload.Handler." + Optional.ofNullable(uploadHandlerID).orElse("Default"));
        if (uploadHandler.isEmpty()) {
            throw new BadRequestException("The UploadHandler " + uploadHandlerID + " is invalid!");
        }
        return uploadHandler.get();
    }

    @GET
    @Path("{uploadHandler}/{path:.+}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response validateFile(
        @PathParam("uploadHandler") String uploadHandlerID,
        @PathParam("path") String path,
        @QueryParam("size") String size) {
        String fileName = Paths.get(path).getFileName().toString();
        String unicodeNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);

        try {
            getUploadHandler(uploadHandlerID).validateFileMetadata(unicodeNormalizedFileName, Long.parseLong(size));
        } catch (MCRUploadForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (MCRInvalidFileException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRUploadServerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok().build();
    }

    @PUT
    @Path("{buckedID}/{path:.+}")
    public Response uploadFile(@PathParam("path") String path,
        @PathParam("buckedID") String buckedID,
        @QueryParam(("isDirectory")) boolean isDirectory,
        InputStream contents)
        throws IOException {

        final MCRFileUploadBucket bucket = MCRFileUploadBucket.getBucket(buckedID);
        if (bucket == null) {
            throw new BadRequestException("buckedID " + buckedID + " is invalid!");
        }

        String unicodeNormalizedPath = Normalizer.normalize(path, Normalizer.Form.NFC);
        final java.nio.file.Path filePath = MCRUtils.safeResolve(bucket.getRoot(), unicodeNormalizedPath);
        createBucketRootIfNotExist(filePath);

        String actualStringFileName = bucket.getRoot().relativize(filePath).getFileName().toString();
        MCRUploadHandler uploadHandler = bucket.getUploadHandler();

        String contentLengthStr = request.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        long contentLength = contentLengthStr == null ? 0 : Long.parseLong(contentLengthStr);
        try {
            uploadHandler.validateFileMetadata(actualStringFileName, contentLength);
        } catch (MCRUploadForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (MCRInvalidFileException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (MCRUploadServerException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        if (isDirectory) {
            Files.createDirectory(filePath);
        } else {
            final List<MCRPostUploadFileProcessor> processors = FILE_PROCESSORS.stream()
                .filter(processor -> processor.isProcessable(unicodeNormalizedPath))
                .toList();

            if (processors.isEmpty()) {
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
        return Response.noContent().build();
    }

    private static void createBucketRootIfNotExist(java.nio.file.Path filePath) throws IOException {
        if (filePath.getNameCount() > 1) {
            java.nio.file.Path parentDirectory = filePath.getParent();
            if (!Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
            }
        }
    }

}
