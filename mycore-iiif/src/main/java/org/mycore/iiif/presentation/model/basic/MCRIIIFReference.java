package org.mycore.iiif.presentation.model.basic;

import org.mycore.iiif.model.MCRIIIFBase;

public class MCRIIIFReference extends MCRIIIFBase {

    public MCRIIIFReference(String id, String type) {
        super(id, type, API_PRESENTATION_2);
    }

    public MCRIIIFReference(MCRIIIFCanvas canvas) {
        super(canvas.getId(), canvas.getType(), canvas.getContext());
    }

    public MCRIIIFReference(MCRIIIFRange range) {
        super(range.getId(), range.getType(), range.getContext());
    }
}
