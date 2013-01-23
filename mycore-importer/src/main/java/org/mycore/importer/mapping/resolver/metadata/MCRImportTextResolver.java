package org.mycore.importer.mapping.resolver.metadata;

import org.jdom2.Namespace;


public class MCRImportTextResolver extends MCRImportAbstractMetadataResolver {

    @Override
    protected void resolveAdditional() {
        setDefaultAttributes();
    }

    protected void setDefaultAttributes() {
        // default lang is de
        String lang = saveToElement.getAttributeValue("lang", Namespace.XML_NAMESPACE); 
        if(lang == null || lang.equals(""))
            saveToElement.setAttribute("lang", "de", Namespace.XML_NAMESPACE);

        // default form is "plain"
        String form = saveToElement.getAttributeValue("form"); 
        if(form == null || form.equals(""))
            saveToElement.setAttribute("form", "plain");       
    }

    protected boolean isValid() {
        return !(saveToElement.getText() == null || saveToElement.getText().equals(""));
    }
}