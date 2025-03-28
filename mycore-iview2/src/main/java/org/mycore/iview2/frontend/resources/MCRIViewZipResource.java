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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

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
    public Response zip(@PathParam("derivateID") String derivateID, @QueryParam("zoom") Integer zoom) {
        if (!MCRAccessManager.checkDerivateContentPermission(MCRObjectID.getInstance(derivateID),
            MCRAccessManager.PERMISSION_READ)) {
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
        public void write(OutputStream out) throws WebApplicationException {
            MCRSessionMgr.getCurrentSession();
            MCRTransactionManager.beginTransactions();
            try {
                final ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(new BufferedOutputStream(out));
                zipStream.setLevel(Deflater.BEST_SPEED);
                SimpleFileVisitor<java.nio.file.Path> zipper = new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
                        throws IOException {
                        Objects.requireNonNull(file);
                        Objects.requireNonNull(attrs);
                        MCRPath mcrPath = MCRPath.ofPath(file);
                        if (MCRIView2Tools.isFileSupported(file)) {
                            java.nio.file.Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(),
                                mcrPath.getOwner(), mcrPath.getOwnerRelativePath());
                            if (!Files.exists(iviewFile)) {
                                return super.visitFile(iviewFile, attrs);
                            }
                            MCRTiledPictureProps imageProps = MCRTiledPictureProps.getInstanceFromFile(iviewFile);
                            int zoomLevel = (zoom == null || zoom > imageProps.getZoomlevel()) ? imageProps
                                .getZoomlevel() : zoom;
                            BufferedImage image = MCRIView2Tools.getZoomLevel(iviewFile, zoomLevel);
                            ZipArchiveEntry entry = new ZipArchiveEntry(file.getFileName() + ".jpg");
                            zipStream.putArchiveEntry(entry);
                            ImageIO.write(image, "jpg", zipStream);
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
                MCRSessionMgr.getCurrentSession();
                MCRTransactionManager.commitTransactions();
            }
        }
    }

}
