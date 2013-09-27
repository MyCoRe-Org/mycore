package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.tracker.MCRSwapElements;

public class MCRRepeatBinding extends MCRBinding {

    private int repeatPosition;

    private int maxRepeats;

    private String xPath;

    public MCRRepeatBinding(String xPath, MCRBinding parent, int minRepeats, int maxRepeats) throws JaxenException, JDOMException {
        this(xPath, parent);

        while (getBoundNodes().size() < minRepeats)
            cloneBoundElement(getBoundNodes().size() - 1);

        this.maxRepeats = maxRepeats < 1 ? Integer.MAX_VALUE : maxRepeats;
        this.maxRepeats = Math.max(this.maxRepeats, getBoundNodes().size());
    }

    public MCRRepeatBinding(String xPath, MCRBinding parent) throws JaxenException, JDOMException {
        super(xPath, parent);
        this.xPath = xPath;
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

    public void up(int pos) {
        // Swap positions in boundNodes
        Element a = (Element) (boundNodes.get(pos - 2));
        Element b = (Element) (boundNodes.remove(pos - 1));
        boundNodes.add(pos - 2, b);

        // Swap positions in parent element, assume same parent
        Element parent = a.getParentElement();
        int posA = parent.indexOf(a);
        int posB = parent.indexOf(b);
        track(MCRSwapElements.swap(parent, posA, posB));
    }

    public void down(int pos) {
        up(pos + 1);
    }

    public String getControlsParameter() throws UnsupportedEncodingException {
        return parent.getAbsoluteXPath() + "_" + MCREncoder.encode(xPath) + "_" + repeatPosition;
    }
}
