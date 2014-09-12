package org.mycore.frontend.xeditor;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.tracker.MCRSwapElements;

public class MCRRepeatBinding extends MCRBinding {

    private int repeatPosition;

    private int maxRepeats;

    public MCRRepeatBinding(String xPath, MCRBinding parent, int minRepeats, int maxRepeats) throws JaxenException, JDOMException {
        this(xPath, parent);

        while (getBoundNodes().size() < minRepeats)
            cloneBoundElement(getBoundNodes().size() - 1);

        this.maxRepeats = maxRepeats < 1 ? Integer.MAX_VALUE : maxRepeats;
        this.maxRepeats = Math.max(this.maxRepeats, getBoundNodes().size());
    }

    public MCRRepeatBinding(String xPath, MCRBinding parent) throws JaxenException, JDOMException {
        super(xPath, true, parent);
        this.maxRepeats = Integer.MAX_VALUE;
    }

    public int getRepeatPosition() {
        return repeatPosition;
    }

    public MCRBinding bindRepeatPosition() throws JDOMException, JaxenException {
        repeatPosition++;
        return new MCRBinding(repeatPosition, this);
    }

    public int getMaxRepeats() {
        return maxRepeats;
    }

    public String getSwapParameter(int posA, int posB) {
        Element a = (Element) (boundNodes.get(posA - 1));
        Element b = (Element) (boundNodes.get(posB - 1));
        
        String xPathP = MCRXPathBuilder.buildXPath( a.getParent() );
        String xPathA = MCRXPathBuilder.buildChildPath(a);
        String xPathB = MCRXPathBuilder.buildChildPath(b);
        
        return xPathP + "|" + xPathA + "|" + xPathB;
    }
    
    public static void swap(String swapParameter, MCRBinding context) throws JaxenException, JDOMException {
        String[] tokens = swapParameter.split("\\|");
        String xPathP = tokens[0];
        String xPathA = tokens[1];
        String xPathB = tokens[2];
        
        MCRBinding bindingP = new MCRBinding(xPathP, false, context);
        MCRBinding bindingA = new MCRBinding(xPathA, false, bindingP);
        MCRBinding bindingB = new MCRBinding(xPathB, false, bindingP);

        Element parent = (Element) (bindingP.getBoundNode());
        Element elementA = (Element)( bindingA.getBoundNode() );
        Element elementB = (Element)( bindingB.getBoundNode() );
        bindingP.track(MCRSwapElements.swap(parent, elementA, elementB));
        
        bindingA.detach();
        bindingB.detach();
        bindingP.detach();
    }
}
