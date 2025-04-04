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

package org.mycore.iiif.image.model;

import java.util.ArrayList;
import java.util.List;

import org.mycore.iiif.model.MCRIIIFBase;

public class MCRIIIFImageInformation extends MCRIIIFBase {

    /**
     * Required!
     * Defines the protocol. Should be: <a href="http://iiif.io/api/image">http://iiif.io/api/image/2/context.json</a>
     */
    public String protocol;

    /**
     * Required!
     * width of image in pixels
     */
    public int width;

    /**
     * Required!
     * height of image in pixels
     */
    public int height;

    /**
     * Required for caching.
     */
    public transient long lastModified;

    /**
     * Required!
     * A array of profiles, first entry is always a <i>compliance level URI</i> like
     * <a href="http://iiif.io/api/image/2/level2.json">http://iiif.io/api/image/2/level2.json</a>.
     */
    public List<Object> profile;

    /**
     * Optional!
     */
    public List<MCRIIIFImageTileInformation> tiles;

    public MCRIIIFImageInformation(String context, String id, String protocol, int width, int height,
        long lastModified) {
        super(id, null, context);
        this.protocol = protocol;
        this.width = width;
        this.height = height;
        this.tiles = new ArrayList<>();
        this.profile = new ArrayList<>();
        this.lastModified = lastModified;
    }
}
