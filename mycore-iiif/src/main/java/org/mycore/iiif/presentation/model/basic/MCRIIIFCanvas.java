package org.mycore.iiif.presentation.model.basic;

import java.util.ArrayList;
import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;

public class MCRIIIFCanvas extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Canvas";

    public List<MCRIIIFAnnotationBase> images = new ArrayList<>();

    public List<MCRIIIFMetadata> metadata = new ArrayList<>();

    private String label;

    private String description = null;

    private MCRIIIFResource thumbnail = null;

    private int height;

    private int width;

    public MCRIIIFCanvas(String id, String label, int width, int height) {
        super(id, TYPE, API_PRESENTATION_2);
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public MCRIIIFResource getThumbnail() {
        return thumbnail;
    }
}
