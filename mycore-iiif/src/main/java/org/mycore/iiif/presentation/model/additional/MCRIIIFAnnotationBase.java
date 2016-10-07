package org.mycore.iiif.presentation.model.additional;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;

public class MCRIIIFAnnotationBase extends MCRIIIFPresentationBase {

    // custom serializer needed
    protected String on;

    private String label;

    private String description;

    private String motivation = "sc:painting";

    private transient MCRIIIFCanvas parent;

    public MCRIIIFAnnotationBase(String id, MCRIIIFCanvas parent, String type) {
        super(id, type, API_PRESENTATION_2);
        this.parent = parent;
        refresh();
    }

    public void refresh() {
        this.on = this.parent.getId();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

}
