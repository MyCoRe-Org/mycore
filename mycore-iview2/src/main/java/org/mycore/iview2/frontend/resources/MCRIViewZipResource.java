package org.mycore.iview2.frontend.resources;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
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
        MCRFilesystemNode root = MCRFilesystemNode.getRootNode(derivateID);
        if (root == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        ZipStreamingOutput stream = new ZipStreamingOutput(derivateID, zoom, root);
        return Response.ok(stream).header("Content-Disposition", "attachnment; filename=\"" + derivateID + ".zip\"")
            .build();
    }

    public static class ZipStreamingOutput implements StreamingOutput {

        protected String derivateID;

        protected Integer zoom;

        protected MCRFilesystemNode root;

        public ZipStreamingOutput(String derivateID, Integer zoom, MCRFilesystemNode root) {
            this.derivateID = derivateID;
            this.zoom = zoom;
            this.root = root;
        }

        @Override
        public void write(OutputStream out) throws IOException, WebApplicationException {
            MCRSessionMgr.getCurrentSession().beginTransaction();
            try {
                ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(new BufferedOutputStream(out));
                zipStream.setLevel(Deflater.BEST_SPEED);
                for (MCRFile file : root.getFiles()) {
                    File iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivateID, file.getName());
                    if (!iviewFile.exists()) {
                        continue;
                    }
                    MCRTiledPictureProps imageProps = MCRTiledPictureProps.getInstance(iviewFile);
                    Integer zoomLevel = (zoom == null || zoom > imageProps.getZoomlevel()) ? imageProps.getZoomlevel()
                        : zoom;
                    BufferedImage image = MCRIView2Tools.getZoomLevel(iviewFile, zoomLevel);
                    ZipArchiveEntry entry = new ZipArchiveEntry(file.getName() + ".jpg");
                    zipStream.putArchiveEntry(entry);
                    ImageIO.write(image, "jpg", zipStream);
                    zipStream.closeArchiveEntry();
                }
                zipStream.close();
            } catch (Exception exc) {
                throw new WebApplicationException(exc);
            } finally {
                MCRSessionMgr.getCurrentSession().commitTransaction();
            }
        }
    }

}
