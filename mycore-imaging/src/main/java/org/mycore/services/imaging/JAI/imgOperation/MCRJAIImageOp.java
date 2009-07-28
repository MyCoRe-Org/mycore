package org.mycore.services.imaging.JAI.imgOperation;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

public interface MCRJAIImageOp {

    public RenderedOp executeOp(PlanarImage image);
    public int getOrder();

}