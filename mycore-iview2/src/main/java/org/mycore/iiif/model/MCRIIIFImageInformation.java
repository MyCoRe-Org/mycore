package org.mycore.iiif.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MCRIIIFImageInformation {


    public MCRIIIFImageInformation(String context, String id, String protocol, int width, int height) {
        this.context = context;
        this.id = id;
        this.protocol = protocol;
        this.width = width;
        this.height = height;
        this.tiles = new ArrayList<>();
        this.profile = new ArrayList<>();
    }

    /**
     * Required!
     * Describes the semantics of the terms used in the document.
     * Must be the URI: <a href="http://iiif.io/api/image/2/context.json">http://iiif.io/api/image/2/context.json</a> for version 2.0 of the IIIF Image API.
     */
    @SerializedName("@context")
    public String context;

    /**
     * Required!
     * Base URI of the image.
     */
    @SerializedName("@id")
    public String id;

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
}
