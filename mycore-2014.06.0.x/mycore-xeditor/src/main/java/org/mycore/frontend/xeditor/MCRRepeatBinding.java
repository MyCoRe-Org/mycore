package org.mycore.frontend.xeditor;

import org.jaxen.JaxenException;
import org.jdom2.Content;
import org.jdom2.JDOMException;
import org.jdom2.Parent;

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
        Content a = (Content) (boundNodes.get(posA - 1));
        Content b = (Content) (boundNodes.get(posB - 1));
        Parent parent = a.getParent();
        posA = parent.indexOf(a);
        posB = parent.indexOf(b);
        String xPath = MCRXPathBuilder.buildXPath(parent);
        return xPath + "|" + posA + "|" + posB;
    }
}
