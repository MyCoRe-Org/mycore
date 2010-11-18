package org.mycore.frontend.editor.validation.xml;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.frontend.editor.validation.MCRValidatorBase;
import org.mycore.frontend.editor.validation.MCRXSLConditionTester;

public class MCRXSLConditionElementValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("xsl");
    }

    @Override
    protected boolean isValidOrDie(Object... input) throws Exception {
        if ((input == null) || (input[0] == null))
            return true;

        String condition = getProperty("xsl");
        Document xml = buildInputDocument(input);
        return new MCRXSLConditionTester(condition).testCondition(xml);
    }

    private Document buildInputDocument(Object... input) {
        Element element = (Element) (input[0]);
        element = (Element) (element.clone());
        return new Document(element);
    }
}
