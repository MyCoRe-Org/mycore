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

/**
 * Helps transforming xed:bind elements.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRBindTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_XPATH = "xpath";
    private static final String ATTR_INITIALLY = "initially";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_SET = "set";
    private static final String ATTR_NAME = "name";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("bind");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        call.registerDeclaredNamespaces();
        handleBind(call);
        handleSetAttribute(call);
        handleDefaultAttribute(call);
    }

    private void handleBind(MCRTransformerHelperCall call) throws JaxenException {
        String xPath = call.getAttributeValue(ATTR_XPATH);
        String initialValue = call.getAttributeValueOrDefault(ATTR_INITIALLY, null);
        String name = call.getAttributeValueOrDefault(ATTR_NAME, null);

        if (getSession().getEditedXML() == null) {
            createEmptyDocumentFromXPath(xPath);
        }

        if (getCurrentBinding() == null) {
            setCurrentBinding(getSession().getRootBinding());
        }

        if (initialValue != null) {
            initialValue = replaceXPaths(initialValue);
        }

        setCurrentBinding(new MCRBinding(xPath, initialValue, name, getCurrentBinding()));
    }

    private void createEmptyDocumentFromXPath(String xPath) throws JaxenException {
        Element root = createRootElement(xPath);
        getSession().setEditedXML(new Document(root));
        getSession().setBreakpoint("Starting with empty XML document");
    }

    private Element createRootElement(String xPath) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        LocationPath lp = (LocationPath) (baseXPath.getRootExpr());
        NameStep nameStep = (NameStep) (lp.getSteps().getFirst());
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);
        return new Element(nameStep.getLocalName(), ns);
    }

    private void handleSetAttribute(MCRTransformerHelperCall call) {
        String valueToSet = call.getAttributeValueOrDefault(ATTR_SET, null);
        if (valueToSet != null) {
            getCurrentBinding().setValues(replaceXPaths(valueToSet));
        }
    }

    private void handleDefaultAttribute(MCRTransformerHelperCall call) {
        String defaultValueToSet = call.getAttributeValueOrDefault(ATTR_DEFAULT, null);
        if (defaultValueToSet != null) {
            defaultValueToSet = replaceXPaths(defaultValueToSet);
            getCurrentBinding().setDefault(defaultValueToSet);
            getSession().getSubmission().markDefaultValue(getCurrentBinding().getAbsoluteXPath(), defaultValueToSet);
        }
    }
}
