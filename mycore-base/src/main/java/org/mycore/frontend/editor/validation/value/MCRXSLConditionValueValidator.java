package org.mycore.frontend.editor.validation.value;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.frontend.editor.validation.MCRXSLConditionTester;

public class MCRXSLConditionValueValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("xsl");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        String condition = getProperty("xsl");
        Document xml = buildInputDocument(input);
        return new MCRXSLConditionTester(condition).testCondition(xml);
    }

    private Document buildInputDocument(String input) {
        return new Document(new Element("input").addContent(input));
    }
}
