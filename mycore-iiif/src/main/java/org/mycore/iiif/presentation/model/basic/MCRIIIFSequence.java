package org.mycore.iiif.presentation.model.basic;

import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFViewingDirection;

public class MCRIIIFSequence extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Sequence";

    public List<MCRIIIFCanvas> canvases;

    public List<MCRIIIFMetadata> metadata;

    protected MCRIIIFReference startCanvas;

    private transient MCRIIIFCanvas origStartCanvas;

    private String description;

    private String label;

    private MCRIIIFAnnotationBase thumbnail = null;

    private MCRIIIFViewingDirection viewingDirection = null;

    public MCRIIIFSequence(String id) {
        super(id, TYPE, API_PRESENTATION_2);
    }

    public MCRIIIFCanvas getStartCanvas() {
        return origStartCanvas;
    }

    public void setStartCanvas(MCRIIIFCanvas startCanvas) {
        this.startCanvas = new MCRIIIFReference(this.origStartCanvas = startCanvas);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public MCRIIIFAnnotationBase getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(MCRIIIFAnnotationBase thumbnail) {
        this.thumbnail = thumbnail;
    }

    public MCRIIIFViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(MCRIIIFViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

}
