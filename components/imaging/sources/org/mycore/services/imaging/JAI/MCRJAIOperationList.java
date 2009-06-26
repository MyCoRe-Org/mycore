package org.mycore.services.imaging.JAI;

import java.util.LinkedList;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

public class MCRJAIOperationList implements MCRJAIImageOp {
    private LinkedList<MCRJAIImageOp> opList = new LinkedList<MCRJAIImageOp>();
    
    public void addOp(MCRJAIImageOp op){
        opList.add(op);
    }
    
    public RenderedOp executeOp(PlanarImage image) {
        for (MCRJAIImageOp op : opList) {
            image = op.executeOp(image);
        }
        
        return (RenderedOp) image;
    }

}
