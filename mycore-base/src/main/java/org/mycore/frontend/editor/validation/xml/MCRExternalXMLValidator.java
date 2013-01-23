package org.mycore.frontend.editor.validation.xml;

import org.jdom2.Element;
import org.mycore.frontend.editor.validation.MCRExternalValidator;

public class MCRExternalXMLValidator extends MCRExternalValidator {

    @Override
    @SuppressWarnings("unchecked")
    protected Class getArgumentType() {
        return Element.class;
    }
}
