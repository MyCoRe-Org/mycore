package org.mycore.iiif.presentation.model.additional;

import java.util.ArrayList;
import java.util.List;

import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;

public class MCRIIIFAnnotationList extends MCRIIIFAnnotationBase {

    public List<MCRIIIFAnnotation> resources = new ArrayList<>();

    public MCRIIIFAnnotationList(String id, MCRIIIFCanvas parent) {
        super(id, parent, "sc:AnnotationList");
    }
}
