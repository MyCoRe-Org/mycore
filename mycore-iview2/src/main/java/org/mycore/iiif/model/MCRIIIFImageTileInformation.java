package org.mycore.iiif.model;

import java.util.ArrayList;
import java.util.List;

public class MCRIIIFImageTileInformation {


    public final int width;
    public final int height;
    public List<Integer> scaleFactors = new ArrayList<>();
    public MCRIIIFImageTileInformation(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
