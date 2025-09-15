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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.jdom2.Element;

/**
 * Helps preparing xed:include elements by replacing parameters in the attributes.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRIncludeTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_URI = "uri";
    private static final String ATTR_REF = "ref";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("replaceRef", "include");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws TransformerException, IOException, ParserConfigurationException {
        if (call.getMethod().equals("include")) {
            handleInclude(call);
        } else {
            handleReplaceRef(call);
        }
    }

    private void handleInclude(MCRTransformerHelperCall call)
        throws TransformerFactoryConfigurationError, TransformerException {
        String uri = call.getAttributeValue(ATTR_URI);
        String ref = call.getAttributeValue(ATTR_REF);

        Element resolved;

        if (uri != null) {
            uri = replaceXPaths(uri);

            String sStatic = call.getAttributeValue("static");
            resolved = transformationState.getIncludeHandler().resolve(uri, sStatic);
        } else {
            ref = replaceXPaths(ref);
            resolved = transformationState.getIncludeHandler().resolve(ref);
        }

        if (resolved != null) {
            call.setReturnElement(resolved);
        }
    }

    private void handleReplaceRef(MCRTransformerHelperCall call) {
        String oldValue = call.getAttributeValue(ATTR_REF);
        if (oldValue != null) {
            String newValue = replaceXPaths(oldValue);
            call.getReturnElement().setAttribute(ATTR_REF, newValue);
        }
    }
}
