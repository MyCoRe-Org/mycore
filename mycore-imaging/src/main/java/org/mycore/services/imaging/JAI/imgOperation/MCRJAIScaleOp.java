package org.mycore.services.imaging.JAI.imgOperation;

import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


public class MCRJAIScaleOp implements MCRJAIImageOp{
    protected float scaleFactor;
    
    public MCRJAIScaleOp(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
    
    protected RenderedOp scale(PlanarImage image, float scaleFactor) {
        if (scaleFactor <= 0.0001)
            return (RenderedOp) image;
        
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(scaleFactor);
        pb.add(scaleFactor);
        pb.add(0f);
        pb.add(0f);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("scale", pb);
    }
    
    public RenderedOp executeOp(PlanarImage image){
        return scale(image, scaleFactor);
    }

    public int getOrder() {
        return 0;
    }
}
