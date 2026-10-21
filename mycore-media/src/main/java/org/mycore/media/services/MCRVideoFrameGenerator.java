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

package org.mycore.media.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Extracts a full-resolution video frame without resizing it. */
public interface MCRVideoFrameGenerator {

    /**
     * Checks whether this generator can process the supplied video.
     *
     * @param video the source video
     * @return {@code true} if frame generation can be attempted
     */
    default boolean checkRequirements(Path video) {
        return true;
    }

    /**
     * Extracts a frame from a video, including videos on non-default file systems.
     *
     * @param video the source video
     * @return the decoded frame at its original resolution
     * @throws IOException if the video cannot be read or no frame can be extracted
     */
    BufferedImage generateFrame(Path video) throws IOException;
}
