/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.xeditor;

import java.util.List;
import java.util.Objects;

import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.Step;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.frontend.xeditor.tracker.MCRAddedElement;
import org.mycore.frontend.xeditor.tracker.MCRSwapElements;

public class MCRRepeatBinding extends MCRBinding {

    private static final String METHOD_BUILD = "build";

    private static final String METHOD_CLONE = "clone";

    private int repeatPosition;

    private int maxRepeats;

    private static final String DEFAULT_METHOD = MCRConfiguration2
        .getString("MCR.XEditor.InsertTarget.DefaultMethod")
        .orElse(METHOD_BUILD);

    private final String method; // build|clone

    public MCRRepeatBinding(String xPath, MCRBinding parent, int minRepeats, int maxRepeats, String method)
        throws JaxenException {
        this(xPath, parent, method);

        while (getBoundNodes().size() < minRepeats) {
            insert(getBoundNodes().size());
        }

        this.maxRepeats = maxRepeats < 1 ? Integer.MAX_VALUE : maxRepeats;
        this.maxRepeats = Math.max(this.maxRepeats, getBoundNodes().size());
    }

    public MCRRepeatBinding(String xPath, MCRBinding parent, String method) throws JaxenException {
        super(xPath, true, parent);
        this.method = Objects.equals(method, METHOD_CLONE) ? METHOD_CLONE
            : Objects.equals(method, METHOD_BUILD) ? METHOD_BUILD : DEFAULT_METHOD;
        this.maxRepeats = Integer.MAX_VALUE;
    }

    public int getRepeatPosition() {
        return repeatPosition;
    }

    public MCRBinding bindRepeatPosition() {
        repeatPosition++;
        return new MCRBinding(repeatPosition, this);
    }

    public int getMaxRepeats() {
        return maxRepeats;
    }

    public String getMethod() {
        return method;
    }

    public Element getParentElement() {
        return ((Element) getBoundNode()).getParentElement();
    }

    @SuppressWarnings("unchecked")
    public String getElementNameWithPredicates() throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        Expr rootExpr = baseXPath.getRootExpr();
        LocationPath locationPath = (LocationPath) rootExpr;
        List<Step> steps = locationPath.getSteps();
        Step lastStep = steps.getLast();
        return MCRNodeBuilder.simplify(lastStep.getText());
    }

    public void swap(int pos) {
        Element parent = getParentElement();
        Element elementA = (Element) (getBoundNodes().get(pos - 1));
        Element elementB = (Element) (getBoundNodes().get(pos));
        track(MCRSwapElements.swap(parent, elementA, elementB));
    }

    public void insert(int pos) throws JaxenException {
        if (Objects.equals(method, METHOD_BUILD)) {
            Element parentElement = getParentElement();
            Element precedingElement = (Element) (getBoundNodes().get(pos - 1));
            int posOfPrecedingInParent = parentElement.indexOf(precedingElement);
            int targetPos = posOfPrecedingInParent + 1;
            String pathToBuild = getElementNameWithPredicates();
            Element newElement = (Element) (new MCRNodeBuilder().buildNode(pathToBuild, null, null));
            parentElement.addContent(targetPos, newElement);
            boundNodes.add(pos, newElement);
            track(MCRAddedElement.added(newElement));
        } else {
            cloneBoundElement(pos - 1);
        }
    }

}
