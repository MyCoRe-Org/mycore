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

import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.frontend.xeditor.MCRBinding;

public class MCRBindTransformerHelper extends MCRTransformerHelperBase {

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("bind");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        call.registerDeclaredNamespaces();

        String xPath = call.getAttributeValue("xpath");
        String initialValue = call.getAttributeValueOrDefault("initially", null);
        String name = call.getAttributeValueOrDefault("name", null);

        bind(xPath, initialValue, name);

        String setAttr = call.getAttributeValueOrDefault("set", null);
        if (setAttr != null) {
            setValues(setAttr);
        }

        String setDefault = call.getAttributeValueOrDefault("default", null);
        if (setDefault != null) {
            setDefault(setDefault);
        }
    }

    private void bind(String xPath, String initialValue, String name)
        throws JaxenException {
        if (state.editorSession.getEditedXML() == null) {
            createEmptyDocumentFromXPath(xPath);
        }

        if (state.currentBinding == null) {
            state.currentBinding = state.editorSession.getRootBinding();
        }

        if (initialValue != null) {
            initialValue = state.replaceXPaths(initialValue);
        }
        state.setCurrentBinding(new MCRBinding(xPath, initialValue, name, state.currentBinding));
    }

    private void createEmptyDocumentFromXPath(String xPath) throws JaxenException {
        Element root = createRootElement(xPath);
        state.editorSession.setEditedXML(new Document(root));
        state.editorSession.setBreakpoint("Starting with empty XML document");
    }

    private Element createRootElement(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().getFirst());
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);
        return new Element(nameStep.getLocalName(), ns);
    }

    private void setValues(String value) {
        state.currentBinding.setValues(state.replaceXPaths(value));
    }

    private void setDefault(String value) {
        value = state.replaceXPaths(value);
        state.currentBinding.setDefault(value);
        state.editorSession.getSubmission().markDefaultValue(state.currentBinding.getAbsoluteXPath(), value);
    }
}
