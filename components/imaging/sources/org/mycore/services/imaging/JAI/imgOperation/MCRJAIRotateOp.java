package org.mycore.services.imaging.JAI.imgOperation;

import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


public class MCRJAIRotateOp implements MCRJAIImageOp {
    protected float rotAngle;
    
    public MCRJAIRotateOp(float rotAngle) {
        this.rotAngle = rotAngle;
    }
    
    protected RenderedOp rotate(PlanarImage image, float rotAngle) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add((float)image.getWidth()/2);
        pb.add((float)image.getHeight()/2);
        pb.add(rotAngle);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("rotate", pb);
    }
    
    public RenderedOp executeOp(PlanarImage image) {
        return rotate(image, rotAngle);
    }

    public int getOrder() {
        return 100;
    }

}
