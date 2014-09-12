package org.mycore.frontend.xeditor.tracker;

import org.jdom2.Content;
import org.jdom2.Element;

public class MCRSwapElements implements MCRChange {

    public static MCRChangeData swap(Element parent, Element a, Element b) {
        int posA = parent.indexOf(a);
        int posB = parent.indexOf(b);
        return swap(parent, posA, a, posB, b);
    }

    public static MCRChangeData swap(Element parent, int posA, int posB) {
        Content a = parent.getContent().get(posA);
        Content b = parent.getContent().get(posB);
        return swap(parent, posA, a, posB, b);
    }

    public static MCRChangeData swap(Element parent, int posA, Content a, int posB, Content b) {
        if (posA > posB)
            return swap(parent, posB, b, posA, a);

        b.detach(); // x a x x x  
        parent.addContent(posA, b); // x b a x x x 
        a.detach(); // x b x x x
        parent.addContent(posB, a); // x b x x a x

        return new MCRChangeData("swapped-elements", posA + " " + posB, posB, parent);
    }

    public void undo(MCRChangeData data) {
        int posA = Integer.parseInt(data.getText().split(" ")[0]);
        int posB = Integer.parseInt(data.getText().split(" ")[1]);
        swap(data.getContext(), posA, posB);
    }
}
