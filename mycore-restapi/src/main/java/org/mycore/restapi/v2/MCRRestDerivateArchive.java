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

package org.mycore.restapi.v2;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.services.zipper.MCRArchiver;
import org.mycore.services.zipper.MCRTarArchiver;
import org.mycore.services.zipper.MCRZipArchiver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

/**
 * Delivers all files of a derivate as ZIP or TAR archive.
 * <p>
 * JSON and XML are listed as additional media types only to deliver error responses.
 */
@Path("/objects/{" + PARAM_MCRID + "}/derivates/{" + PARAM_DERID + "}/archive")
public class MCRRestDerivateArchive {

    private static final MCRZipArchiver ZIP_ARCHIVER = new MCRZipArchiver();

    private static final MCRTarArchiver TAR_ARCHIVER = new MCRTarArchiver();

    @Context
    ContainerRequestContext request;

    @Context
    UriInfo uriInfo;

    @Parameter(example = "mir_mods_00004711")
    @PathParam(PARAM_MCRID)
    MCRObjectID mcrId;

    @Parameter(example = "mir_derivate_00004711")
    @PathParam(PARAM_DERID)
    MCRObjectID derid;

    @GET
    @Path("/zip")
    @Produces({ "application/zip", MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRCacheControl(noCache = @MCRCacheControl.FieldArgument(active = true))
    @Operation(summary = "Delivers all files of the derivate as ZIP archive", tags = MCRRestUtils.TAG_MYCORE_FILE)
    public Response getZipArchive() {
        return createArchiveResponse(ZIP_ARCHIVER);
    }

    @GET
    @Path("/tar")
    @Produces({ "application/x-tar", MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRCacheControl(noCache = @MCRCacheControl.FieldArgument(active = true))
    @Operation(summary = "Delivers all files of the derivate as TAR archive", tags = MCRRestUtils.TAG_MYCORE_FILE)
    public Response getTarArchive() {
        return createArchiveResponse(TAR_ARCHIVER);
    }

    private <T extends ArchiveOutputStream<?>> Response createArchiveResponse(MCRArchiver<T> archiver) {
        MCRRestDerivates.validateDerivateRelation(mcrId, derid);
        MCRPath root = MCRPath.getPath(derid.toString(), "/");
        Date lastModified = getLastModified(root);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        String comment = "Created by " + uriInfo.getRequestUri() + " at " + lastModified.toInstant();
        StreamingOutput stream = out -> {
            try (T container = archiver.createContainer(out, comment)) {
                archiver.sendTree(root, container);
                container.finish();
            }
        };
        return Response.ok(stream, archiver.getMimeType())
            .lastModified(lastModified)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + derid + "." + archiver.getFileExtension() + "\"")
            .build();
    }

    /**
     * Returns the latest modification date of all files and directories of the derivate.
     */
    private Date getLastModified(MCRPath root) {
        long[] lastModified = { 0 };
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) {
                    lastModified[0] = Math.max(lastModified[0], attrs.lastModifiedTime().toMillis());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
                    lastModified[0] = Math.max(lastModified[0], attrs.lastModifiedTime().toMillis());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException e) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_NOT_FOUND)
                .withMessage("Could not find files of derivate " + derid + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        } catch (IOException e) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_IO_ERROR)
                .withMessage("Could not read files of derivate " + derid + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        return new Date(lastModified[0]);
    }

}
