package org.mycore.frontend.editor.validation;

import org.jdom.Element;

public class MCRExternalXMLValidator extends MCRExternalValidator {

    @Override
    @SuppressWarnings("unchecked")
    protected Class getArgumentType() {
        return Element.class;
    }
}
