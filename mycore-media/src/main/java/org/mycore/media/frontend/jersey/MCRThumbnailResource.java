package org.mycore.media.frontend.jersey;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.media.services.MCRThumbnailGenerator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;

import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Path("thumbnail")
public class MCRThumbnailResource {
    @Context
    private Request request;

    /**
     * This method returns a thumbnail for a given document with a given size in pixel for the shortest side.
     * @param documentId the documentID you want the thumbnail from
     * @param size the size of the shortest side in pixel
     * @return the thumbnail as png, jpg or error 404 if if there is no derivate or no generator for filetype
     */
    @GET
    @Path("{documentId}/{size}.{ext}")
    @Produces({"image/png","image/jpeg"})
    public Response getThumbnailFromDocument(@PathParam("documentId") String documentId, @PathParam("size") int size,
            @PathParam("ext") String ext) {
        return getThumbnail(documentId, size, ext);
    }

    /**
     * This method returns a thumbnail for a given document with a default size in pixel for the shortest side.
     * @param documentId the documentID you want the thumbnail from
     * @return the thumbnail as png, jpg or error 404 if if there is no derivate or no generator for filetype
     */
    @GET
    @Path("{documentId}.{ext}")
    @Produces({"image/png", "image/jpeg"})
    public Response getThumbnailFromDocument(@PathParam("documentId") String documentId, @PathParam("ext") String ext) {
        int defaultSize = MCRConfiguration.instance().getInt("MCR.Media.Thumbnail.DefaultSize");
        return getThumbnail(documentId, defaultSize, ext);
    }


    private Response getThumbnail(String documentId, int size, String ext) {
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(MCRJerseyUtil.getID(documentId),
            1,TimeUnit.MINUTES);
        for (MCRObjectID derivateId : derivateIds) {
            if (MCRAccessManager.checkPermissionForReadingDerivate(derivateId.toString())){
                String nameOfMainFile = MCRMetadataManager.retrieveMCRDerivate(derivateId).getDerivate().getInternals()
                    .getMainDoc();
                if (nameOfMainFile != null && !nameOfMainFile.equals("")) {
                    MCRPath mainFile = MCRPath.getPath(derivateId.toString(), '/' + nameOfMainFile);
                    try {
                        String mimeType = Files.probeContentType(mainFile);
                        String generators = MCRConfiguration.instance()
                            .getString("MCR.Media.Thumbnail.Generators");
                        for (String generator : generators.split(",")) {
                            Class<MCRThumbnailGenerator> classObject = (Class<MCRThumbnailGenerator>) Class
                                .forName(generator);
                            Constructor<MCRThumbnailGenerator> constructor = classObject.getConstructor();
                            MCRThumbnailGenerator thumbnailGenerator = constructor.newInstance();
                            if (thumbnailGenerator.matchesFileType(mimeType, mainFile)) {
                                FileTime lastModified = Files.getLastModifiedTime(mainFile);
                                Date lastModifiedDate = new Date(lastModified.toMillis());
                                Response.ResponseBuilder resp = request.evaluatePreconditions(lastModifiedDate);
                                if (resp == null) {
                                    Optional<BufferedImage> thumbnail =
                                        thumbnailGenerator.getThumbnail(mainFile, size);
                                    if (thumbnail.isPresent()) {
                                        CacheControl cc = new CacheControl();
                                        cc.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));
                                        String type = "image/png";
                                        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
                                            type = "image/jpeg";
                                        }
                                        return Response.ok(thumbnail.get())
                                            .cacheControl(cc)
                                            .lastModified(lastModifiedDate)
                                            .type(type)
                                            .build();
                                    }
                                    return Response.status(Response.Status.NOT_FOUND).build();
                                }
                                return resp.build();
                            }
                        }
                    } catch (IOException | ReflectiveOperationException e) {
                        throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
