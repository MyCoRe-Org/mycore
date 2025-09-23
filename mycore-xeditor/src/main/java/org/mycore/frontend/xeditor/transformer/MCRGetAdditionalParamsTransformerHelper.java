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

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.mycore.frontend.xeditor.MCREditorSessionStore;
import org.mycore.frontend.xeditor.MCREditorSubmission;

/**
 * Helps outputting additional hidden fields for form submission. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRGetAdditionalParamsTransformerHelper extends MCRTransformerHelperBase {

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("getAdditionalParameters");
    }

    @Override
    void handle(MCRTransformerHelperCall call) {
        Element div = new Element("div").setAttribute("style", "visibility:hidden");
        call.getReturnElement().addContent(div);

        getSession().getRequestParameters().forEach((name, values) -> {
            Arrays.stream(values).filter(StringUtils::isNotEmpty).forEach(value -> addField(div, name, value));
        });

        String xPaths2CheckResubmission = getSession().getSubmission().getXPaths2CheckResubmission();
        if (!xPaths2CheckResubmission.isEmpty()) {
            addField(div, MCREditorSubmission.PREFIX_CHECK_RESUBMISSION, xPaths2CheckResubmission);
        }

        getSession().getSubmission().getDefaultValues().forEach(
            (xPath, defaultValue) -> addField(div, MCREditorSubmission.PREFIX_DEFAULT_VALUE + xPath, defaultValue));

        getSession().setBreakpoint("After transformation to HTML");
        addField(div, MCREditorSessionStore.XEDITOR_SESSION_PARAM, getSession().getCombinedSessionStepID());
    }

    private void addField(Element parent, String name, String value) {
        Element input = new Element("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", name);
        input.setAttribute("value", value);
        parent.addContent(input);
    }
}
