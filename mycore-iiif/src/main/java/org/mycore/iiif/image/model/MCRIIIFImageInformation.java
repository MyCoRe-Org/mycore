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
     * Required!
     * A array of profiles, first entry is always a <i>compliance level URI</i> like http://iiif.io/api/image/2/level2.json.
     */
    public List<Object> profile;

    /**
     * Optional!
     */
    public List<MCRIIIFImageTileInformation> tiles;

    public MCRIIIFImageInformation(String context, String id, String protocol, int width, int height) {
        super(id, null, context);
        this.protocol = protocol;
        this.width = width;
        this.height = height;
        this.tiles = new ArrayList<>();
        this.profile = new ArrayList<>();
    }
}
