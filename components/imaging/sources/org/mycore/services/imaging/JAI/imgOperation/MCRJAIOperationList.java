package org.mycore.services.imaging.JAI.imgOperation;

import java.util.LinkedList;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


public class MCRJAIOperationList implements MCRJAIImageOp {
    private LinkedList<MCRJAIImageOp> opList = new LinkedList<MCRJAIImageOp>();
    
    public void addOp(MCRJAIImageOp op){
        MCRJAIImageOp pop = opList.pollFirst();
        
        if(pop == null){
            opList.push(op);
            return;
        }
        
        if(pop.getOrder() < op.getOrder()){
            opList.push(op);
            opList.push(pop);
        } else{
            opList.push(pop);
            opList.push(op);
        }
    }
    
    public RenderedOp executeOp(PlanarImage image) {
        for (MCRJAIImageOp op : opList) {
            image = op.executeOp(image);
        }
        
        return (RenderedOp) image;
    }

    public int getOrder() {
        return 0;
    }

}
