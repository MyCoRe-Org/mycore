package org.mycore.iiif.presentation.model.additional;

import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;

public class MCRIIIFAnnotation extends MCRIIIFAnnotationBase {

    public static final String TYPE = "@oa:Annotation";

    private MCRIIIFResource resource;

    public MCRIIIFAnnotation(String id, MCRIIIFCanvas parent) {
        super(id, parent, TYPE);
    }

    public MCRIIIFResource getResource() {
        return resource;
    }

    public void setResource(MCRIIIFResource resource) {
        this.resource = resource;
    }
}
