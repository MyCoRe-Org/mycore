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

package org.mycore.frontend.xeditor.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRRepeatBinding;
import org.mycore.frontend.xeditor.target.MCRInsertTarget;
import org.mycore.frontend.xeditor.target.MCRSwapTarget;

public class MCRRepeatTransformerHelper {

    private static final char COLON = ':';

    private static final String CONTROL_INSERT = "insert";
    private static final String CONTROL_APPEND = "append";
    private static final String CONTROL_REMOVE = "remove";
    private static final String CONTROL_UP = "up";
    private static final String CONTROL_DOWN = "down";

    private static final String DEFAULT_CONTROLS =
        String.join(" ", CONTROL_INSERT, CONTROL_REMOVE, CONTROL_UP, CONTROL_DOWN);

    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_MAX = "max";
    private static final String ATTR_MIN = "min";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_NAME = "name";

    private MCRTransformerHelper state;

    private int anchorID = 0;

    MCRRepeatTransformerHelper(MCRTransformerHelper state) {
        this.state = state;
    }

    void handleRepeat(MCRTransformerHelperCall call)
        throws JaxenException {
        call.registerDeclaredNamespaces();

        String xPath = call.getAttributeValue(ATTR_XPATH);
        int minRepeats = Integer.parseInt(call.getAttributeValueOrDefault(ATTR_MIN, "0"));
        int maxRepeats = Integer.parseInt(call.getAttributeValueOrDefault(ATTR_MAX, "0"));
        String method = call.getAttributeValue(ATTR_METHOD);
        List<Element> repeats = repeat(xPath, minRepeats, maxRepeats, method);
        call.getReturnElement().addContent(repeats);
    }

    private List<Element> repeat(String xPath, int minRepeats, int maxRepeats, String method)
        throws JaxenException {
        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, state.currentBinding, minRepeats, maxRepeats, method);
        state.setCurrentBinding(repeat);

        List<Element> repeats = new ArrayList<>();
        repeat.getBoundNodes().forEach(node -> repeats.add(new Element("repeat")));
        return repeats;
    }

    private MCRRepeatBinding getCurrentRepeat() {
        MCRBinding binding = state.currentBinding;
        while (!(binding instanceof MCRRepeatBinding)) {
            binding = binding.getParent();
        }
        return (MCRRepeatBinding) binding;
    }

    void handleControls(MCRTransformerHelperCall call)
        throws JaxenException {
        int pos = getCurrentRepeat().getRepeatPosition();
        int num = getCurrentRepeat().getBoundNodes().size();
        int max = getCurrentRepeat().getMaxRepeats();

        String text = call.getAttributeValueOrDefault(ATTR_TEXT, DEFAULT_CONTROLS);
        for (String token : text.split("\\s+")) {
            if ((CONTROL_APPEND.equals(token) && (pos < num)) ||
                (CONTROL_UP.equals(token) && (pos == 1)) ||
                (CONTROL_DOWN.equals(token) && (pos == num)) ||
                ((CONTROL_INSERT.equals(token) || CONTROL_APPEND.equals(token)) && (num == max))) {
                continue;
            }

            Element control = new Element("control").setText(token);

            StringBuilder name = new StringBuilder();
            name.append("_xed_submit_").append(token).append(COLON);

            if (CONTROL_APPEND.equals(token) || CONTROL_INSERT.equals(token)) {
                name.append(MCRInsertTarget.getInsertParameter(getCurrentRepeat()));
            } else if (CONTROL_REMOVE.equals(token)) {
                name.append(state.currentBinding.getAbsoluteXPath());
            } else if (CONTROL_UP.equals(token) || CONTROL_DOWN.equals(token)) {
                name.append(getSwapParameter(token));
            }

            name.append("|rep-");

            if (CONTROL_REMOVE.equals(token) && (pos > 1)) {
                name.append(previousAnchorID());
            } else {
                name.append(anchorID);
            }

            control.setAttribute(ATTR_NAME, name.toString());
            call.getReturnElement().addContent(control);
        }
    }

    private String getSwapParameter(String action) throws JaxenException {
        boolean direction = Objects.equals(action, CONTROL_DOWN) ? MCRSwapTarget.MOVE_DOWN : MCRSwapTarget.MOVE_UP;
        return MCRSwapTarget.getSwapParameter(getCurrentRepeat(), direction);
    }

    void handleBindRepeatPosition(MCRTransformerHelperCall call) {
        state.setCurrentBinding(getCurrentRepeat().bindRepeatPosition());
        state.editorSession.getValidator().setValidationMarker(state.currentBinding);

        Element anchor = new Element("a");
        String id = "rep-" + ++anchorID;
        anchor.setAttribute("id", id);
        call.getReturnElement().addContent(anchor);
    }

    private int previousAnchorID() {
        return (anchorID == 0 ? 1 : anchorID - 1);
    }
}
