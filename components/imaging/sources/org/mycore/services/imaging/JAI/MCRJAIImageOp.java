package org.mycore.services.imaging.JAI;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

public interface MCRJAIImageOp {

    public RenderedOp executeOp(PlanarImage image);

}