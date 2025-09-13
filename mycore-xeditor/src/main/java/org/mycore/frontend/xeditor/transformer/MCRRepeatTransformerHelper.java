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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRRepeatBinding;
import org.mycore.frontend.xeditor.target.MCRInsertTarget;
import org.mycore.frontend.xeditor.target.MCRSwapTarget;

/**
 * Helps transforming xed:repeat and xed:controls elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRRepeatTransformerHelper extends MCRTransformerHelperBase {

    private static final String METHOD_CONTROLS = "controls";

    private static final String METHOD_REPEATED = "repeated";

    private static final String METHOD_REPEAT = "repeat";

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
    private static final String ATTR_TEXT = "xed:text";
    private static final String ATTR_NAME = "name";

    private int anchorID;

    @Override
    public List<String> getSupportedMethods() {
        return Arrays.asList(METHOD_REPEAT, METHOD_REPEATED, METHOD_CONTROLS);
    }

    @Override
    public void handle(MCRTransformerHelperCall call) throws JaxenException {
        switch (call.getMethod()) {
            case METHOD_REPEAT:
                handleRepeat(call);
                break;
            case METHOD_REPEATED:
                handleRepeated(call);
                break;
            case METHOD_CONTROLS:
                handleControls(call);
                break;
            default:
                ;
        }
    }

    private void handleRepeat(MCRTransformerHelperCall call) throws JaxenException {
        call.registerDeclaredNamespaces();

        String xPath = call.getAttributeValue(ATTR_XPATH);
        int minRepeats = Integer.parseInt(call.getAttributeValueOrDefault(ATTR_MIN, "0"));
        int maxRepeats = Integer.parseInt(call.getAttributeValueOrDefault(ATTR_MAX, "0"));
        String method = call.getAttributeValue(ATTR_METHOD);

        MCRRepeatBinding repeat = new MCRRepeatBinding(xPath, getCurrentBinding(), minRepeats, maxRepeats, method);
        setCurrentBinding(repeat);

        Element repeated = new Element(METHOD_REPEATED, MCRConstants.getStandardNamespace("xed"));
        repeat.getBoundNodes().forEach(node -> call.getReturnElement().addContent(repeated.clone()));
    }

    private void handleRepeated(MCRTransformerHelperCall call) {
        transformationState.setCurrentBinding(findCurrentRepeatBinding().bindRepeatPosition());
        getSession().getValidator().setValidationMarker(transformationState.getCurrentBinding());

        Element anchor = new Element("a").setAttribute("id", "rep-" + ++anchorID);
        call.getReturnElement().addContent(anchor);
    }

    private void handleControls(MCRTransformerHelperCall call) throws JaxenException {
        MCRRepeatBinding currentRepeat = findCurrentRepeatBinding();
        int pos = currentRepeat.getRepeatPosition();
        int num = currentRepeat.getBoundNodes().size();
        int max = currentRepeat.getMaxRepeats();

        String controlTokens = call.getAttributeValueOrDefault(ATTR_TEXT, DEFAULT_CONTROLS);
        for (String token : controlTokens.split("\\s+")) {
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
                name.append(MCRInsertTarget.getInsertParameter(findCurrentRepeatBinding()));
            } else if (CONTROL_REMOVE.equals(token)) {
                name.append(getCurrentBinding().getAbsoluteXPath());
            } else if (CONTROL_UP.equals(token) || CONTROL_DOWN.equals(token)) {
                name.append(getSwapParameter(token));
            }

            name.append("|rep-");
            name.append(CONTROL_REMOVE.equals(token) && (pos > 1) ? anchorID - 1 : anchorID);

            control.setAttribute(ATTR_NAME, name.toString());
            call.getReturnElement().addContent(control);
        }
    }

    private MCRRepeatBinding findCurrentRepeatBinding() {
        MCRBinding binding = transformationState.getCurrentBinding();
        while (!(binding instanceof MCRRepeatBinding)) {
            binding = binding.getParent();
        }
        return (MCRRepeatBinding) binding;
    }

    private String getSwapParameter(String action) throws JaxenException {
        boolean direction = Objects.equals(action, CONTROL_DOWN) ? MCRSwapTarget.MOVE_DOWN : MCRSwapTarget.MOVE_UP;
        return MCRSwapTarget.getSwapParameter(findCurrentRepeatBinding(), direction);
    }
}
