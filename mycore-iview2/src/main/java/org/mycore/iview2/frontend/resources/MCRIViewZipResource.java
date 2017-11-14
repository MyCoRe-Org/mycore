package org.mycore.iview2.frontend.resources;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

@Path("/iview/zip")
public class MCRIViewZipResource {

    /**
     * Zips a derivate and its containing iview images as jpg's. All other files are ignored.
     * 
     * @param derivateID the derivate to zip
     * @param zoom if undefined the base resolution is assumed
     * @return zip file
     */
    @GET
    @Produces("application/zip")
    @Path("{derivateID}")
    public Response zip(@PathParam("derivateID") String derivateID, @QueryParam("zoom") Integer zoom) throws Exception {
        if (!MCRAccessManager.checkPermissionForReadingDerivate(derivateID)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        MCRPath derivateRoot = MCRPath.getPath(derivateID, "/");
        if (!Files.exists(derivateRoot)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        ZipStreamingOutput stream = new ZipStreamingOutput(derivateRoot, zoom);
        return Response.ok(stream).header("Content-Disposition", "attachnment; filename=\"" + derivateID + ".zip\"")
            .build();
    }

    public static class ZipStreamingOutput implements StreamingOutput {

        protected MCRPath derivateRoot;

        protected Integer zoom;

        public ZipStreamingOutput(MCRPath derivateRoot, Integer zoom) {
            this.derivateRoot = derivateRoot;
            this.zoom = zoom;
        }

        @Override
        public void write(OutputStream out) throws IOException, WebApplicationException {
            MCRSessionMgr.getCurrentSession().beginTransaction();
            try {
                final ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(new BufferedOutputStream(out));
                zipStream.setLevel(Deflater.BEST_SPEED);
                SimpleFileVisitor<java.nio.file.Path> zipper = new SimpleFileVisitor<java.nio.file.Path>() {
                    @Override
                    public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
                        throws IOException {
                        Objects.requireNonNull(file);
                        Objects.requireNonNull(attrs);
                        MCRPath mcrPath = MCRPath.toMCRPath(file);
                        if (MCRIView2Tools.isFileSupported(file)) {
                            java.nio.file.Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(),
                                mcrPath.getOwner(), mcrPath.getOwnerRelativePath());
                            if (!Files.exists(iviewFile)) {
                                return super.visitFile(iviewFile, attrs);
                            }
                            try {
                                MCRTiledPictureProps imageProps = MCRTiledPictureProps.getInstanceFromFile(iviewFile);
                                Integer zoomLevel = (zoom == null || zoom > imageProps.getZoomlevel()) ? imageProps
                                    .getZoomlevel() : zoom;
                                BufferedImage image = MCRIView2Tools.getZoomLevel(iviewFile, zoomLevel);
                                ZipArchiveEntry entry = new ZipArchiveEntry(file.getFileName() + ".jpg");
                                zipStream.putArchiveEntry(entry);
                                ImageIO.write(image, "jpg", zipStream);
                            } catch (JDOMException e) {
                                throw new WebApplicationException(e);
                            }
                            zipStream.closeArchiveEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(derivateRoot, zipper);
                zipStream.close();
            } catch (Exception exc) {
                throw new WebApplicationException(exc);
            } finally {
                MCRSessionMgr.getCurrentSession().commitTransaction();
            }
        }
    }

}
