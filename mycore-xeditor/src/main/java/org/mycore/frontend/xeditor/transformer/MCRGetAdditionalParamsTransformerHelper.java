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
import java.util.Collection;
import java.util.Map;

import org.jdom2.Element;
import org.mycore.frontend.xeditor.MCREditorSessionStore;
import org.mycore.frontend.xeditor.MCREditorSubmission;

public class MCRGetAdditionalParamsTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_STYLE = "style";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("getAdditionalParameters");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        Element div = new Element("div").setAttribute(ATTR_STYLE, "visibility:hidden");

        Map<String, String[]> parameters = state.editorSession.getRequestParameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if ((value != null) && !value.isEmpty()) {
                    div.addContent(buildAdditionalParameterElement(name, value));
                }
            }
        }

        String xPaths2CheckResubmission = state.editorSession.getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty()) {
            div.addContent(buildAdditionalParameterElement(MCREditorSubmission.PREFIX_CHECK_RESUBMISSION,
                xPaths2CheckResubmission));
        }

        Map<String, String> defaultValues = state.editorSession.getSubmission().getDefaultValues();
        for (String xPath : defaultValues.keySet()) {
            div.addContent(buildAdditionalParameterElement(MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath,
                defaultValues.get(xPath)));
        }

        state.editorSession.setBreakpoint("After transformation to HTML");
        div.addContent(buildAdditionalParameterElement(MCREditorSessionStore.XEDITOR_SESSION_PARAM,
            state.editorSession.getCombinedSessionStepID()));

        call.getReturnElement().addContent(div);
    }

    private Element buildAdditionalParameterElement(String name, String value) {
        Element input = new Element("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", name);
        input.setAttribute("value", value);
        return input;
    }
}
