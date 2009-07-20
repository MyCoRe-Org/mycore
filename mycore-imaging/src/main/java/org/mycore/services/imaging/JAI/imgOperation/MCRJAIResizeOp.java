package org.mycore.services.imaging.JAI.imgOperation;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


public class MCRJAIResizeOp extends MCRJAIScaleOp implements MCRJAIImageOp {
    int newWidth;
    int newHeight;
    
    public MCRJAIResizeOp(int newWidth, int newHeight) {
        super(1);
        
        this.newWidth = newWidth;
        this.newHeight = newHeight;
    }
    
    /* (non-Javadoc)
     * @see org.mycore.services.imaging.MCRJAIImageOp#executeOp(javax.media.jai.PlanarImage)
     */
    public RenderedOp executeOp(PlanarImage image){
        float xScale = (newWidth <= 0 )? 1.0F : (float) newWidth / (float) image.getWidth();
        float yScale = (newHeight <= 0)? 1.0F : (float) newHeight / (float) image.getHeight();
        scaleFactor = (yScale < xScale) ? yScale : xScale;
        
        return scale(image, scaleFactor);
    }
}
