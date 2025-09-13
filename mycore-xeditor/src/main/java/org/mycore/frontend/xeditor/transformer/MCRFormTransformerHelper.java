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

import org.mycore.frontend.MCRFrontendUtil;

/**
 * Helps transforming xed:form elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFormTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_METHOD = "method";

    private static final String VALUE_POST = "post";
    private static final String VALUE_OUTPUT = "output";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("form");
    }

    @Override
    void handle(MCRTransformerHelperCall call) {
        call.registerDeclaredNamespaces();

        String method = call.getAttributeValueOrDefault(ATTR_METHOD, VALUE_POST);
        if (!Objects.equals(method, VALUE_OUTPUT)) {
            replaceXPaths(call);

            String actionURL = MCRFrontendUtil.getBaseURL() + "servlets/XEditor";
            call.getReturnElement().setAttribute(ATTR_ACTION, actionURL);
            call.getReturnElement().setAttribute(ATTR_METHOD, method);
        }
    }
}
