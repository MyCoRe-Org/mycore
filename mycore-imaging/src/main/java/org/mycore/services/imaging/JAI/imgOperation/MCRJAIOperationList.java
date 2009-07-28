package org.mycore.services.imaging.JAI.imgOperation;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


public class MCRJAIOperationList implements MCRJAIImageOp {
    private List<MCRJAIImageOp> opList = new LinkedList<MCRJAIImageOp>();
    
    public void addOp(MCRJAIImageOp op){
        opList.add(op);
    }
    
    public RenderedOp executeOp(PlanarImage image) {
        Comparator<? super MCRJAIImageOp> compare = new Comparator<MCRJAIImageOp>(){

            public int compare(MCRJAIImageOp o1, MCRJAIImageOp o2) {
                return o1.getOrder() - o2.getOrder();
            }
        };
        
        Collections.sort(opList, compare);
        for (MCRJAIImageOp op : opList) {
            image = op.executeOp(image);
        }
        
        return (RenderedOp) image;
    }

    public int getOrder() {
        return 0;
    }

}
