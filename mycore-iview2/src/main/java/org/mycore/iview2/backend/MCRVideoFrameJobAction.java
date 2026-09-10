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

package org.mycore.iview2.backend;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.media.services.MCRVideoFrameGenerator;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

/** Stores a full-resolution video frame as an iView image under the video's path. */
public class MCRVideoFrameJobAction extends MCRJobAction {

    public static final String DERIVATE_PARAMETER = "derivate";

    public static final String PATH_PARAMETER = "path";

    public MCRVideoFrameJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return getClass().getName();
    }

    @Override
    public void execute() {
        MCRPath video = MCRPath.getPath(job.getParameter(DERIVATE_PARAMETER), job.getParameter(PATH_PARAMETER));
        try {
            if (!Files.isRegularFile(video)) {
                return;
            }
            String contentType = MCRContentTypes.probeContentType(video);
            if (contentType == null || !contentType.startsWith("video/")) {
                return;
            }
            MCRVideoFrameGenerator generator = MCRConfiguration2.getInstanceOfOrThrow(
                MCRVideoFrameGenerator.class, "MCR.Media.VideoFrameGenerator");
            if (!generator.checkRequirements(video)) {
                return;
            }
            BufferedImage frame = generator.generateFrame(video);
            Path image = Files.createTempFile("MyCoRe-Video-Frame-", ".png");
            try {
                if (!ImageIO.write(frame, "png", image.toFile())) {
                    throw new IOException("No PNG writer available");
                }
                // A queued video may have been deleted while FFmpeg was running.
                if (!Files.isRegularFile(video)) {
                    return;
                }
                MCRImage tiledImage = MCRImage.getInstance(image, video.getOwner(), video.getOwnerRelativePath());
                tiledImage.setTileDir(MCRIView2Tools.getTileDir());
                tiledImage.tile();
                if (!Files.isRegularFile(video)) {
                    MCRIView2Commands.deleteImageTiles(video.getOwner(), video.getOwnerRelativePath());
                }
            } finally {
                Files.deleteIfExists(image);
            }
        } catch (IOException e) {
            throw new MCRException("Could not generate iView image for video " + video, e);
        }
    }

    @Override
    public void rollback() {
        // Files are cleaned up in execute(); a retry regenerates the frame.
    }
}
