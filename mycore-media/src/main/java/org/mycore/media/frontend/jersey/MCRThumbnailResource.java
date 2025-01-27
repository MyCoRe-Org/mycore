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

package org.mycore.media.frontend.jersey;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.media.services.MCRThumbnailGenerator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

@Path("thumbnail")
public class MCRThumbnailResource {
    @Context
    private Request request;

    /**
     * This method returns a thumbnail for a given document with a given size in pixel for the shortest side.
     * @param documentId the documentID you want the thumbnail from
     * @param size the size of the shortest side in pixel
     * @param ext  the extension of the new image file (jpg ord png)
     * @return the thumbnail as png, jpg or error 404 if if there is no derivate or no generator for filetype
     */
    @GET
    @Path("{documentId}/{size}.{ext}")
    @Produces({ "image/png", "image/jpeg" })
    public Response getThumbnailFromDocument(@PathParam("documentId") String documentId, @PathParam("size") int size,
        @PathParam("ext") String ext) {
        return getThumbnailResponse(documentId, size, ext);
    }

    /**
     * This method returns a thumbnail for a given document with a default size in pixel for the shortest side.
     * @param documentId the documentID you want the thumbnail from
     * @param ext  the extension of the new image file (jpg ord png)
     * @return the thumbnail as png, jpg or error 404 if if there is no derivate or no generator for filetype
     */
    @GET
    @Path("{documentId}.{ext}")
    @Produces({ "image/png", "image/jpeg" })
    public Response getThumbnailFromDocument(@PathParam("documentId") String documentId, @PathParam("ext") String ext) {
        int defaultSize = MCRConfiguration2.getOrThrow("MCR.Media.Thumbnail.DefaultSize", Integer::parseInt);
        return getThumbnailResponse(documentId, defaultSize, ext);
    }

    private Response getThumbnailResponse(String documentId, int size, String ext) {
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(MCRJerseyUtil.getID(documentId),
            1, TimeUnit.MINUTES);
        for (MCRObjectID derivateId : derivateIds) {
            if (!MCRAccessManager.checkDerivateDisplayPermission(derivateId.toString())) {
                continue;
            }
            String mainDoc = MCRMetadataManager.retrieveMCRDerivate(derivateId)
                .getDerivate().getInternals().getMainDoc();
            if (mainDoc == null || mainDoc.isEmpty()) {
                continue;
            }
            MCRPath mainFile = MCRPath.getPath(derivateId.toString(), '/' + mainDoc);
            try {
                FileTime lastModified = Files.getLastModifiedTime(mainFile);
                Date lastModifiedDate = new Date(lastModified.toMillis());
                Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModifiedDate);
                if (responseBuilder != null) {
                    return responseBuilder.build();
                }
                final Optional<BufferedImage> thumbnail = getThumbnail(mainFile, size);
                if (thumbnail.isEmpty()) {
                    continue;
                }
                return getThumbnailResponse(ext, thumbnail.get(), lastModifiedDate);
            } catch (IOException | RuntimeException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private static Response getThumbnailResponse(String ext, BufferedImage thumbnail, Date lastModifiedDate) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));
        String type = "image/png";
        if (Objects.equals(ext, "jpg") || Objects.equals(ext, "jpeg")) {
            type = "image/jpeg";
        }
        return Response.ok(thumbnail)
            .cacheControl(cc)
            .lastModified(lastModifiedDate)
            .type(type)
            .build();
    }

    private static Optional<BufferedImage> getThumbnail(MCRPath mainFile, int size) throws IOException {
        String mimeType = Files.probeContentType(mainFile);
        List<MCRThumbnailGenerator> generators = MCRConfiguration2
            .getOrThrow("MCR.Media.Thumbnail.Generators", MCRConfiguration2::splitValue)
            .map(MCRConfiguration2::<MCRThumbnailGenerator>instantiateClass)
            .filter(thumbnailGenerator -> thumbnailGenerator.matchesFileType(mimeType, mainFile))
            .toList();
        return generators.stream()
            .map(thumbnailGenerator -> {
                try {
                    return thumbnailGenerator.getThumbnail(mainFile, size);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

}
