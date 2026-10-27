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

package org.mycore.iview2.events;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iview2.backend.MCRVideoFrameJobAction;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.media.services.MCRVideoFrameGenerator;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueueManager;
import org.mycore.services.queuedjob.MCRJobStatus;

/** Creates iView images for every video file, independent of the derivate's main document. */
public class MCRVideoFrameEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String VIDEO_FRAME_GENERATOR_PROPERTY = "MCR.Media.VideoFrameGenerator";

    @Override
    public void handlePathCreated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath path) || !attrs.isRegularFile()) {
            return;
        }
        try {
            String contentType = MCRContentTypes.probeContentType(path);
            if (contentType != null && contentType.startsWith("video/") && checkRequirements(path)) {
                MCRJob job = new MCRJob(MCRVideoFrameJobAction.class);
                job.setParameter(MCRVideoFrameJobAction.DERIVATE_PARAMETER, path.getOwner());
                job.setParameter(MCRVideoFrameJobAction.PATH_PARAMETER, path.getOwnerRelativePath());
                MCRJobQueueManager manager = MCRJobQueueManager.getInstance();
                if (manager.getJobDAO().getJobCount(MCRVideoFrameJobAction.class, job.getParameters(),
                    List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR)) > 0) {
                    LOGGER.info("There is already a video frame job for {}. Skipping.", file);
                } else if (!manager.getJobQueue(MCRVideoFrameJobAction.class).offer(job)) {
                    LOGGER.warn("Video frame job queue did not accept job for {}", file);
                }
            }
        } catch (IOException | RuntimeException e) {
            // The poster is optional and must not abort the file event.
            LOGGER.warn("Could not enqueue video frame generation for {}", file, e);
        }
    }

    private boolean checkRequirements(MCRPath path) {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRVideoFrameGenerator.class,
            VIDEO_FRAME_GENERATOR_PROPERTY).checkRequirements(path);
    }

    @Override
    public void handlePathUpdated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathCreated(evt, file, attrs);
    }

    @Override
    public void handlePathRepaired(MCREvent evt, Path file, BasicFileAttributes attrs) {
        // A repair only generates missing frames. Delete the tiles to force a new frame.
        if (file instanceof MCRPath path && hasTiles(path)) {
            return;
        }
        handlePathCreated(evt, file, attrs);
    }

    boolean hasTiles(MCRPath path) {
        return MCRIView2Tools.hasTiles(path.getOwner(), path.getOwnerRelativePath());
    }

    @Override
    public void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (file instanceof MCRPath path && attrs.isRegularFile()) {
            // MIME detection need not work after deletion. The tiling handler also uses this cleanup.
            try {
                MCRIView2Commands.deleteImageTiles(path.getOwner(), path.getOwnerRelativePath());
            } catch (IOException e) {
                throw new MCRException("Could not delete video frame tiles for " + file, e);
            }
        }
    }
}
